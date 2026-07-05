/**
 * UI-прогон мини-аппа в Chromium: реальный initData, подписанный ботовским
 * токеном (в точности как это делает Telegram), мок window.Telegram.WebApp.
 * Скриншоты: кейсы, превью кейса, рулетка, приз, профиль, пополнение, админка.
 */
import { chromium } from 'playwright';
// Запуск: BOT_TOKEN=... OUT=./shots node dev/ui-test.mjs (нужны backend:8080, vite:5173)
import crypto from 'node:crypto';

const BOT_TOKEN = process.env.BOT_TOKEN ?? (() => { throw new Error('BOT_TOKEN env required'); })();
const OUT = process.env.OUT ?? '.';

function makeInitData(user) {
  const params = new URLSearchParams({
    auth_date: String(Math.floor(Date.now() / 1000)),
    query_id: 'AAFtest',
    user: JSON.stringify(user),
  });
  const dcs = [...params.entries()].map(([k, v]) => `${k}=${v}`).sort().join('\n');
  const secret = crypto.createHmac('sha256', 'WebAppData').update(BOT_TOKEN).digest();
  params.set('hash', crypto.createHmac('sha256', secret).update(dcs).digest('hex'));
  return params.toString();
}

const adminInitData = makeInitData({
  id: 8486449177, first_name: 'Админ', username: 'boss', language_code: 'ru',
});

const browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium-1194/chrome-linux/chrome' });
const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });

await page.addInitScript((initData) => {
  window.Telegram = {
    WebApp: {
      initData,
      ready() {}, expand() {},
      openInvoice(url, cb) { setTimeout(() => cb('paid'), 300); },
      HapticFeedback: { impactOccurred() {}, notificationOccurred() {} },
    },
  };
}, adminInitData);

page.on('console', (msg) => { if (msg.type() === 'error') console.log('CONSOLE ERR:', msg.text()); });
page.on('pageerror', (err) => console.log('PAGE ERR:', err.message));

await page.goto(process.env.APP_URL ?? 'http://localhost:5173/');
await page.waitForSelector('.case-card', { timeout: 15000 });
await page.screenshot({ path: `${OUT}/1-cases.png` });

// Превью кейса «Золото» → открытие → рулетка → приз
await page.click('.cases-grid .case-card >> nth=2');
await page.waitForSelector('.btn-big');
await page.screenshot({ path: `${OUT}/2-preview.png` });
await page.click('.btn-big');
await page.waitForSelector('.roulette', { timeout: 10000 });
await page.waitForTimeout(1700);
await page.screenshot({ path: `${OUT}/3-spin.png` });
await page.waitForSelector('.prize-burst', { timeout: 10000 });
await page.screenshot({ path: `${OUT}/4-reveal.png` });
await page.click('.btn-ghost');

// Апгрейд: предмет → цель → форс-выигрыш
await page.click('.tab >> nth=1');
await page.waitForSelector('.upgrade-pick', { timeout: 10000 });
await page.click('.upgrade-pick >> nth=0');
await page.waitForSelector('.upgrade-slot-full', { timeout: 10000 });
await page.waitForTimeout(500);
await page.click('.upgrade-pick >> nth=1');
await page.waitForTimeout(400);
await page.screenshot({ path: `${OUT}/10-upgrade.png` });
await page.click('.btn-big');
await page.waitForSelector('.upgrade-result', { timeout: 15000 });
await page.waitForTimeout(300);
await page.screenshot({ path: `${OUT}/11-upgrade-result.png` });

// Профиль с инвентарём
await page.click('.tab >> nth=2');
await page.waitForSelector('.profile-card');
await page.waitForTimeout(400);
await page.screenshot({ path: `${OUT}/5-profile.png` });

// Шит пополнения
await page.click('.balance-pill');
await page.waitForSelector('.sheet');
await page.waitForTimeout(300);
await page.screenshot({ path: `${OUT}/6-topup.png` });
await page.click('.sheet-backdrop', { position: { x: 10, y: 60 } });

// Админка
await page.click('.tab >> nth=3');
await page.waitForSelector('.admin-stats', { timeout: 10000 });
await page.waitForTimeout(400);
await page.screenshot({ path: `${OUT}/7-admin.png` });

// Вывод подарка из профиля
await page.click('.tab >> nth=2');
await page.waitForSelector('.inv-item');
const btn = page.locator('.inv-item .btn-primary').first();
if (await btn.count()) {
  await btn.click();
  await page.waitForSelector('.notice', { timeout: 20000 });
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${OUT}/8-withdraw.png` });
}

// Конструктор кейсов в админке
await page.click('.tab >> nth=3');
await page.waitForSelector('.admin-stats');
await page.getByText('+ Создать кейс').click();
await page.waitForSelector('.builder-gifts');
await page.fill('input[placeholder="Название кейса"]', 'VIP кейс');
await page.fill('input[placeholder="Цена открытия, ⭐"]', '150');
const giftButtons = page.locator('.builder-gift-main');
for (let i = 0; i < 4; i++) await giftButtons.nth(i).click();
await page.waitForTimeout(500);
await page.screenshot({ path: `${OUT}/9-builder.png` });

console.log('UI OK');
await browser.close();
