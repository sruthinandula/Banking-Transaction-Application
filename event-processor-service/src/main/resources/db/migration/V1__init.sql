-- Audit trail: one row per event; unique event_id makes consumption idempotent.
CREATE TABLE audit_logs (
    id             BIGSERIAL PRIMARY KEY,
    event_id       VARCHAR(64)   NOT NULL UNIQUE,
    event_type     VARCHAR(20)   NOT NULL,
    transaction_id BIGINT,
    from_account   VARCHAR(20),
    to_account     VARCHAR(20),
    amount         NUMERIC(19,2) NOT NULL,
    payload        TEXT          NOT NULL,
    received_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id             BIGSERIAL PRIMARY KEY,
    event_id       VARCHAR(64)  NOT NULL,
    account_number VARCHAR(20)  NOT NULL,
    message        VARCHAR(300) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_event_account UNIQUE (event_id, account_number)
);

-- Events that exhausted their retries and landed on the dead-letter topic.
CREATE TABLE failed_events (
    id                    BIGSERIAL PRIMARY KEY,
    source_topic          VARCHAR(200),
    failed_consumer_group VARCHAR(200),
    message_key           VARCHAR(100),
    payload               TEXT,
    error_message         TEXT,
    failed_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);
