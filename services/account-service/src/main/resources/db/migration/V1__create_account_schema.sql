CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    masked_account_number VARCHAR(32) NOT NULL,
    nickname VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    opened_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_customer_status ON accounts(customer_id, status);

CREATE TABLE account_balances (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE,
    current_balance NUMERIC(19, 2) NOT NULL,
    available_balance NUMERIC(19, 2) NOT NULL,
    pending_debit_amount NUMERIC(19, 2) NOT NULL,
    pending_credit_amount NUMERIC(19, 2) NOT NULL,
    as_of TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_account_balances_account_id ON account_balances(account_id);

INSERT INTO accounts (
    id,
    customer_id,
    account_type,
    masked_account_number,
    nickname,
    status,
    currency,
    opened_date,
    created_at,
    updated_at,
    version
) VALUES
(
    '44444444-4444-4444-4444-444444444444',
    '11111111-1111-1111-1111-111111111111',
    'CHECKING',
    '**** 4821',
    'Daily Checking',
    'ACTIVE',
    'USD',
    DATE '2024-01-16',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
),
(
    '55555555-5555-5555-5555-555555555555',
    '11111111-1111-1111-1111-111111111111',
    'SAVINGS',
    '**** 7710',
    'Emergency Savings',
    'ACTIVE',
    'USD',
    DATE '2024-02-20',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO account_balances (
    id,
    account_id,
    current_balance,
    available_balance,
    pending_debit_amount,
    pending_credit_amount,
    as_of,
    version
) VALUES
(
    '66666666-6666-6666-6666-666666666666',
    '44444444-4444-4444-4444-444444444444',
    4286.42,
    4036.42,
    250.00,
    0.00,
    CURRENT_TIMESTAMP,
    0
),
(
    '77777777-7777-7777-7777-777777777777',
    '55555555-5555-5555-5555-555555555555',
    12950.00,
    12950.00,
    0.00,
    0.00,
    CURRENT_TIMESTAMP,
    0
);
