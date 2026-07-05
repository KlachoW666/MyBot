/**
 * Локальный эмулятор Telegram Bot API для разработки и тестов
 * (боевой сервер ходит в https://api.telegram.org, эмулятор включается
 * через TELEGRAM_API_BASE=http://localhost:8081).
 *
 * Реализует: getMe, getAvailableGifts, createInvoiceLink, sendGift,
 * answerPreCheckoutQuery, sendMessage, setWebhook.
 *
 * Плюс dev-ручка POST /emu/pay {user_id, amount} — эмулирует полный цикл
 * оплаты Stars: шлёт на вебхук backend'а pre_checkout_query, а затем
 * successful_payment с корректным secret_token.
 *
 * Запуск: node dev/telegram-emulator.js  (порт EMU_PORT, по умолчанию 8081)
 */
import Fastify from 'fastify';
import crypto from 'node:crypto';

const PORT = Number(process.env.EMU_PORT ?? 8081);
const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080';
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET ?? 'dev-webhook-secret';

const GIFTS = [
  { id: 'emu-bear', emoji: '🧸', star_count: 15 },
  { id: 'emu-heart', emoji: '💝', star_count: 15 },
  { id: 'emu-rose', emoji: '🌹', star_count: 25 },
  { id: 'emu-cake', emoji: '🎂', star_count: 50 },
  { id: 'emu-champagne', emoji: '🍾', star_count: 50 },
  { id: 'emu-bouquet', emoji: '💐', star_count: 50 },
  { id: 'emu-rocket', emoji: '🚀', star_count: 100 },
  { id: 'emu-trophy', emoji: '🏆', star_count: 100 },
  { id: 'emu-ring', emoji: '💍', star_count: 200 },
  { id: 'emu-unicorn', emoji: '🦄', star_count: 350, total_count: 5000, remaining_count: 341 },
  { id: 'emu-crown', emoji: '👑', star_count: 500, total_count: 2000, remaining_count: 87 },
  { id: 'emu-diamond', emoji: '💎', star_count: 1000, total_count: 1000, remaining_count: 12 },
].map((gift) => ({
  ...gift,
  sticker: { file_id: `stk-${gift.id}`, emoji: gift.emoji },
}));

// Хвост URL вида /bot<token>/<method>; токен не проверяем — это dev.
const app = Fastify({ logger: { level: 'warn' } });
const invoices = new Map(); // slug → { payload, amount }
let flakySendGift = 0;      // каждый 4-й sendGift отвечает 429 — проверка ретраев

const ok = (result) => ({ ok: true, result });

app.post('/bot:token/:method', async (request, reply) => {
  const { method } = request.params;
  const body = request.body ?? {};

  switch (method) {
    case 'getMe':
      return ok({ id: 8511073187, is_bot: true, first_name: 'GiftCasesBot',
        username: 'gift_cases_dev_bot' });

    case 'getAvailableGifts':
      return ok({ gifts: GIFTS });

    case 'createInvoiceLink': {
      const slug = crypto.randomBytes(8).toString('hex');
      invoices.set(slug, { payload: body.payload, amount: body.prices?.[0]?.amount });
      return ok(`https://t.me/$${slug}`);
    }

    case 'sendGift': {
      flakySendGift++;
      if (flakySendGift % 4 === 0) {
        return reply.status(429).send({ ok: false, error_code: 429,
          description: 'Too Many Requests: retry after 1', parameters: { retry_after: 1 } });
      }
      const gift = GIFTS.find((g) => g.id === body.gift_id);
      if (!gift) {
        return reply.status(400).send({ ok: false, error_code: 400,
          description: 'Bad Request: GIFT_INVALID' });
      }
      app.log.warn({ user_id: body.user_id, gift_id: body.gift_id }, 'EMU: gift sent');
      return ok(true);
    }

    case 'answerPreCheckoutQuery':
    case 'setChatMenuButton':
      return ok(true);

    case 'sendMessage':
      app.log.warn({ chat_id: body.chat_id, text: body.text }, 'EMU: message');
      return ok({ message_id: Date.now(), chat: { id: body.chat_id }, text: body.text });

    case 'setWebhook':
      return ok(true);

    default:
      return reply.status(404).send({ ok: false, error_code: 404,
        description: `EMU: method ${method} not implemented` });
  }
});

/**
 * POST /emu/pay { user_id, amount, username?, first_name? }
 * Полный цикл оплаты: pre_checkout_query → successful_payment на вебхук.
 */
app.post('/emu/pay', async (request, reply) => {
  const { user_id, amount, username = 'devuser', first_name = 'Dev' } = request.body ?? {};
  if (!user_id || !amount) return reply.status(400).send({ error: 'user_id and amount required' });

  const payload = JSON.stringify({ t: 'topup', uid: user_id, amt: amount,
    n: crypto.randomBytes(6).toString('hex') });
  const from = { id: user_id, is_bot: false, first_name, username };
  const post = (update) => fetch(`${BACKEND_URL}/api/bot/webhook`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-telegram-bot-api-secret-token': WEBHOOK_SECRET,
    },
    body: JSON.stringify(update),
  });

  const pre = await post({ update_id: 1, pre_checkout_query: {
    id: 'q1', from, currency: 'XTR', total_amount: amount, invoice_payload: payload } });
  const pay = await post({ update_id: 2, message: {
    message_id: 1, date: Math.floor(Date.now() / 1000), chat: { id: user_id, type: 'private' }, from,
    successful_payment: {
      currency: 'XTR', total_amount: amount, invoice_payload: payload,
      telegram_payment_charge_id: `emu-${crypto.randomUUID()}`,
      provider_payment_charge_id: 'emu',
    } } });
  return { pre_checkout: pre.status, payment: pay.status };
});

await app.listen({ port: PORT, host: '0.0.0.0' });
console.log(`Telegram Bot API emulator on :${PORT} → webhook ${BACKEND_URL}/api/bot/webhook`);
