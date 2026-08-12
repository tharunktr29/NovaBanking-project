CREATE TABLE user_credentials (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL,
    failed_login_count INTEGER NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_user_credentials_email ON user_credentials(email);
CREATE INDEX idx_user_credentials_username ON user_credentials(username);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_credentials(id),
    token_hash VARCHAR(96) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_hash VARCHAR(96),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE TABLE login_attempts (
    id UUID PRIMARY KEY,
    username_or_email VARCHAR(160) NOT NULL,
    successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(80),
    ip_address VARCHAR(80),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_login_attempts_username_time ON login_attempts(username_or_email, attempted_at);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    action VARCHAR(80) NOT NULL,
    correlation_id VARCHAR(80),
    metadata VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_logs_user_time ON audit_logs(user_id, created_at);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_credentials(id),
    token_hash VARCHAR(96) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_password_reset_tokens_hash ON password_reset_tokens(token_hash);
