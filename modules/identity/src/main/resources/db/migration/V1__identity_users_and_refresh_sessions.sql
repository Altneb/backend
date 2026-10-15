CREATE TABLE identity_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_identity_users_email UNIQUE (email),
    CONSTRAINT ck_identity_users_email_lowercase CHECK (email = LOWER(email))
);

CREATE TABLE identity_refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity_users(id),
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_id UUID,
    CONSTRAINT uk_identity_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_identity_refresh_replacement FOREIGN KEY (replaced_by_id)
        REFERENCES identity_refresh_sessions(id)
);

CREATE INDEX ix_identity_refresh_user ON identity_refresh_sessions(user_id);
CREATE INDEX ix_identity_refresh_family ON identity_refresh_sessions(family_id);
