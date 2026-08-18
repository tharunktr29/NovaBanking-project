CREATE TABLE statements (id UUID PRIMARY KEY, customer_id UUID NOT NULL, account_id UUID NOT NULL, period_start DATE NOT NULL, period_end DATE NOT NULL, opening_balance NUMERIC(19,2) NOT NULL, closing_balance NUMERIC(19,2) NOT NULL, currency VARCHAR(3) NOT NULL, document_nonce BYTEA NOT NULL, document_content BYTEA NOT NULL, created_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_statements_customer ON statements(customer_id, period_end DESC);
CREATE TABLE disputes (id UUID PRIMARY KEY, customer_id UUID NOT NULL, transaction_id UUID NOT NULL, account_id UUID NOT NULL, reason VARCHAR(40) NOT NULL, details VARCHAR(1000) NOT NULL, amount NUMERIC(19,2) NOT NULL, currency VARCHAR(3) NOT NULL, status VARCHAR(30) NOT NULL, provisional_credit_id UUID, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_disputes_customer ON disputes(customer_id, created_at DESC);
CREATE UNIQUE INDEX uq_dispute_transaction ON disputes(customer_id, transaction_id);
CREATE TABLE notifications (id UUID PRIMARY KEY, customer_id UUID NOT NULL, category VARCHAR(30) NOT NULL, title VARCHAR(120) NOT NULL, message VARCHAR(500) NOT NULL, read_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_notifications_customer ON notifications(customer_id, created_at DESC);
CREATE TABLE notification_preferences (customer_id UUID PRIMARY KEY, in_app BOOLEAN NOT NULL, email BOOLEAN NOT NULL, security BOOLEAN NOT NULL, payments BOOLEAN NOT NULL, statements BOOLEAN NOT NULL, disputes BOOLEAN NOT NULL, updated_at TIMESTAMPTZ NOT NULL);
