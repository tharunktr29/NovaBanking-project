CREATE TABLE ledger_transactions (
    id UUID PRIMARY KEY,
    business_operation_id UUID NOT NULL UNIQUE,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference VARCHAR(120) NOT NULL,
    correlation_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    posted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_ledger_transaction_type CHECK (type IN ('INTERNAL_TRANSFER', 'CREDIT_CARD_PAYMENT', 'EXTERNAL_PAYMENT', 'REVERSAL')),
    CONSTRAINT chk_ledger_transaction_status CHECK (status IN ('PENDING', 'POSTED', 'REVERSED', 'FAILED'))
);

CREATE INDEX idx_ledger_transactions_business_operation ON ledger_transactions(business_operation_id);
CREATE INDEX idx_ledger_transactions_reference ON ledger_transactions(reference);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    ledger_transaction_id UUID NOT NULL REFERENCES ledger_transactions(id),
    account_id UUID NOT NULL,
    direction VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_ledger_entry_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_entry_amount CHECK (amount > 0),
    CONSTRAINT chk_ledger_entry_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_ledger_entries_transaction ON ledger_entries(ledger_transaction_id);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id);
