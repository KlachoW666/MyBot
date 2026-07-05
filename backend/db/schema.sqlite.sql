-- Gift Cases — схема SQLite (зеркало schema.sql; ENUM → TEXT CHECK).

CREATE TABLE IF NOT EXISTS users (
    telegram_id  INTEGER PRIMARY KEY,
    username     TEXT,
    first_name   TEXT,
    balance      INTEGER NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS gifts_catalog (
    gift_id             TEXT PRIMARY KEY,
    sticker_file_id     TEXT,
    emoji               TEXT,
    star_count          INTEGER NOT NULL CHECK (star_count > 0),
    upgrade_star_count  INTEGER,
    total_count         INTEGER,
    remaining_count     INTEGER,
    is_available        INTEGER NOT NULL DEFAULT 1,
    snapshot            TEXT NOT NULL,          -- JSON строкой
    updated_at          TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cases (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    slug         TEXT NOT NULL UNIQUE,
    title        TEXT NOT NULL,
    price_stars  INTEGER NOT NULL CHECK (price_stars > 0),
    is_active    INTEGER NOT NULL DEFAULT 1,
    created_at   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS case_items (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    case_id  INTEGER NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    gift_id  TEXT    NOT NULL REFERENCES gifts_catalog(gift_id),
    weight   INTEGER NOT NULL CHECK (weight > 0),
    UNIQUE (case_id, gift_id)
);
CREATE INDEX IF NOT EXISTS case_items_case_idx ON case_items (case_id);

CREATE TABLE IF NOT EXISTS inventory (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL REFERENCES users(telegram_id),
    case_id     INTEGER REFERENCES cases(id),
    gift_id     TEXT    NOT NULL REFERENCES gifts_catalog(gift_id),
    star_value  INTEGER NOT NULL,
    status      TEXT NOT NULL DEFAULT 'won' CHECK (status IN
                    ('won', 'withdraw_pending', 'withdrawn', 'refunded', 'lost')),
    created_at  TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS inventory_user_idx ON inventory (user_id, status);

CREATE TABLE IF NOT EXISTS transactions (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER NOT NULL REFERENCES users(telegram_id),
    type             TEXT    NOT NULL CHECK (type IN
                         ('deposit', 'case_open', 'refund', 'adjustment')),
    amount           INTEGER NOT NULL,
    ref              TEXT,
    idempotency_key  TEXT UNIQUE,
    created_at       TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS transactions_user_idx ON transactions (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS withdrawals (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    inventory_id  INTEGER NOT NULL UNIQUE REFERENCES inventory(id),
    user_id       INTEGER NOT NULL REFERENCES users(telegram_id),
    gift_id       TEXT    NOT NULL,
    status        TEXT NOT NULL DEFAULT 'pending' CHECK (status IN
                      ('pending', 'sent', 'failed', 'refunded')),
    attempts      INTEGER NOT NULL DEFAULT 0,
    tg_result     TEXT,
    created_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS upgrades (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER NOT NULL REFERENCES users(telegram_id),
    inventory_id  INTEGER NOT NULL REFERENCES inventory(id),
    from_gift_id  TEXT    NOT NULL,
    from_value    INTEGER NOT NULL,
    to_gift_id    TEXT    NOT NULL,
    to_value      INTEGER NOT NULL,
    chance_bp     INTEGER NOT NULL CHECK (chance_bp BETWEEN 1 AND 10000),
    roll_bp       INTEGER NOT NULL CHECK (roll_bp BETWEEN 0 AND 9999),
    won           INTEGER NOT NULL,
    created_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS upgrades_user_idx ON upgrades (user_id, created_at DESC);
