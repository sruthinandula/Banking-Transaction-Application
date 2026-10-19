CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id             BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20)   NOT NULL UNIQUE,
    owner_username VARCHAR(50)   NOT NULL REFERENCES users (username),
    holder_name    VARCHAR(100)  NOT NULL,
    balance        NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_owner ON accounts (owner_username);

-- idempotency_key is UNIQUE
CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    idempotency_key     VARCHAR(100)  NOT NULL UNIQUE,
    request_fingerprint VARCHAR(300)  NOT NULL,
    type                VARCHAR(20)   NOT NULL,
    from_account        VARCHAR(20),
    to_account          VARCHAR(20),
    amount              NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    status              VARCHAR(20)   NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_from ON transactions (from_account, created_at DESC);
CREATE INDEX idx_transactions_to   ON transactions (to_account, created_at DESC);


CREATE TABLE outbox_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     VARCHAR(64)  NOT NULL UNIQUE,
    event_key    VARCHAR(50)  NOT NULL,
    payload      TEXT         NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (id) WHERE published = FALSE;
