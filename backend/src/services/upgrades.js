import crypto from 'node:crypto';
import { pool, withTransaction } from '../db/pool.js';
import { acquireLock } from '../redis.js';
import { AppError } from '../lib/errors.js';
import { config } from '../config.js';

/**
 * Шанс апгрейда в базисных пунктах (1 bp = 0.01%).
 * chance = from/to: 10⭐ → 100⭐ = 1000 bp = 10%.
 * Клампится в [upgradeMinBp, upgradeMaxBp] (по умолчанию 1%..75%).
 */
export function computeChanceBp(fromValue, toValue) {
  const raw = Math.round((fromValue / toValue) * 10_000);
  return Math.min(config.upgradeMaxBp, Math.max(config.upgradeMinBp, raw));
}

/** Предмет юзера + список доступных целей с посчитанными шансами. */
export async function getUpgradeOptions({ userId, inventoryId }) {
  const { rows: [item] } = await pool.query(
    `SELECT i.id, i.gift_id, i.star_value, gc.emoji
     FROM inventory i JOIN gifts_catalog gc ON gc.gift_id = i.gift_id
     WHERE i.id = $1 AND i.user_id = $2 AND i.status = 'won'`,
    [inventoryId, userId],
  );
  if (!item) throw new AppError(404, 'ITEM_NOT_FOUND', 'Предмет не найден или уже использован');

  // Цель всегда дороже предмета — иначе это не апгрейд.
  const { rows: targets } = await pool.query(
    `SELECT gift_id, emoji, star_count, total_count, remaining_count
     FROM gifts_catalog
     WHERE is_available AND star_count > $1
     ORDER BY star_count`,
    [item.star_value],
  );
  return {
    item,
    min_bp: config.upgradeMinBp,
    max_bp: config.upgradeMaxBp,
    targets: targets.map((gift) => ({
      ...gift,
      chance_bp: computeChanceBp(item.star_value, gift.star_count),
    })),
  };
}

/**
 * Апгрейд: предмет (status=won) против более дорогого подарка из каталога.
 * Ролл ТОЛЬКО на сервере (CSPRNG). Победа — предмет становится целевым
 * подарком; поражение — предмет сгорает (status=lost). Всё в транзакции,
 * от даблкликов — Redis-лок + условие status='won' в UPDATE.
 */
export async function performUpgrade({ userId, inventoryId, targetGiftId, log }) {
  const release = await acquireLock(`upgrade:${inventoryId}`, 30);
  if (!release) throw new AppError(409, 'UPGRADE_IN_PROGRESS', 'Апгрейд уже выполняется');

  try {
    const { rows: [item] } = await pool.query(
      `SELECT id, gift_id, star_value FROM inventory
       WHERE id = $1 AND user_id = $2 AND status = 'won'`,
      [inventoryId, userId],
    );
    if (!item) throw new AppError(404, 'ITEM_NOT_FOUND', 'Предмет не найден или уже использован');

    const { rows: [target] } = await pool.query(
      `SELECT gift_id, emoji, star_count, sticker_file_id FROM gifts_catalog
       WHERE gift_id = $1 AND is_available`,
      [targetGiftId],
    );
    if (!target) throw new AppError(409, 'TARGET_UNAVAILABLE', 'Целевой подарок распродан');
    if (target.star_count <= item.star_value) {
      throw new AppError(400, 'BAD_TARGET', 'Цель должна быть дороже вашего предмета');
    }

    const chanceBp = computeChanceBp(item.star_value, target.star_count);
    // UPGRADE_FORCE_ROLL — только для тестов/QA: детерминированный исход.
    const rollBp = process.env.UPGRADE_FORCE_ROLL !== undefined
      ? Number(process.env.UPGRADE_FORCE_ROLL)
      : crypto.randomInt(10_000);
    const won = rollBp < chanceBp;

    await withTransaction(async (client) => {
      const { rowCount } = await client.query(
        won
          ? `UPDATE inventory SET gift_id = $2, star_value = $3, updated_at = now()
             WHERE id = $1 AND status = 'won'`
          : `UPDATE inventory SET status = 'lost', updated_at = now()
             WHERE id = $1 AND status = 'won'`,
        won ? [item.id, target.gift_id, target.star_count] : [item.id],
      );
      if (rowCount === 0) throw new AppError(409, 'ITEM_NOT_FOUND', 'Предмет уже использован');
      await client.query(
        `INSERT INTO upgrades
           (user_id, inventory_id, from_gift_id, from_value, to_gift_id, to_value,
            chance_bp, roll_bp, won)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
        [userId, item.id, item.gift_id, item.star_value,
         target.gift_id, target.star_count, chanceBp, rollBp, won],
      );
    });

    log?.info({ userId, inventoryId, from: item.gift_id, to: target.gift_id,
      chanceBp, rollBp, won }, 'upgrade performed');

    return {
      won,
      chance_bp: chanceBp,
      gift: won ? {
        gift_id: target.gift_id,
        emoji: target.emoji,
        star_count: target.star_count,
      } : null,
      lost: won ? null : {
        gift_id: item.gift_id,
        star_value: item.star_value,
      },
    };
  } finally {
    await release();
  }
}
