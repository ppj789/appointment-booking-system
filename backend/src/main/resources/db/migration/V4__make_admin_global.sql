ALTER TABLE app_user
    DROP CONSTRAINT ck_app_user_role,
    ALTER COLUMN branch_id DROP NOT NULL;

ALTER TABLE app_user
    RENAME COLUMN role TO account_type;

UPDATE app_user
SET account_type = 'ADMIN',
    branch_id = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE account_type = 'BRANCH_ADMIN';

ALTER INDEX ix_app_user_branch_role_status
    RENAME TO ix_app_user_branch_type_status;

COMMENT ON COLUMN app_user.branch_id IS
    'Required for STAFF accounts; NULL for the global ADMIN account.';

COMMENT ON TABLE app_user IS
    'Global Admin and Branch-scoped Staff accounts; accounts are deactivated rather than deleted.';
