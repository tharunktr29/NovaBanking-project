CREATE TABLE customer_profiles (
    id UUID PRIMARY KEY,
    auth_user_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL,
    phone VARCHAR(32),
    address_line_1 VARCHAR(160) NOT NULL,
    address_line_2 VARCHAR(160),
    city VARCHAR(80) NOT NULL,
    state VARCHAR(2) NOT NULL,
    postal_code VARCHAR(16) NOT NULL,
    country VARCHAR(80) NOT NULL,
    kyc_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_customer_profiles_auth_user_id ON customer_profiles(auth_user_id);
CREATE INDEX idx_customer_profiles_email ON customer_profiles(email);

CREATE TABLE customer_preferences (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL UNIQUE,
    email_alerts BOOLEAN NOT NULL,
    sms_alerts BOOLEAN NOT NULL,
    push_alerts BOOLEAN NOT NULL,
    security_alerts BOOLEAN NOT NULL,
    payment_alerts BOOLEAN NOT NULL,
    low_balance_alerts BOOLEAN NOT NULL,
    paperless_statements BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_customer_preferences_customer_id ON customer_preferences(customer_id);

INSERT INTO customer_profiles (
    id,
    auth_user_id,
    first_name,
    last_name,
    email,
    phone,
    address_line_1,
    address_line_2,
    city,
    state,
    postal_code,
    country,
    kyc_status,
    created_at,
    updated_at,
    version
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Demo',
    'Customer',
    'demo.user@novabank.test',
    '+1 555 010 2000',
    '100 Fictional Avenue',
    'Suite 21',
    'Indianapolis',
    'IN',
    '46204',
    'USA',
    'VERIFIED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

INSERT INTO customer_preferences (
    id,
    customer_id,
    email_alerts,
    sms_alerts,
    push_alerts,
    security_alerts,
    payment_alerts,
    low_balance_alerts,
    paperless_statements,
    created_at,
    updated_at,
    version
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222222',
    true,
    false,
    true,
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);
