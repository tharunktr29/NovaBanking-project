CREATE TABLE bank_cards (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    account_id UUID NOT NULL,
    card_reference VARCHAR(40) NOT NULL UNIQUE,
    card_type VARCHAR(20) NOT NULL,
    card_network VARCHAR(20) NOT NULL,
    status VARCHAR(40) NOT NULL,
    cardholder_name VARCHAR(120) NOT NULL,
    last_four VARCHAR(4) NOT NULL,
    masked_card_number VARCHAR(32) NOT NULL,
    expiration_month INTEGER NOT NULL,
    expiration_year INTEGER NOT NULL,
    activated_at TIMESTAMP WITH TIME ZONE,
    locked_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT chk_bank_cards_card_type CHECK (card_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_bank_cards_network CHECK (card_network IN ('VISA', 'MASTERCARD')),
    CONSTRAINT chk_bank_cards_status CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'LOCKED', 'REPLACEMENT_REQUESTED', 'EXPIRED', 'CLOSED')),
    CONSTRAINT chk_bank_cards_last_four CHECK (last_four ~ '^[0-9]{4}$'),
    CONSTRAINT chk_bank_cards_expiration_month CHECK (expiration_month BETWEEN 1 AND 12),
    CONSTRAINT chk_bank_cards_masked_number CHECK (masked_card_number NOT LIKE '%' || last_four || '%' OR right(masked_card_number, 4) = last_four)
);

CREATE INDEX idx_bank_cards_customer_id ON bank_cards(customer_id);
CREATE INDEX idx_bank_cards_account_id ON bank_cards(account_id);
CREATE INDEX idx_bank_cards_customer_account ON bank_cards(customer_id, account_id);
CREATE INDEX idx_bank_cards_status ON bank_cards(status);
CREATE INDEX idx_bank_cards_card_type ON bank_cards(card_type);

CREATE TABLE card_controls (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL UNIQUE REFERENCES bank_cards(id) ON DELETE CASCADE,
    online_purchases_enabled BOOLEAN NOT NULL,
    contactless_enabled BOOLEAN NOT NULL,
    international_purchases_enabled BOOLEAN NOT NULL,
    atm_withdrawals_enabled BOOLEAN NOT NULL,
    daily_purchase_limit NUMERIC(19, 2) NOT NULL,
    daily_atm_limit NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT chk_card_controls_daily_purchase_limit CHECK (daily_purchase_limit >= 0),
    CONSTRAINT chk_card_controls_daily_atm_limit CHECK (daily_atm_limit >= 0),
    CONSTRAINT chk_card_controls_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_card_controls_atm_disabled_limit CHECK (atm_withdrawals_enabled OR daily_atm_limit = 0)
);

CREATE INDEX idx_card_controls_card_id ON card_controls(card_id);

CREATE TABLE credit_details (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL UNIQUE REFERENCES bank_cards(id) ON DELETE CASCADE,
    credit_limit NUMERIC(19, 2) NOT NULL,
    current_balance NUMERIC(19, 2) NOT NULL,
    available_credit NUMERIC(19, 2) NOT NULL,
    minimum_payment_due NUMERIC(19, 2) NOT NULL,
    payment_due_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT chk_credit_details_amounts CHECK (
        credit_limit >= 0
        AND current_balance >= 0
        AND available_credit >= 0
        AND minimum_payment_due >= 0
        AND available_credit = credit_limit - current_balance
    ),
    CONSTRAINT chk_credit_details_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_credit_details_card_id ON credit_details(card_id);

CREATE TABLE card_lifecycle_history (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL REFERENCES bank_cards(id) ON DELETE CASCADE,
    action VARCHAR(40) NOT NULL,
    previous_status VARCHAR(40),
    new_status VARCHAR(40) NOT NULL,
    reason_code VARCHAR(40),
    correlation_id UUID NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_card_lifecycle_history_action CHECK (action IN ('CREATED', 'ACTIVATED', 'LOCKED', 'UNLOCKED', 'REPLACEMENT_REQUESTED', 'CONTROLS_UPDATED', 'EXPIRED', 'CLOSED')),
    CONSTRAINT chk_card_lifecycle_history_previous_status CHECK (previous_status IS NULL OR previous_status IN ('PENDING_ACTIVATION', 'ACTIVE', 'LOCKED', 'REPLACEMENT_REQUESTED', 'EXPIRED', 'CLOSED')),
    CONSTRAINT chk_card_lifecycle_history_new_status CHECK (new_status IN ('PENDING_ACTIVATION', 'ACTIVE', 'LOCKED', 'REPLACEMENT_REQUESTED', 'EXPIRED', 'CLOSED'))
);

CREATE INDEX idx_card_lifecycle_history_card_id ON card_lifecycle_history(card_id);
CREATE INDEX idx_card_lifecycle_history_occurred_at ON card_lifecycle_history(occurred_at);

CREATE TABLE replacement_requests (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL REFERENCES bank_cards(id) ON DELETE CASCADE,
    reason VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    request_reference VARCHAR(40) NOT NULL UNIQUE,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    CONSTRAINT chk_replacement_requests_reason CHECK (reason IN ('LOST', 'STOLEN', 'DAMAGED', 'EXPIRED', 'NAME_CHANGE', 'OTHER')),
    CONSTRAINT chk_replacement_requests_status CHECK (status IN ('REQUESTED', 'PROCESSING', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_replacement_requests_card_id ON replacement_requests(card_id);
CREATE INDEX idx_replacement_requests_status ON replacement_requests(status);
CREATE UNIQUE INDEX uk_replacement_requests_one_active
    ON replacement_requests(card_id)
    WHERE status IN ('REQUESTED', 'PROCESSING');

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL,
    customer_id UUID NOT NULL,
    operation VARCHAR(80) NOT NULL,
    resource_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_idempotency_key CHECK (idempotency_key ~ '^[A-Za-z0-9._:-]{8,120}$')
);

CREATE UNIQUE INDEX uk_idempotency_customer_operation_resource_key
    ON idempotency_records(customer_id, operation, resource_id, idempotency_key);
CREATE INDEX idx_idempotency_customer_key ON idempotency_records(customer_id, idempotency_key);
CREATE INDEX idx_idempotency_expires_at ON idempotency_records(expires_at);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL,
    correlation_id VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_outbox_events_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED', 'ABANDONED')),
    CONSTRAINT chk_outbox_events_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_events_status_next_attempt ON outbox_events(status, next_attempt_at);
CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events(aggregate_id);

INSERT INTO bank_cards (
    id,
    customer_id,
    account_id,
    card_reference,
    card_type,
    card_network,
    status,
    cardholder_name,
    last_four,
    masked_card_number,
    expiration_month,
    expiration_year,
    activated_at,
    locked_at,
    expires_at,
    created_at,
    updated_at,
    version
) VALUES
(
    '88888888-8888-8888-8888-888888888888',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444444',
    'CARD-DEBIT-CHECKING-4242',
    'DEBIT',
    'VISA',
    'ACTIVE',
    'Demo User',
    '4242',
    '**** **** **** 4242',
    8,
    2029,
    CURRENT_TIMESTAMP,
    NULL,
    TIMESTAMP WITH TIME ZONE '2029-08-31 23:59:59+00',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    '99999999-9999-9999-9999-999999999999',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555555',
    'CARD-DEBIT-SAVINGS-1881',
    'DEBIT',
    'MASTERCARD',
    'PENDING_ACTIVATION',
    'Demo User',
    '1881',
    '**** **** **** 1881',
    11,
    2029,
    NULL,
    NULL,
    TIMESTAMP WITH TIME ZONE '2029-11-30 23:59:59+00',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444444',
    'CARD-CREDIT-9090',
    'CREDIT',
    'VISA',
    'ACTIVE',
    'Demo User',
    '9090',
    '**** **** **** 9090',
    5,
    2030,
    CURRENT_TIMESTAMP,
    NULL,
    TIMESTAMP WITH TIME ZONE '2030-05-31 23:59:59+00',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO card_controls (
    id,
    card_id,
    online_purchases_enabled,
    contactless_enabled,
    international_purchases_enabled,
    atm_withdrawals_enabled,
    daily_purchase_limit,
    daily_atm_limit,
    currency,
    created_at,
    updated_at,
    version
) VALUES
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '88888888-8888-8888-8888-888888888888',
    true,
    true,
    false,
    true,
    1500.00,
    400.00,
    'USD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    '99999999-9999-9999-9999-999999999999',
    true,
    false,
    false,
    false,
    500.00,
    0.00,
    'USD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    true,
    true,
    false,
    false,
    2500.00,
    0.00,
    'USD',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO credit_details (
    id,
    card_id,
    credit_limit,
    current_balance,
    available_credit,
    minimum_payment_due,
    payment_due_date,
    currency,
    updated_at,
    version
) VALUES
(
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    7500.00,
    1250.35,
    6249.65,
    35.00,
    DATE '2026-09-15',
    'USD',
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO card_lifecycle_history (
    id,
    card_id,
    action,
    previous_status,
    new_status,
    reason_code,
    correlation_id,
    occurred_at
) VALUES
(
    '12121212-1212-1212-1212-121212121212',
    '88888888-8888-8888-8888-888888888888',
    'CREATED',
    NULL,
    'PENDING_ACTIVATION',
    NULL,
    '00000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP - INTERVAL '60 days'
),
(
    '13131313-1313-1313-1313-131313131313',
    '88888888-8888-8888-8888-888888888888',
    'ACTIVATED',
    'PENDING_ACTIVATION',
    'ACTIVE',
    NULL,
    '00000000-0000-0000-0000-000000000002',
    CURRENT_TIMESTAMP - INTERVAL '59 days'
),
(
    '14141414-1414-1414-1414-141414141414',
    '99999999-9999-9999-9999-999999999999',
    'CREATED',
    NULL,
    'PENDING_ACTIVATION',
    NULL,
    '00000000-0000-0000-0000-000000000003',
    CURRENT_TIMESTAMP - INTERVAL '10 days'
),
(
    '15151515-1515-1515-1515-151515151515',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'CREATED',
    NULL,
    'PENDING_ACTIVATION',
    NULL,
    '00000000-0000-0000-0000-000000000004',
    CURRENT_TIMESTAMP - INTERVAL '45 days'
),
(
    '16161616-1616-1616-1616-161616161616',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'ACTIVATED',
    'PENDING_ACTIVATION',
    'ACTIVE',
    NULL,
    '00000000-0000-0000-0000-000000000005',
    CURRENT_TIMESTAMP - INTERVAL '44 days'
);
