-- Gift Cases — схема PostgreSQL.
-- Все суммы — в Telegram Stars (целые числа).

BEGIN;

-- ---------------------------------------------------------------- users
-- Единственный идентификатор пользователя — telegram_id из проверенного
-- initData. Никаких паролей/почты.
CREATE TABLE IF NOT EXISTS users (
    telegram_id  BIGINT PRIMARY KEY,
    username     TEXT,
    first_name   TEXT,
    balance      BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------- gifts_catalog
-- Снапшот getAvailableGifts. gift_id НЕ статичны: лимитированные подарки
-- распродаются и исчезают из выдачи — тогда is_available=false.
CREATE TABLE IF NOT EXISTS gifts_catalog (
    gift_id             TEXT PRIMARY KEY,
    sticker_file_id     TEXT,
    emoji               TEXT,
    star_count          INTEGER NOT NULL CHECK (star_count > 0),
    upgrade_star_count  INTEGER,
    total_count         INTEGER,           -- NULL = безлимитный
    remaining_count     INTEGER,
    is_available        BOOLEAN NOT NULL DEFAULT TRUE,
    snapshot            JSONB NOT NULL,    -- сырой объект Gift из Bot API
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------- cases
CREATE TABLE IF NOT EXISTS cases (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug         TEXT NOT NULL UNIQUE,
    title        TEXT NOT NULL,
    price_stars  INTEGER NOT NULL CHECK (price_stars > 0),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------ case_items
-- Дроп-таблица кейса: вероятность = weight / SUM(weight) по кейсу.
CREATE TABLE IF NOT EXISTS case_items (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    case_id  BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    gift_id  TEXT   NOT NULL REFERENCES gifts_catalog(gift_id),
    weight   INTEGER NOT NULL CHECK (weight > 0),
    UNIQUE (case_id, gift_id)
);
CREATE INDEX IF NOT EXISTS case_items_case_idx ON case_items (case_id);

-- ------------------------------------------------------------- inventory
-- Выигрыши. Статусная машина: won → withdraw_pending → withdrawn,
-- won/withdraw_pending → refunded (GIFT_INVALID и т.п.),
-- won → lost (проигранный апгрейд).
DO $$ BEGIN
    CREATE TYPE inventory_status AS ENUM
        ('won', 'withdraw_pending', 'withdrawn', 'refunded', 'lost');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
ALTER TYPE inventory_status ADD VALUE IF NOT EXISTS 'lost';

CREATE TABLE IF NOT EXISTS inventory (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(telegram_id),
    case_id     BIGINT REFERENCES cases(id),
    gift_id     TEXT   NOT NULL REFERENCES gifts_catalog(gift_id),
    -- стоимость подарка в Stars на момент выигрыша (для рефанда)
    star_value  INTEGER NOT NULL,
    status      inventory_status NOT NULL DEFAULT 'won',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS inventory_user_idx ON inventory (user_id, status);

-- ---------------------------------------------------------- transactions
-- Журнал всех движений баланса. amount: + зачисление, - списание.
-- idempotency_key уникален → повторная обработка (ретрай вебхука,
-- дабл-клик по «открыть») не задвоит операцию.
CREATE TABLE IF NOT EXISTS transactions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(telegram_id),
    type             TEXT   NOT NULL CHECK (type IN
                         ('deposit', 'case_open', 'refund', 'adjustment')),
    amount           BIGINT NOT NULL,
    ref              TEXT,                       -- inventory.id / charge_id и т.п.
    idempotency_key  TEXT UNIQUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS transactions_user_idx ON transactions (user_id, created_at DESC);

-- ----------------------------------------------------------- withdrawals
-- Журнал выводов. UNIQUE(inventory_id) — один предмет физически нельзя
-- вывести дважды, даже если лок в Redis по какой-то причине не сработал.
CREATE TABLE IF NOT EXISTS withdrawals (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventory_id  BIGINT NOT NULL UNIQUE REFERENCES inventory(id),
    user_id       BIGINT NOT NULL REFERENCES users(telegram_id),
    gift_id       TEXT   NOT NULL,
    status        TEXT   NOT NULL DEFAULT 'pending' CHECK (status IN
                      ('pending', 'sent', 'failed', 'refunded')),
    attempts      INTEGER NOT NULL DEFAULT 0,
    tg_result     JSONB,                         -- ответ/ошибка Bot API
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------- upgrades
-- Журнал апгрейдов: предмет ставится против более дорогого подарка,
-- шанс = from_value / to_value (базисные пункты, клампится 1%..75%).
-- Ролл и исход — только сервер.
CREATE TABLE IF NOT EXISTS upgrades (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(telegram_id),
    inventory_id  BIGINT NOT NULL REFERENCES inventory(id),
    from_gift_id  TEXT   NOT NULL,
    from_value    INTEGER NOT NULL,
    to_gift_id    TEXT   NOT NULL,
    to_value      INTEGER NOT NULL,
    chance_bp     INTEGER NOT NULL CHECK (chance_bp BETWEEN 1 AND 10000),
    roll_bp       INTEGER NOT NULL CHECK (roll_bp BETWEEN 0 AND 9999),
    won           BOOLEAN NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS upgrades_user_idx ON upgrades (user_id, created_at DESC);

COMMIT;
