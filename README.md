# Gift Cases — Telegram Mini App

Мини-приложение «открытие кейсов с подарками» внутри Telegram WebApp.

- **Frontend**: React + Vite, тёмный премиум-дизайн, до **5 кейсов на одном экране**
- **Backend**: Node.js + Fastify (вебхук бота встроен, без polling)
- **БД**: PostgreSQL, **кэш/локи/рейт-лимиты**: Redis
- **Оплата**: Telegram Stars (XTR), **призы**: Telegram Gifts (`sendGift`)
- **Админка**: встроена в апп, доступ по `ADMIN_IDS` (по умолчанию `8486449177`)

## Структура проекта

```
.
├── backend/
│   ├── db/schema.sql                  # Схема PostgreSQL
│   ├── dev/telegram-emulator.js       # Локальный эмулятор Bot API (+ /emu/pay)
│   ├── scripts/
│   │   ├── migrate.js                 # Применение schema.sql
│   │   ├── seed.js                    # 5 кейсов из живого каталога подарков
│   │   └── set-webhook.js             # setWebhook с secret_token
│   ├── src/
│   │   ├── index.js                   # Точка входа
│   │   ├── server.js                  # Сборка Fastify-приложения
│   │   ├── config.js                  # Конфиг из env (ADMIN_IDS, TELEGRAM_API_BASE, ...)
│   │   ├── bot.js                     # Обработчик апдейтов: /start, pre_checkout, платежи
│   │   ├── db/pool.js                 # pg Pool + helper транзакций
│   │   ├── redis.js                   # ioredis + распределённый лок
│   │   ├── lib/
│   │   │   ├── validate-init-data.js  # HMAC-валидация initData (+ auth_date ≤ 1ч)
│   │   │   ├── telegram-api.js        # Клиент Bot API: getAvailableGifts, sendGift,
│   │   │   │                          #   createInvoiceLink, answerPreCheckoutQuery...
│   │   │   ├── rng.js                 # CSPRNG по весам (crypto.randomInt)
│   │   │   └── errors.js              # AppError → HTTP-ответы
│   │   ├── plugins/auth.js            # JWT (authenticate) + requireAdmin
│   │   ├── services/
│   │   │   ├── catalog.js             # Redis-кэш каталога + снапшот в БД
│   │   │   ├── case-opening.js        # Транзакция открытия, идемпотентность
│   │   │   ├── payments.js            # Инвойсы XTR, идемпотентное зачисление
│   │   │   └── withdrawals.js         # Лок + статусная машина + ретраи/рефанд
│   │   └── routes/
│   │       ├── auth.js                # POST /auth
│   │       ├── catalog.js             # GET  /catalog
│   │       ├── cases.js               # GET  /cases, POST /cases/:id/open
│   │       ├── pay.js                 # POST /pay/invoice
│   │       ├── withdraw.js            # POST /withdraw
│   │       ├── me.js                  # GET  /me, /me/inventory
│   │       ├── admin.js               # /admin/*: статы, кейсы, каталог, начисления
│   │       └── webhook.js             # POST /bot/webhook (secret_token, timing-safe)
│   └── test/                          # unit (initData, RNG) + e2e (все флоу)
├── frontend/
│   ├── dev/ui-test.mjs                # Playwright-прогон всех экранов (скриншоты)
│   ├── src/
│   │   ├── App.jsx                    # Авторизация, табы, баланс
│   │   ├── telegram.js                # Обёртка WebApp (initData, openInvoice, haptics)
│   │   ├── api.js                     # HTTP-клиент с JWT + admin API
│   │   ├── screens/
│   │   │   ├── CasesScreen.jsx        # 5 кейсов: hero + сетка 2×2, один вьюпорт
│   │   │   ├── ProfileScreen.jsx      # Аватар, статы, инвентарь, вывод
│   │   │   └── AdminScreen.jsx        # Статы, кейсы on/off, каталог, начисление ⭐
│   │   └── components/                # CaseCard, OpenOverlay (рулетка), TopUpSheet,
│   │                                  #   InventoryList, TabBar, BalancePill
│   └── ...
├── docker-compose.yml                 # postgres + redis
└── .env.example
```

## Быстрый старт (production)

```bash
cp .env.example backend/.env      # BOT_TOKEN, JWT_SECRET, WEBHOOK_SECRET, PUBLIC_URL
docker compose up -d postgres redis

cd backend
npm install
npm run migrate
npm run seed                      # 5 кейсов из живого getAvailableGifts
npm start                         # API + вебхук на :8080

cd ../frontend
npm install && npm run build      # раздать dist/ с того же домена (path /, api → /api)

cd ../backend && npm run set-webhook   # HTTPS-вебхук с secret_token
```

В @BotFather: **Bot Settings → Menu Button** (или Main Mini App) → URL фронтенда.

## Локальная разработка без Telegram

Реальный Bot API заменяется эмулятором:

```bash
cd backend
TELEGRAM_API_BASE=http://localhost:8081 npm run dev   # backend
node dev/telegram-emulator.js                          # фейковый Bot API + /emu/pay
cd ../frontend && npm run dev                          # WebApp на :5173

# эмуляция оплаты Stars (полный цикл через вебхук):
curl -X POST localhost:8081/emu/pay -H 'content-type: application/json' \
  -d '{"user_id": 8486449177, "amount": 1000}'

# UI-прогон с реально подписанным initData + скриншоты:
cd frontend && BOT_TOKEN=<токен> OUT=./shots node dev/ui-test.mjs
```

## Ключевые инварианты

1. **Auth**: только `initData` → HMAC (`secret = HMAC_SHA256("WebAppData", BOT_TOKEN)`),
   `auth_date` ≤ 1 ч → JWT. Единственный идентификатор — проверенный `telegram_id`.
2. **Открытие** — только сервер: `FOR UPDATE`-баланс → CSPRNG по весам →
   `inventory(won)`; идемпотентность по ключу; клиент лишь рисует рулетку
   по готовому результату.
3. **Оплата**: `createInvoiceLink(XTR)` → `openInvoice` → вебхук
   (`pre_checkout_query` → `successful_payment`) → зачисление, идемпотентное
   по `telegram_payment_charge_id`. Юзер создаётся при первом платеже, если его нет.
4. **Вывод**: Redis-лок → `won → withdraw_pending` (транзакция) →
   `sendGift(gift_id, telegram_id)` → `withdrawn`. `GIFT_INVALID` → авто-рефанд
   в ⭐; `429` → ретрай с `retry_after`; `UNIQUE(inventory_id)` в withdrawals.
5. **Кейсы**: максимум 5 активных (влезают в один экран) — лимит проверяет сервер.
6. **Админка** (`ADMIN_IDS`): статистика, вкл/выкл кейсов, upsert кейса с весами,
   обновление каталога, ручное начисление Stars (журналируется в transactions).

## Тесты

```bash
cd backend
npm test               # unit: initData HMAC, weighted RNG
node test/e2e.js       # против реальных Postgres+Redis (Bot API застаблен):
                       # auth/подделки, идемпотентное открытие, 429-ретрай,
                       # GIFT_INVALID-рефанд, дабл-вывод, вебхук-секрет,
                       # зачисление платежа (в т.ч. новому юзеру), админ-гард,
                       # лимит 5 кейсов
```
