CREATE TABLE payment_orders (
    id UUID PRIMARY KEY,
    payment_reference VARCHAR(60) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    payment_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    source_account_id UUID NOT NULL,
    destination_account_id UUID,
    destination_card_id UUID,
    external_payee_id UUID,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    memo VARCHAR(160),
    execution_type VARCHAR(20) NOT NULL,
    scheduled_for TIMESTAMPTZ,
    processing_started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_code VARCHAR(40),
    failure_message VARCHAR(200),
    correlation_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_payment_orders_customer_created ON payment_orders(customer_id, created_at DESC);
CREATE INDEX idx_payment_orders_customer_status ON payment_orders(customer_id, status);
CREATE INDEX idx_payment_orders_source_account ON payment_orders(source_account_id);
CREATE INDEX idx_payment_orders_scheduled ON payment_orders(status, scheduled_for);

CREATE TABLE external_payees (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    payee_reference VARCHAR(60) NOT NULL UNIQUE,
    nickname VARCHAR(80) NOT NULL,
    bank_name VARCHAR(120) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    masked_account_number VARCHAR(32) NOT NULL,
    external_account_token VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_external_payees_customer ON external_payees(customer_id);
CREATE INDEX idx_external_payees_status ON external_payees(status);

CREATE TABLE payment_status_history (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    reason_code VARCHAR(60),
    event_id UUID,
    correlation_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_status_history_payment ON payment_status_history(payment_id);
CREATE INDEX idx_payment_status_history_occurred ON payment_status_history(occurred_at);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    operation VARCHAR(80) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    resource_id UUID,
    response_status INTEGER NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_payment_idempotency_customer_operation_key UNIQUE (customer_id, operation, idempotency_key)
);

CREATE INDEX idx_payment_idempotency_expires ON idempotency_records(expires_at);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    correlation_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_outbox_status_next ON outbox_events(status, next_attempt_at);
CREATE INDEX idx_payment_outbox_aggregate ON outbox_events(aggregate_id);

INSERT INTO external_payees (
    id, customer_id, payee_reference, nickname, bank_name, account_type,
    masked_account_number, external_account_token, status, created_at, updated_at, version
) VALUES
(
    '55555555-5555-5555-5555-555555555501',
    '11111111-1111-1111-1111-111111111111',
    'PAYEE-DEMO-001',
    'Fictional Rent Account',
    'Demo Community Bank',
    'CHECKING',
    '**** 2468',
    'demo-token-rent-2468',
    'VERIFIED',
    now(),
    now(),
    0
),
(
    '55555555-5555-5555-5555-555555555502',
    '11111111-1111-1111-1111-111111111111',
    'PAYEE-DEMO-002',
    'Fictional Savings Goal',
    'Imaginary Federal Credit Union',
    'SAVINGS',
    '**** 1357',
    'demo-token-savings-1357',
    'VERIFIED',
    now(),
    now(),
    0
);
