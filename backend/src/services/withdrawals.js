import { pool, withTransaction } from '../db/pool.js';
import { acquireLock } from '../redis.js';
import { sendGift, getMyStarBalance, TelegramApiError } from '../lib/telegram-api.js';
import { markGiftUnavailable } from './catalog.js';
import { AppError } from '../lib/errors.js';

const SEND_ATTEMPTS = 3;

/**
 * Вывод подарка из inventory реальным Telegram-подарком.
 *
 * Идемпотентность в три слоя:
 *   1. Redis-лок withdraw:{inventoryId} — отсекает конкурентные клики дёшево;
 *   2. UPDATE ... WHERE status='won' — статус меняется ровно один раз;
 *   3. withdrawals.inventory_id UNIQUE — БД-инвариант «один вывод на предмет».
 *
 * Статусы: won → withdraw_pending → withdrawn | refunded.
 * sendGift зовём ПОСЛЕ коммита withdraw_pending: предмет уже заблокирован,
 * упавший процесс оставит его в pending для доследования, а не отправит дважды.
 *
 * userId — проверенный telegram_id из JWT-сессии: подарок уходит напрямую ему.
 */
export async function withdrawItem({ userId, inventoryId, log }) {
  const release = await acquireLock(`withdraw:${inventoryId}`, 60);
  if (!release) {
    throw new AppError(409, 'WITHDRAW_IN_PROGRESS', 'Withdrawal already in progress');
  }

  try {
    // Шаг 1: won → withdraw_pending + строка в withdrawals, атомарно.
    const item = await withTransaction(async (client) => {
      const { rows: [row] } = await client.query(
        `UPDATE inventory SET status = 'withdraw_pending', updated_at = now()
         WHERE id = $1 AND user_id = $2 AND status = 'won'
         RETURNING id, gift_id, star_value`,
        [inventoryId, userId],
      );
      if (!row) {
        throw new AppError(409, 'ITEM_NOT_WITHDRAWABLE',
          'Item not found, not yours, or already withdrawn');
      }
      await client.query(
        `INSERT INTO withdrawals (inventory_id, user_id, gift_id, status)
         VALUES ($1, $2, $3, 'pending')`,
        [row.id, userId, row.gift_id],
      );
      return row;
    });

    // Баланс бота проверяем до отправки: не хватает — сразу понятная ошибка.
    const botBalance = await getMyStarBalance().catch(() => null);
    if (botBalance !== null && botBalance < item.star_value) {
      log?.error({ botBalance, need: item.star_value }, 'bot star balance too low');
      await withTransaction(async (client) => {
        await client.query(
          `UPDATE inventory SET status = 'won', updated_at = now()
           WHERE id = $1 AND status = 'withdraw_pending'`, [inventoryId]);
        await client.query(
          `DELETE FROM withdrawals WHERE inventory_id = $1 AND status = 'pending'`, [inventoryId]);
      });
      throw new AppError(503, 'BOT_BALANCE_LOW',
        'Выводы временно недоступны — казна бота пополняется. Попробуй позже.');
    }

    // Шаг 2: отправка подарка с ретраями на 429/5xx.
    let lastError;
    for (let attempt = 1; attempt <= SEND_ATTEMPTS; attempt++) {
      await pool.query(
        'UPDATE withdrawals SET attempts = attempts + 1, updated_at = now() WHERE inventory_id = $1',
        [inventoryId],
      );
      try {
        const result = await sendGift({ userId, giftId: item.gift_id, text: 'Поздравляем с выигрышем! 🎉' });
        log?.info({ userId, inventoryId, giftId: item.gift_id }, 'gift sent');
        await finalize(inventoryId, 'withdrawn', 'sent', result ?? { ok: true });
        return { status: 'withdrawn' };
      } catch (error) {
        lastError = error;
        if (!(error instanceof TelegramApiError)) break;

        if (error.isGiftInvalid) {
          // Подарок распродан между выигрышем и выводом → рефанд в Stars.
          log?.warn({ userId, inventoryId, giftId: item.gift_id }, 'gift invalid, refunding');
          await markGiftUnavailable(item.gift_id);
          await refund({ userId, inventoryId, giftId: item.gift_id,
            amount: item.star_value, tgError: error });
          return { status: 'refunded', refundedStars: item.star_value };
        }
        if (error.retryAfter != null && attempt < SEND_ATTEMPTS) {
          log?.warn({ inventoryId, retryAfter: error.retryAfter }, 'sendGift 429, retrying');
          await new Promise((r) => setTimeout(r, Math.min(error.retryAfter, 30) * 1000));
          continue;
        }
        if (error.isTransient && attempt < SEND_ATTEMPTS) {
          await new Promise((r) => setTimeout(r, 2 ** attempt * 1000));
          continue;
        }
        break; // окончательная ошибка (в т.ч. BALANCE_TOO_LOW у бота)
      }
    }

    // Не отправили: предмет возвращаем в won, попытку помечаем failed.
    log?.error({ err: lastError, userId, inventoryId }, 'withdrawal failed');
    await withTransaction(async (client) => {
      await client.query(
        `UPDATE inventory SET status = 'won', updated_at = now()
         WHERE id = $1 AND status = 'withdraw_pending'`,
        [inventoryId],
      );
      // Строку удаляем, чтобы UNIQUE(inventory_id) не блокировал повторную попытку.
      await client.query(
        'DELETE FROM withdrawals WHERE inventory_id = $1 AND status = $2',
        [inventoryId, 'pending'],
      );
    });

    if (lastError instanceof TelegramApiError && lastError.isInsufficientBotBalance) {
      throw new AppError(503, 'BOT_BALANCE_LOW',
        'Withdrawals temporarily unavailable, try again later');
    }
    throw new AppError(502, 'WITHDRAW_FAILED', 'Failed to send gift, item returned to inventory');
  } finally {
    await release();
  }
}

async function finalize(inventoryId, invStatus, wdStatus, tgResult) {
  await withTransaction(async (client) => {
    await client.query(
      `UPDATE inventory SET status = $2, updated_at = now()
       WHERE id = $1 AND status = 'withdraw_pending'`,
      [inventoryId, invStatus],
    );
    await client.query(
      `UPDATE withdrawals SET status = $2, tg_result = $3, updated_at = now()
       WHERE inventory_id = $1`,
      [inventoryId, wdStatus, JSON.stringify(tgResult)],
    );
  });
}

async function refund({ userId, inventoryId, giftId, amount, tgError }) {
  await withTransaction(async (client) => {
    // Переход статуса — единственный «пропуск» к зачислению: повторный вызов
    // обновит 0 строк и денег не добавит.
    const { rowCount } = await client.query(
      `UPDATE inventory SET status = 'refunded', updated_at = now()
       WHERE id = $1 AND status = 'withdraw_pending'`,
      [inventoryId],
    );
    if (rowCount === 0) return;
    await client.query(
      `UPDATE withdrawals SET status = 'refunded', tg_result = $2, updated_at = now()
       WHERE inventory_id = $1`,
      [inventoryId, JSON.stringify({ error_code: tgError.errorCode, description: tgError.description })],
    );
    await client.query(
      `UPDATE users SET balance = balance + $1, updated_at = now() WHERE telegram_id = $2`,
      [amount, userId],
    );
    await client.query(
      `INSERT INTO transactions (user_id, type, amount, ref, idempotency_key)
       VALUES ($1, 'refund', $2, $3, $4)
       ON CONFLICT (idempotency_key) DO NOTHING`,
      [userId, amount, String(inventoryId), `refund:${inventoryId}`],
    );
  });
}
