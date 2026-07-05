import crypto from 'node:crypto';
import { withTransaction } from '../db/pool.js';
import { createInvoiceLink } from '../lib/telegram-api.js';
import { AppError } from '../lib/errors.js';

const MIN_TOPUP = 1;
const MAX_TOPUP = 10_000;

/**
 * Ссылка на инвойс пополнения баланса в Stars (currency XTR).
 * payload вернётся в pre_checkout_query/successful_payment — по нему
 * сверяем юзера и сумму на вебхуке.
 */
export async function createTopUpInvoice({ userId, amountStars }) {
  if (!Number.isInteger(amountStars) || amountStars < MIN_TOPUP || amountStars > MAX_TOPUP) {
    throw new AppError(400, 'BAD_AMOUNT', `Amount must be ${MIN_TOPUP}..${MAX_TOPUP} stars`);
  }
  const payload = JSON.stringify({
    t: 'topup',
    uid: userId,
    amt: amountStars,
    n: crypto.randomBytes(6).toString('hex'),
  });
  const link = await createInvoiceLink({
    title: `Пополнение на ${amountStars} ⭐`,
    description: 'Пополнение баланса для открытия кейсов',
    payload,
    amountStars,
  });
  return { link };
}

/** Проверка payload на pre_checkout_query: наш ли инвойс и сходится ли сумма. */
export function parseTopUpPayload(rawPayload, { fromId, totalAmount }) {
  let payload;
  try {
    payload = JSON.parse(rawPayload);
  } catch {
    return null;
  }
  if (payload?.t !== 'topup') return null;
  if (payload.uid !== fromId) return null;        // платить может только сам юзер
  if (payload.amt !== totalAmount) return null;   // сумма не подменена
  return payload;
}

/**
 * Зачисление после successful_payment. Идемпотентно по
 * telegram_payment_charge_id: ретрай вебхука не задвоит депозит.
 */
export async function creditDeposit({ userId, username, firstName, amountStars, chargeId }) {
  return withTransaction(async (client) => {
    // Сначала юзер: платить может и тот, кто ни разу не открывал мини-апп.
    await client.query(
      `INSERT INTO users (telegram_id, username, first_name)
       VALUES ($1, $2, $3)
       ON CONFLICT (telegram_id) DO UPDATE SET
         username = COALESCE(EXCLUDED.username, users.username),
         first_name = COALESCE(EXCLUDED.first_name, users.first_name),
         updated_at = now()`,
      [userId, username ?? null, firstName ?? null],
    );

    const { rowCount } = await client.query(
      `INSERT INTO transactions (user_id, type, amount, ref, idempotency_key)
       VALUES ($1, 'deposit', $2, $3, $4)
       ON CONFLICT (idempotency_key) DO NOTHING`,
      [userId, amountStars, chargeId, `tgpay:${chargeId}`],
    );
    if (rowCount === 0) return { duplicate: true }; // уже зачислено

    await client.query(
      `UPDATE users SET balance = balance + $1, updated_at = now() WHERE telegram_id = $2`,
      [amountStars, userId],
    );
    return { duplicate: false };
  });
}
