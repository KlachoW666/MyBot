import { pool } from '../db/pool.js';
import { redis } from '../redis.js';
import { getAvailableGifts } from '../lib/telegram-api.js';
import { config } from '../config.js';

const CACHE_KEY = 'gifts:catalog';
const REFRESH_LOCK = 'gifts:catalog:refresh';

/**
 * Каталог подарков: Redis-кэш (TTL 5–15 мин) поверх getAvailableGifts,
 * плюс снапшот в gifts_catalog. gift_id не статичны — исчезнувшие из
 * выдачи подарки помечаем is_available=false (распроданы).
 *
 * @param {{ force?: boolean }} [options]
 * @returns {Promise<Array<object>>} доступные подарки
 */
export async function getCatalog({ force = false } = {}) {
  if (!force) {
    const cached = await redis.get(CACHE_KEY).catch(() => null);
    if (cached) return JSON.parse(cached);
  }

  // Один процесс обновляет, остальные при промахе читают устаревший снапшот из БД.
  const lock = await redis.set(REFRESH_LOCK, '1', 'EX', 30, 'NX').catch(() => 'OK');
  if (!lock && !force) return loadSnapshotFromDb();

  try {
    const gifts = await getAvailableGifts();
    const normalized = gifts.map((gift) => ({
      gift_id: gift.id,
      sticker_file_id: gift.sticker?.file_id ?? null,
      emoji: gift.sticker?.emoji ?? null,
      star_count: gift.star_count,
      upgrade_star_count: gift.upgrade_star_count ?? null,
      total_count: gift.total_count ?? null,
      remaining_count: gift.remaining_count ?? null,
      raw: gift,
    }));
    await saveSnapshot(normalized);
    await redis
      .set(CACHE_KEY, JSON.stringify(normalized), 'EX', config.catalogTtl)
      .catch(() => {});
    return normalized;
  } catch (error) {
    // Bot API недоступен — отдаём последний снапшот, чтобы каталог не «моргал».
    const fallback = await loadSnapshotFromDb();
    if (fallback.length > 0) return fallback;
    throw error;
  } finally {
    await redis.del(REFRESH_LOCK).catch(() => {});
  }
}

/** Пометить конкретный подарок распроданным (реакция на GIFT_INVALID). */
export async function markGiftUnavailable(giftId) {
  await pool.query(
    `UPDATE gifts_catalog SET is_available = FALSE, remaining_count = 0, updated_at = now()
     WHERE gift_id = $1`,
    [giftId],
  );
  await redis.del(CACHE_KEY).catch(() => {});
}

async function saveSnapshot(gifts) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    // Всё, чего нет в свежей выдаче, — распродано/снято.
    await client.query(
      `UPDATE gifts_catalog SET is_available = FALSE, updated_at = now()
       WHERE gift_id <> ALL($1::text[])`,
      [gifts.map((gift) => gift.gift_id)],
    );
    for (const gift of gifts) {
      await client.query(
        `INSERT INTO gifts_catalog
           (gift_id, sticker_file_id, emoji, star_count, upgrade_star_count,
            total_count, remaining_count, is_available, snapshot, updated_at)
         VALUES ($1,$2,$3,$4,$5,$6,$7,TRUE,$8,now())
         ON CONFLICT (gift_id) DO UPDATE SET
           sticker_file_id = EXCLUDED.sticker_file_id,
           emoji = EXCLUDED.emoji,
           star_count = EXCLUDED.star_count,
           upgrade_star_count = EXCLUDED.upgrade_star_count,
           total_count = EXCLUDED.total_count,
           remaining_count = EXCLUDED.remaining_count,
           is_available = TRUE,
           snapshot = EXCLUDED.snapshot,
           updated_at = now()`,
        [gift.gift_id, gift.sticker_file_id, gift.emoji, gift.star_count,
         gift.upgrade_star_count, gift.total_count, gift.remaining_count,
         JSON.stringify(gift.raw)],
      );
    }
    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
}

async function loadSnapshotFromDb() {
  const { rows } = await pool.query(
    `SELECT gift_id, sticker_file_id, emoji, star_count, upgrade_star_count,
            total_count, remaining_count, snapshot AS raw
     FROM gifts_catalog WHERE is_available ORDER BY star_count`,
  );
  return rows;
}
