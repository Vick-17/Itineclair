CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_accounts_email
    UNIQUE (email),

    CONSTRAINT ck_user_accounts_email_lowercase
    CHECK (email = LOWER(email)),

    CONSTRAINT ck_user_accounts_role
    CHECK (role IN ('USER'))
);
