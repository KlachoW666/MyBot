/**
 * Адаптер БД: PostgreSQL (postgres://...) или SQLite (sqlite:путь, дефолт).
 * Наружу — единый интерфейс: pool.query($1-плейсхолдеры) → { rows, rowCount },
 * withTransaction(fn), applySchema(), pool.end().
 */
import { readFile } from 'node:fs/promises';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { config } from '../config.js';

const isSqlite = !config.databaseUrl.startsWith('postgres');
export const dialect = isSqlite ? 'sqlite' : 'pg';

let pool;

if (!isSqlite) {
  const pg = (await import('pg')).default;
  pool = new pg.Pool({ connectionString: config.databaseUrl, max: 10 });
} else {
  const { default: Database } = await import('better-sqlite3');
  const file = config.databaseUrl.replace(/^sqlite:/, '') || 'data/app.db';
  const path = fileURLToPath(new URL(`../../${file}`, import.meta.url));
  mkdirSync(dirname(path), { recursive: true });
  const db = new Database(path);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');

  // $1,$2 → @p1,@p2; PG-специфику вырезаем.
  const prepSql = (text) => text
    .replace(/\bFOR UPDATE\b/gi, '')
    .replace(/\bnow\(\)/gi, 'CURRENT_TIMESTAMP')
    .replace(/\$(\d+)/g, '@p$1');
  const prepParams = (params = []) => Object.fromEntries(params.map((value, i) => [
    `p${i + 1}`,
    typeof value === 'boolean' ? (value ? 1 : 0) : value === undefined ? null : value,
  ]));

  const runQuery = (text, params) => {
    const sql = prepSql(text);
    const stmt = db.prepare(sql);
    if (stmt.reader || /\bRETURNING\b/i.test(sql)) {
      const rows = stmt.all(prepParams(params));
      return { rows, rowCount: rows.length };
    }
    const info = stmt.run(prepParams(params));
    return { rows: [], rowCount: info.changes };
  };

  // Одно соединение → сериализуем всё простым асинхронным мьютексом,
  // чтобы параллельные запросы не влезали в чужую транзакцию.
  let chain = Promise.resolve();
  const withLock = (fn) => {
    const next = chain.then(fn, fn);
    chain = next.catch(() => {});
    return next;
  };

  pool = {
    query: (text, params) => withLock(async () => runQuery(text, params)),
    end: async () => db.close(),
    _tx: (fn) => withLock(async () => {
      db.exec('BEGIN IMMEDIATE');
      try {
        const client = { query: async (text, params) => runQuery(text, params) };
        const result = await fn(client);
        db.exec('COMMIT');
        return result;
      } catch (error) {
        db.exec('ROLLBACK');
        throw error;
      }
    }),
    _exec: (sql) => db.exec(sql),
  };
}

export { pool };

/** Выполнить fn внутри транзакции (обе СУБД). */
export async function withTransaction(fn) {
  if (isSqlite) return pool._tx(fn);
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
}

/** Применить схему нужного диалекта (миграция/первый запуск). */
export async function applySchema() {
  const file = isSqlite ? 'schema.sqlite.sql' : 'schema.sql';
  const sql = await readFile(new URL(`../../db/${file}`, import.meta.url), 'utf8');
  if (isSqlite) pool._exec(sql);
  else await pool.query(sql);
}
