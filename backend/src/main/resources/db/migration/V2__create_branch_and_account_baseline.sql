CREATE TABLE branch (
    id uuid PRIMARY KEY,
    public_code varchar(30) NOT NULL,
    name varchar(120) NOT NULL,
    display_address varchar(300) NOT NULL,
    timezone varchar(64) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_branch_public_code UNIQUE (public_code),
    CONSTRAINT ck_branch_public_code
        CHECK (
            public_code = upper(btrim(public_code))
            AND char_length(public_code) BETWEEN 2 AND 30
        ),
    CONSTRAINT ck_branch_name
        CHECK (name = btrim(name) AND char_length(name) BETWEEN 1 AND 120),
    CONSTRAINT ck_branch_display_address
        CHECK (
            display_address = btrim(display_address)
            AND char_length(display_address) BETWEEN 1 AND 300
        ),
    CONSTRAINT ck_branch_timezone
        CHECK (timezone = btrim(timezone) AND char_length(timezone) BETWEEN 1 AND 64)
);

CREATE TABLE app_user (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL,
    role varchar(20) NOT NULL,
    account_status varchar(20) NOT NULL,
    display_name varchar(100) NOT NULL,
    email varchar(254) NOT NULL,
    password_hash varchar(255),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at timestamptz,
    deactivated_at timestamptz,

    CONSTRAINT fk_app_user_branch
        FOREIGN KEY (branch_id) REFERENCES branch (id) ON DELETE RESTRICT,
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role
        CHECK (role IN ('BRANCH_ADMIN', 'STAFF')),
    CONSTRAINT ck_app_user_status
        CHECK (account_status IN ('PENDING_SETUP', 'ACTIVE', 'DEACTIVATED')),
    CONSTRAINT ck_app_user_display_name
        CHECK (
            display_name = btrim(display_name)
            AND char_length(display_name) BETWEEN 1 AND 100
        ),
    CONSTRAINT ck_app_user_email
        CHECK (
            email = lower(btrim(email))
            AND char_length(email) BETWEEN 3 AND 254
        ),
    CONSTRAINT ck_app_user_password_hash
        CHECK (password_hash IS NULL OR char_length(password_hash) BETWEEN 20 AND 255),
    CONSTRAINT ck_app_user_timestamps
        CHECK (
            updated_at >= created_at
            AND (activated_at IS NULL OR activated_at >= created_at)
            AND (deactivated_at IS NULL OR deactivated_at >= created_at)
        ),
    CONSTRAINT ck_app_user_lifecycle
        CHECK (
            (
                account_status = 'PENDING_SETUP'
                AND password_hash IS NULL
                AND activated_at IS NULL
                AND deactivated_at IS NULL
            )
            OR (
                account_status = 'ACTIVE'
                AND password_hash IS NOT NULL
                AND activated_at IS NOT NULL
                AND deactivated_at IS NULL
            )
            OR (
                account_status = 'DEACTIVATED'
                AND deactivated_at IS NOT NULL
                AND (
                    (activated_at IS NULL AND password_hash IS NULL)
                    OR (activated_at IS NOT NULL AND password_hash IS NOT NULL)
                )
            )
        )
);

CREATE INDEX ix_app_user_branch_role_status
    ON app_user (branch_id, role, account_status);

CREATE TABLE credential_setup (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    token_hash varchar(64) NOT NULL,
    status varchar(20) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    revoked_at timestamptz,

    CONSTRAINT fk_credential_setup_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT uq_credential_setup_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_credential_setup_token_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_credential_setup_status
        CHECK (status IN ('ACTIVE', 'CONSUMED', 'REVOKED')),
    CONSTRAINT ck_credential_setup_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT ck_credential_setup_lifecycle
        CHECK (
            (
                status = 'ACTIVE'
                AND consumed_at IS NULL
                AND revoked_at IS NULL
            )
            OR (
                status = 'CONSUMED'
                AND consumed_at IS NOT NULL
                AND consumed_at >= created_at
                AND revoked_at IS NULL
            )
            OR (
                status = 'REVOKED'
                AND revoked_at IS NOT NULL
                AND revoked_at >= created_at
                AND consumed_at IS NULL
            )
        )
);

CREATE UNIQUE INDEX uq_credential_setup_active_user
    ON credential_setup (user_id)
    WHERE status = 'ACTIVE';

CREATE INDEX ix_credential_setup_user_created
    ON credential_setup (user_id, created_at DESC);

COMMENT ON TABLE branch IS
    'Read-only, preconfigured physical appointment locations.';
COMMENT ON TABLE app_user IS
    'Branch-scoped Branch Admin and Staff accounts; accounts are deactivated rather than deleted.';
COMMENT ON TABLE credential_setup IS
    'Hash-only, one-time capabilities for initial Staff password setup.';
