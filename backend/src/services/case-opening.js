import { pool, withTransaction } from '../db/pool.js';
import { pickWeighted } from '../lib/rng.js';
import { AppError } from '../lib/errors.js';

/**
 * Открытие кейса. Вся логика на сервере, в одной транзакции:
 *   1. SELECT balance FOR UPDATE (сериализуем конкурентные открытия юзера);
 *   2. проверка и списание баланса;
 *   3. RNG по весам среди ДОСТУПНЫХ подарков кейса;
 *   4. inventory(status=won) + transactions(type=case_open).
 *
 * Идемпотентность: idempotencyKey от клиента (uuid на клик). Повтор с тем же
 * ключом возвращает уже сохранённый результат, а не открывает второй кейс.
 *
 * @returns {Promise<{ inventoryId, gift, balance, duplicate: boolean }>}
 */
export async function openCase({ userId, caseId, idempotencyKey }) {
  const idemKey = `open:${userId}:${idempotencyKey}`;

  // Повторный клик/ретрай сети — отдаём прежний результат.
  const existing = await findExistingOpen(idemKey);
  if (existing) return { ...existing, duplicate: true };

  const { rows: [caseRow] } = await pool.query(
    'SELECT id, price_stars FROM cases WHERE id = $1 AND is_active',
    [caseId],
  );
  if (!caseRow) throw new AppError(404, 'CASE_NOT_FOUND', 'Case not found');

  const { rows: items } = await pool.query(
    `SELECT ci.gift_id, ci.weight, gc.star_count, gc.sticker_file_id, gc.emoji
     FROM case_items ci
     JOIN gifts_catalog gc ON gc.gift_id = ci.gift_id
     WHERE ci.case_id = $1 AND gc.is_available`,
    [caseId],
  );
  if (items.length === 0) {
    throw new AppError(409, 'CASE_EMPTY', 'All gifts in this case are sold out');
  }

  const prize = pickWeighted(items);

  try {
    return await withTransaction(async (client) => {
      const { rows: [user] } = await client.query(
        'SELECT balance FROM users WHERE telegram_id = $1 FOR UPDATE',
        [userId],
      );
      if (!user) throw new AppError(401, 'USER_NOT_FOUND', 'Re-authenticate');
      if (user.balance < caseRow.price_stars) {
        throw new AppError(402, 'INSUFFICIENT_BALANCE',
          `Need ${caseRow.price_stars} stars, have ${user.balance}`);
      }

      const { rows: [updated] } = await client.query(
        `UPDATE users SET balance = balance - $1, updated_at = now()
         WHERE telegram_id = $2 RETURNING balance`,
        [caseRow.price_stars, userId],
      );

      const { rows: [inv] } = await client.query(
        `INSERT INTO inventory (user_id, case_id, gift_id, star_value, status)
         VALUES ($1, $2, $3, $4, 'won') RETURNING id`,
        [userId, caseId, prize.gift_id, prize.star_count],
      );

      // UNIQUE(idempotency_key): гонка двух одинаковых запросов упадёт здесь
      // с 23505, транзакция откатится, победитель уже записал результат.
      await client.query(
        `INSERT INTO transactions (user_id, type, amount, ref, idempotency_key)
         VALUES ($1, 'case_open', $2, $3, $4)`,
        [userId, -caseRow.price_stars, String(inv.id), idemKey],
      );

      return {
        inventoryId: inv.id,
        gift: {
          gift_id: prize.gift_id,
          star_count: prize.star_count,
          sticker_file_id: prize.sticker_file_id,
          emoji: prize.emoji,
        },
        balance: Number(updated.balance),
        duplicate: false,
      };
    });
  } catch (error) {
    if (error.code === '23505') { // проигравший гонку — вернуть результат победителя
      const winner = await findExistingOpen(idemKey);
      if (winner) return { ...winner, duplicate: true };
    }
    throw error;
  }
}

async function findExistingOpen(idemKey) {
  const { rows: [row] } = await pool.query(
    `SELECT i.id AS inventory_id, i.gift_id, i.star_value,
            gc.sticker_file_id, gc.emoji, u.balance
     FROM transactions t
     JOIN inventory i ON i.id = t.ref::bigint
     JOIN gifts_catalog gc ON gc.gift_id = i.gift_id
     JOIN users u ON u.telegram_id = t.user_id
     WHERE t.idempotency_key = $1`,
    [idemKey],
  );
  if (!row) return null;
  return {
    inventoryId: row.inventory_id,
    gift: {
      gift_id: row.gift_id,
      star_count: row.star_value,
      sticker_file_id: row.sticker_file_id,
      emoji: row.emoji,
    },
    balance: Number(row.balance),
  };
}
