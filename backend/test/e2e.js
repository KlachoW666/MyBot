/**
 * Интеграционный прогон против реальных PostgreSQL + Redis.
 * Bot API стабится перехватом fetch: getMe, getAvailableGifts, sendGift,
 * createInvoiceLink. Первый sendGift отвечает 429, второй — GIFT_INVALID
 * для одного из подарков, чтобы проверить ретраи и рефанд.
 *
 * Запуск: node test/e2e.js (нужны DATABASE_URL/REDIS_URL и запущенные базы).
 */
import assert from 'node:assert/strict';
import crypto from 'node:crypto';

const BOT_TOKEN = process.env.BOT_TOKEN ?? '42:E2E_FAKE_TOKEN';
process.env.BOT_TOKEN = BOT_TOKEN;
process.env.WEBHOOK_SECRET ??= 'e2e-webhook-secret';
process.env.JWT_SECRET ??= 'e2e-jwt-secret';
process.env.DATABASE_URL ??= 'postgres://mybot@localhost:5432/mybot';
process.env.REDIS_URL ??= 'redis://localhost:6379';
process.env.ADMIN_IDS = '8486449177';
process.env.PUBLIC_URL = 'https://app.example.test';

// ---------------------------------------------------------- стаб Bot API
const FAKE_GIFTS = [
  { id: 'gift-cheap', sticker: { file_id: 'stk1', emoji: '🧸' }, star_count: 15 },
  { id: 'gift-mid', sticker: { file_id: 'stk2', emoji: '💝' }, star_count: 50 },
  { id: 'gift-rare', sticker: { file_id: 'stk3', emoji: '💎' },
    star_count: 100, total_count: 500, remaining_count: 3 },
];
let sendGiftCalls = 0;
const sentMessages = [];

const realFetch = globalThis.fetch;
globalThis.fetch = async (url, options) => {
  const str = String(url);
  if (!str.includes('api.telegram.org')) return realFetch(url, options);
  const method = str.split('/').pop();
  const body = options?.body ? JSON.parse(options.body) : {};
  const reply = (payload, status = 200) =>
    new Response(JSON.stringify(payload), { status });

  switch (method) {
    case 'answerPreCheckoutQuery':
      return reply({ ok: true, result: true });
    case 'sendMessage':
      sentMessages.push(body);
      return reply({ ok: true, result: true });
    case 'getAvailableGifts':
      return reply({ ok: true, result: { gifts: FAKE_GIFTS } });
    case 'createInvoiceLink':
      assert.equal(body.currency, 'XTR');
      return reply({ ok: true, result: `https://t.me/invoice/${crypto.randomUUID()}` });
    case 'sendGift':
      sendGiftCalls++;
      if (sendGiftCalls === 1) { // первый вызов — флуд-лимит, ждём ретрая
        return reply({ ok: false, error_code: 429, description: 'Too Many Requests: retry after 1',
          parameters: { retry_after: 1 } }, 429);
      }
      if (body.gift_id === 'gift-rare') { // редкий распродан → рефанд
        return reply({ ok: false, error_code: 400, description: 'Bad Request: GIFT_INVALID' }, 400);
      }
      return reply({ ok: true, result: true });
    default:
      return reply({ ok: false, error_code: 404, description: `no stub for ${method}` }, 404);
  }
};

// ------------------------------------------------------------- запуск
const { buildServer } = await import('../src/server.js');
const { pool } = await import('../src/db/pool.js');
const { redis } = await import('../src/redis.js');
const { readFile } = await import('node:fs/promises');

await redis.flushdb();
await pool.query('DROP TABLE IF EXISTS withdrawals, transactions, inventory, case_items, cases, gifts_catalog, users CASCADE');
await pool.query('DROP TYPE IF EXISTS inventory_status CASCADE');
await pool.query(await readFile(new URL('../db/schema.sql', import.meta.url), 'utf8'));

const server = await buildServer();
await server.ready();

const inject = (opts, token) => server.inject({
  ...opts,
  headers: { 'content-type': 'application/json',
    ...(token ? { authorization: `Bearer ${token}` } : {}) },
});

// Валидный initData, подписанный фейковым токеном (как это делает Telegram).
function makeInitData(userId) {
  const params = new URLSearchParams({
    auth_date: String(Math.floor(Date.now() / 1000)),
    query_id: 'AAE2E',
    user: JSON.stringify({ id: userId, first_name: 'E2E', username: 'e2e_user' }),
  });
  const dcs = [...params.entries()].map(([k, v]) => `${k}=${v}`).sort().join('\n');
  const secret = crypto.createHmac('sha256', 'WebAppData').update(BOT_TOKEN).digest();
  params.set('hash', crypto.createHmac('sha256', secret).update(dcs).digest('hex'));
  return params.toString();
}

// 1. Авторизация: битый initData → 401, валидный → JWT.
let res = await inject({ method: 'POST', url: '/api/auth', payload: { initData: 'auth_date=1&hash=' + '0'.repeat(64) } });
assert.equal(res.statusCode, 401, 'forged initData must be rejected');

res = await inject({ method: 'POST', url: '/api/auth', payload: { initData: makeInitData(777) } });
assert.equal(res.statusCode, 200, res.body);
const { token } = res.json();
assert.ok(token);

// 2. Каталог (стабовый getAvailableGifts) и снапшот в БД.
res = await inject({ method: 'GET', url: '/api/catalog' }, token);
assert.equal(res.statusCode, 200, res.body);
assert.equal(res.json().gifts.length, 3);
assert.equal((await pool.query('SELECT count(*) FROM gifts_catalog')).rows[0].count, '3');

// 3. Кейс + баланс (кредитим депозит как это сделал бы вебхук successful_payment).
const { creditDeposit } = await import('../src/services/payments.js');
await creditDeposit({ userId: 777, amountStars: 500, chargeId: 'chg-1' });
const dup = await creditDeposit({ userId: 777, amountStars: 500, chargeId: 'chg-1' });
assert.equal(dup.duplicate, true, 'deposit must be idempotent by charge_id');

const { rows: [caseRow] } = await pool.query(
  "INSERT INTO cases (slug, title, price_stars) VALUES ('e2e', 'E2E Case', 100) RETURNING id");
for (const gift of FAKE_GIFTS) {
  await pool.query('INSERT INTO case_items (case_id, gift_id, weight) VALUES ($1, $2, $3)',
    [caseRow.id, gift.id, 10]);
}

// 4. Инвойс на пополнение.
res = await inject({ method: 'POST', url: '/api/pay/invoice', payload: { amountStars: 100 } }, token);
assert.equal(res.statusCode, 200, res.body);
assert.match(res.json().link, /^https:\/\/t\.me\/invoice\//);

// 5. Открытие: идемпотентность по ключу + списание ровно один раз.
const idemKey = crypto.randomUUID();
res = await inject({ method: 'POST', url: `/api/cases/${caseRow.id}/open`, payload: { idempotencyKey: idemKey } }, token);
assert.equal(res.statusCode, 200, res.body);
const opened = res.json();
assert.ok(opened.gift.gift_id);
assert.equal(opened.balance, 400);

res = await inject({ method: 'POST', url: `/api/cases/${caseRow.id}/open`, payload: { idempotencyKey: idemKey } }, token);
assert.equal(res.json().duplicate, true, 'same idempotency key must not open twice');
assert.equal(res.json().balance, 400, 'no double charge');

// Недостаточно баланса → 402.
await pool.query('UPDATE users SET balance = 5 WHERE telegram_id = 777');
res = await inject({ method: 'POST', url: `/api/cases/${caseRow.id}/open`, payload: { idempotencyKey: crypto.randomUUID() } }, token);
assert.equal(res.statusCode, 402);
await pool.query('UPDATE users SET balance = 400 WHERE telegram_id = 777');

// 6. Вывод: первый sendGift получает 429 и ретраится (или рефандится, если выпал rare).
res = await inject({ method: 'POST', url: '/api/withdraw', payload: { inventoryId: opened.inventoryId } }, token);
assert.equal(res.statusCode, 200, res.body);
const wd = res.json();
if (opened.gift.gift_id === 'gift-rare') {
  assert.equal(wd.status, 'refunded');
  assert.equal(wd.refundedStars, 100);
  const { rows: [u] } = await pool.query('SELECT balance FROM users WHERE telegram_id = 777');
  assert.equal(Number(u.balance), 500, 'refund credited');
} else {
  assert.equal(wd.status, 'withdrawn');
  assert.ok(sendGiftCalls >= 2, '429 must be retried');
}

// Повторный вывод того же предмета — 409.
res = await inject({ method: 'POST', url: '/api/withdraw', payload: { inventoryId: opened.inventoryId } }, token);
assert.equal(res.statusCode, 409, 'double withdraw must be rejected');

// 7. Инвентарь отражает финальный статус.
res = await inject({ method: 'GET', url: '/api/me/inventory' }, token);
const item = res.json().items.find((entry) => entry.id === opened.inventoryId);
assert.ok(['withdrawn', 'refunded'].includes(item.status));

// 8. Вебхук: неверный секрет — 401, верный — 200.
res = await server.inject({ method: 'POST', url: '/api/bot/webhook',
  headers: { 'content-type': 'application/json', 'x-telegram-bot-api-secret-token': 'wrong' },
  payload: { update_id: 1 } });
assert.equal(res.statusCode, 401, 'webhook must verify secret token');

res = await server.inject({ method: 'POST', url: '/api/bot/webhook',
  headers: { 'content-type': 'application/json',
    'x-telegram-bot-api-secret-token': process.env.WEBHOOK_SECRET },
  payload: { update_id: 1 } });
assert.equal(res.statusCode, 200);

// 9. Оплата через вебхук: pre_checkout + successful_payment → зачисление.
const paidPayload = JSON.stringify({ t: 'topup', uid: 777, amt: 250, n: 'e2e' });
const paidFrom = { id: 777, is_bot: false, first_name: 'E2E', username: 'e2e_user' };
res = await server.inject({ method: 'POST', url: '/api/bot/webhook',
  headers: { 'content-type': 'application/json',
    'x-telegram-bot-api-secret-token': process.env.WEBHOOK_SECRET },
  payload: { update_id: 3, message: {
    message_id: 9, date: Math.floor(Date.now() / 1000),
    chat: { id: 777, type: 'private' }, from: paidFrom,
    successful_payment: { currency: 'XTR', total_amount: 250, invoice_payload: paidPayload,
      telegram_payment_charge_id: 'chg-webhook-1' } } } });
assert.equal(res.statusCode, 200);
const { rows: [afterPay] } = await pool.query('SELECT balance FROM users WHERE telegram_id = 777');
const balanceBeforePay = opened.gift.gift_id === 'gift-rare' ? 500 : 400;
assert.equal(Number(afterPay.balance), balanceBeforePay + 250, 'webhook payment credited');

// Платёж от юзера, которого ещё НЕТ в БД (никогда не открывал апп) — должен создать его.
const freshFrom = { id: 555000111, is_bot: false, first_name: 'Fresh' };
res = await server.inject({ method: 'POST', url: '/api/bot/webhook',
  headers: { 'content-type': 'application/json',
    'x-telegram-bot-api-secret-token': process.env.WEBHOOK_SECRET },
  payload: { update_id: 4, message: {
    message_id: 10, date: Math.floor(Date.now() / 1000),
    chat: { id: 555000111, type: 'private' }, from: freshFrom,
    successful_payment: { currency: 'XTR', total_amount: 77,
      invoice_payload: JSON.stringify({ t: 'topup', uid: 555000111, amt: 77, n: 'x' }),
      telegram_payment_charge_id: 'chg-fresh-1' } } } });
assert.equal(res.statusCode, 200);
const { rows: [freshUser] } = await pool.query('SELECT balance FROM users WHERE telegram_id = 555000111');
assert.ok(freshUser, 'payment must create unknown user');
assert.equal(Number(freshUser.balance), 77, 'fresh user credited');

// 10. Админка: обычный юзер → 403; админ (8486449177) — статы и корректировка баланса.
res = await inject({ method: 'GET', url: '/api/admin/stats' }, token);
assert.equal(res.statusCode, 403, 'non-admin must be rejected');

res = await inject({ method: 'POST', url: '/api/auth', payload: { initData: makeInitData(8486449177) } });
const adminToken = res.json().token;
assert.equal(res.json().user.is_admin, true);

res = await inject({ method: 'GET', url: '/api/admin/stats' }, adminToken);
assert.equal(res.statusCode, 200, res.body);
assert.ok(res.json().users >= 2);

res = await inject({ method: 'POST', url: '/api/admin/balance',
  payload: { telegram_id: 8486449177, amount: 1000 } }, adminToken);
assert.equal(res.statusCode, 200, res.body);
assert.equal(res.json().balance, 1000);

// Шестой активный кейс — отказ (на экран помещается максимум 5).
for (let i = 0; i < 5; i++) {
  res = await inject({ method: 'POST', url: '/api/admin/cases', payload: {
    slug: `extra-${i}`, title: `Extra ${i}`, price_stars: 10,
    items: [{ gift_id: 'gift-cheap', weight: 1 }] } }, adminToken);
  if (i < 4) assert.equal(res.statusCode, 200, res.body); // + кейс 'e2e' = 5 активных
  else assert.equal(res.statusCode, 409, 'sixth active case must be rejected');
}

// 11. /start → приветствие с web_app-кнопкой «Открыть кейсы».
res = await server.inject({ method: 'POST', url: '/api/bot/webhook',
  headers: { 'content-type': 'application/json',
    'x-telegram-bot-api-secret-token': process.env.WEBHOOK_SECRET },
  payload: { update_id: 5, message: {
    message_id: 11, date: Math.floor(Date.now() / 1000), text: '/start',
    chat: { id: 777, type: 'private' },
    from: { id: 777, is_bot: false, first_name: 'E2E' } } } });
assert.equal(res.statusCode, 200);
const startReply = sentMessages.find((msg) => msg.reply_markup);
assert.ok(startReply, '/start must reply with a button');
assert.equal(startReply.reply_markup.inline_keyboard[0][0].web_app.url,
  'https://app.example.test', '/start button must open the mini app');

console.log('E2E OK: auth, catalog, invoice, open (idempotent), withdraw (429 retry / refund), webhook secret, payment credit, admin guard + 5-case limit, /start web_app button');
await server.close();
await pool.end();
redis.disconnect();
