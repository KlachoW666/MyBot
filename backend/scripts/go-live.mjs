/**
 * Запуск для живого теста в Telegram ОДНОЙ командой:  npm run go
 *
 * Делает всё сам:
 *   1. проверяет .env (нужен только BOT_TOKEN);
 *   2. поднимает postgres + redis через docker compose (если есть docker);
 *   3. применяет схему, сидит 5 кейсов из живого каталога (если кейсов нет);
 *   4. собирает фронт (если dist отсутствует) — backend раздаёт его сам;
 *   5. запускает backend на :8080;
 *   6. открывает публичный HTTPS-туннель (cloudflared, без аккаунта);
 *   7. ставит вебхук бота (secret_token) и кнопку меню «🎁 Кейсы» на URL туннеля.
 *
 * После этого: открой бота в Telegram → кнопка меню → мини-апп работает.
 * Если PUBLIC_URL уже задан в .env (свой домен), шаг с туннелем пропускается.
 */
import { spawn, execSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import process from 'node:process';

const backendDir = fileURLToPath(new URL('..', import.meta.url));
const rootDir = fileURLToPath(new URL('../..', import.meta.url));
const frontendDir = `${rootDir}/frontend`;

// --- .env ---
const envPath = `${backendDir}/.env`;
if (!existsSync(envPath)) {
  console.error(`✗ Нет ${envPath}\n  Сделай: cp .env.example backend/.env и вставь BOT_TOKEN`);
  process.exit(1);
}
const { readFileSync } = await import('node:fs');
// Блокнот/PowerShell пишут UTF-16 или UTF-8 c BOM — учитываем всё.
const rawEnv = readFileSync(envPath);
let envText = rawEnv[0] === 0xff && rawEnv[1] === 0xfe
  ? rawEnv.toString('utf16le')
  : rawEnv.toString('utf8');
envText = envText.replace(/^﻿/, '');
for (const line of envText.split('\n')) {
  const match = line.match(/^\s*([A-Z_]+)\s*=\s*(.*)$/);
  if (!match) continue;
  // Инлайн-комментарии и пробелы не часть значения.
  const value = match[2].replace(/\s+#.*$/, '').trim();
  if (value && !(match[1] in process.env)) process.env[match[1]] = value;
}
if (!process.env.BOT_TOKEN) {
  console.error('✗ BOT_TOKEN пуст в backend/.env — возьми у @BotFather (/mybots → API Token)');
  process.exit(1);
}
process.env.PORT ??= '8080';

const sh = (cmd, opts = {}) =>
  execSync(cmd, { stdio: 'inherit', cwd: backendDir, ...opts });

// --- база (по умолчанию SQLite-файл — Docker не нужен) ---
const { pool, applySchema, dialect } = await import('../src/db/pool.js');
if (dialect === 'pg') {
  try {
    sh('docker compose up -d postgres redis --wait', { cwd: rootDir, stdio: 'pipe' });
    console.log('✓ postgres + redis (docker)');
  } catch {
    console.log('… docker недоступен — считаю, что Postgres/Redis уже запущены локально');
  }
  for (let i = 0; ; i++) {
    try { await pool.query('SELECT 1'); break; }
    catch (err) {
      if (i >= 30) { console.error('✗ Postgres не отвечает:', err.message); process.exit(1); }
      await new Promise((r) => setTimeout(r, 1000));
    }
  }
}
await applySchema();
console.log(`✓ схема БД (${dialect === 'pg' ? 'PostgreSQL' : 'SQLite'})`);

const { rows: [{ n }] } = await pool.query('SELECT count(*) AS n FROM cases WHERE is_active');
if (Number(n) === 0) {
  sh('node scripts/seed.js');
  console.log('✓ кейсы засеяны из каталога подарков');
} else {
  console.log(`✓ кейсы уже есть (${n})`);
}

// --- фронт ---
if (!existsSync(`${frontendDir}/dist/index.html`)) {
  if (!existsSync(`${frontendDir}/node_modules`)) sh('npm install', { cwd: frontendDir });
  sh('npm run build', { cwd: frontendDir });
}
console.log('✓ фронт собран');

// --- backend ---
const { buildServer } = await import('../src/server.js');
const { config } = await import('../src/config.js');
const server = await buildServer();
await server.listen({ port: config.port, host: '0.0.0.0' });
console.log(`✓ backend на :${config.port} (API + мини-апп)`);

// --- туннель ---
let publicUrl = config.publicUrl;
if (!publicUrl) {
  console.log('… открываю HTTPS-туннель (cloudflared)');
  publicUrl = await startTunnel(config.port);
  console.log(`✓ туннель: ${publicUrl}`);
  // Обработчик /start берёт URL из конфига в момент запроса —
  // прокидываем адрес туннеля, чтобы кнопка «Открыть кейсы» работала.
  config.publicUrl = publicUrl;
}

// --- вебхук + кнопка меню + /start в списке команд ---
const { setWebhook, setMenuButton, setMyCommands } = await import('../src/lib/telegram-api.js');
await setWebhook({ url: `${publicUrl}/api/bot/webhook`, secretToken: config.webhookSecret });
await setMenuButton({ url: publicUrl });
await setMyCommands([{ command: 'start', description: '🎁 Открыть кейсы' }]);
console.log('✓ вебхук, кнопка меню и команды настроены');

console.log(`
┌──────────────────────────────────────────────────────┐
│  ГОТОВО! Проверяй в Telegram:                        │
│                                                      │
│  1. Открой своего бота                               │
│  2. Нажми кнопку «🎁 Кейсы» слева от поля ввода      │
│     (или отправь /start)                             │
│                                                      │
│  Мини-апп: ${publicUrl}
│                                                      │
│  Админка появится автоматически для ID из ADMIN_IDS  │
│  (${process.env.ADMIN_IDS ?? '8486449177'}) — там можно начислить себе ⭐.       │
│                                                      │
│  Остановить: Ctrl+C (туннель и вебхук слетят —       │
│  просто запусти npm run go заново).                  │
└──────────────────────────────────────────────────────┘`);

function startTunnel(port) {
  return new Promise((resolve, reject) => {
    // npm-пакет cloudflared сам скачивает бинарник под ОС.
    const proc = spawn('npx', ['-y', 'cloudflared', 'tunnel', '--url', `http://localhost:${port}`],
      { cwd: backendDir, shell: process.platform === 'win32' });
    const timer = setTimeout(
      () => reject(new Error('cloudflared не выдал URL за 60с — проверь интернет')), 60_000);
    const onData = (chunk) => {
      const match = String(chunk).match(/https:\/\/[a-z0-9-]+\.trycloudflare\.com/);
      if (match) { clearTimeout(timer); resolve(match[0]); }
    };
    proc.stdout.on('data', onData);
    proc.stderr.on('data', onData);
    proc.on('exit', (code) => reject(new Error(`cloudflared завершился (код ${code})`)));
    process.on('exit', () => proc.kill());
  });
}
