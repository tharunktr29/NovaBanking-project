CREATE TABLE card_payment_applications (
    id UUID PRIMARY KEY,
    business_operation_id UUID NOT NULL UNIQUE,
    card_id UUID NOT NULL REFERENCES bank_cards(id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    correlation_id UUID NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_card_payment_applications_amount CHECK (amount > 0),
    CONSTRAINT chk_card_payment_applications_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_card_payment_applications_business_operation ON card_payment_applications(business_operation_id);
CREATE INDEX idx_card_payment_applications_card_id ON card_payment_applications(card_id);
