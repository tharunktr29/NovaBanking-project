ALTER TABLE payment_orders ADD COLUMN risk_assessment_id UUID;
ALTER TABLE payment_orders ADD COLUMN customer_safe_reason VARCHAR(240);
CREATE UNIQUE INDEX uq_payment_risk_assessment ON payment_orders(risk_assessment_id) WHERE risk_assessment_id IS NOT NULL;
