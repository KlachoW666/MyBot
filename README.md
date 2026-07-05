# Gift Cases — Telegram Mini App

Мини-приложение «открытие кейсов с подарками» внутри Telegram WebApp.

- **Frontend**: React + Vite (Telegram Mini App)
- **Backend**: Node.js + Fastify
- **Бот**: grammY (webhook, встроен в backend)
- **БД**: PostgreSQL, **кэш/локи**: Redis
- **Оплата**: Telegram Stars (XTR), **призы**: Telegram Gifts (`sendGift`)

## Структура проекта

```
.
├── backend/
│   ├── db/
│   │   └── schema.sql            # Схема PostgreSQL
│   ├── scripts/
│   │   ├── migrate.js            # Применение schema.sql
│   │   ├── seed.js               # Кейсы из живого каталога подарков
│   │   └── set-webhook.js        # setWebhook с secret_token
│   ├── src/
│   │   ├── index.js              # Точка входа
│   │   ├── server.js             # Сборка Fastify-приложения
│   │   ├── config.js             # Конфиг из env
│   │   ├── bot.js                # grammY: pre_checkout_query, successful_payment
│   │   ├── db/pool.js            # pg Pool + helper транзакций
│   │   ├── redis.js              # ioredis + распределённый лок
│   │   ├── lib/
│   │   │   ├── validate-init-data.js  # HMAC-валидация initData
│   │   │   ├── telegram-api.js        # Клиент Bot API (getAvailableGifts, sendGift, createInvoiceLink)
│   │   │   └── errors.js              # AppError → HTTP-ответы
│   │   ├── plugins/
│   │   │   └── auth.js           # JWT-декоратор (fastify.authenticate)
│   │   ├── services/
│   │   │   ├── catalog.js        # Каталог подарков: Redis-кэш + снапшот в БД
│   │   │   ├── case-opening.js   # Серверный RNG по весам, транзакция открытия
│   │   │   ├── payments.js       # Инвойсы Stars, зачисление баланса
│   │   │   └── withdrawals.js    # Вывод подарка: лок, sendGift, ретраи, рефанд
│   │   └── routes/
│   │       ├── auth.js           # POST /auth
│   │       ├── catalog.js        # GET  /catalog
│   │       ├── cases.js          # GET  /cases, GET /cases/:id, POST /cases/:id/open
│   │       ├── pay.js            # POST /pay/invoice
│   │       ├── withdraw.js       # POST /withdraw
│   │       ├── me.js             # GET  /me, GET /me/inventory
│   │       └── webhook.js        # POST /bot/webhook (secret_token)
│   └── package.json
├── frontend/
│   ├── src/
│   │   ├── main.jsx
│   │   ├── App.jsx
│   │   ├── telegram.js           # Обёртка window.Telegram.WebApp
│   │   ├── api.js                # HTTP-клиент с JWT
│   │   ├── components/
│   │   │   ├── CaseCard.jsx
│   │   │   ├── CaseOpenModal.jsx # Анимация по ответу сервера
│   │   │   ├── Inventory.jsx
│   │   │   └── TopUp.jsx
│   │   └── styles.css
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
├── docker-compose.yml
└── .env.example
```

## Запуск

```bash
cp .env.example .env            # заполнить BOT_TOKEN и секреты
docker compose up -d postgres redis

cd backend
npm install
npm run migrate                 # применить db/schema.sql
npm run seed                    # создать кейсы из живого каталога подарков
npm run dev                     # API на :8080

cd ../frontend
npm install
npm run dev                     # WebApp на :5173 (проксирует /api на :8080)
```

Webhook бота (после деплоя за HTTPS):

```bash
cd backend && npm run set-webhook   # setWebhook(PUBLIC_URL/bot/webhook, secret_token=WEBHOOK_SECRET)
```

## Поток данных

1. **Auth**: фронт шлёт `window.Telegram.WebApp.initData` → backend проверяет
   HMAC (`secret = HMAC_SHA256("WebAppData", BOT_TOKEN)`) и `auth_date` (≤ 1 ч) →
   выдаёт JWT c `telegram_id`. Клиенту не доверяем: все id — только из подписи.
2. **Каталог**: backend зовёт `getAvailableGifts` → Redis (TTL 10 мин) + снапшот
   в `gifts_catalog`. `gift_id` не статичны, лимитированные распродаются.
3. **Открытие**: только сервер. Транзакция: `SELECT ... FOR UPDATE` баланса →
   списание → `crypto`-RNG по весам среди доступных подарков → запись в
   `inventory (status=won)`. Клиент получает результат и рисует анимацию.
4. **Оплата**: `createInvoiceLink(currency=XTR)` → `openInvoice` на фронте →
   `pre_checkout_query` → `successful_payment` → идемпотентное зачисление
   по `telegram_payment_charge_id`.
5. **Вывод**: Redis-лок на предмет → `won → withdraw_pending` в транзакции →
   `sendGift(gift_id, telegram_id из сессии)` → `withdrawn`.
   `GIFT_INVALID` → рефанд во внутренний баланс; `429` → авто-ретрай.
