ALTER TABLE app_user
    ADD CONSTRAINT uq_app_user_id_branch UNIQUE (id, branch_id);

CREATE TABLE invitation (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL,
    staff_user_id uuid NOT NULL,
    status varchar(20) NOT NULL,
    title varchar(120) NOT NULL,
    description varchar(2000) NOT NULL,
    guest_name varchar(100) NOT NULL,
    guest_email varchar(254) NOT NULL,
    default_duration_minutes smallint NOT NULL,
    token_hash varchar(64),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at timestamptz,
    consumed_at timestamptz,
    revoked_at timestamptz,

    CONSTRAINT uq_invitation_id_scope
        UNIQUE (id, branch_id, staff_user_id),
    CONSTRAINT fk_invitation_staff_branch
        FOREIGN KEY (staff_user_id, branch_id)
        REFERENCES app_user (id, branch_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_invitation_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_invitation_staff_status_created
    ON invitation (staff_user_id, status, created_at DESC);

CREATE INDEX ix_invitation_active_staff
    ON invitation (staff_user_id)
    WHERE status = 'ACTIVE';

CREATE TABLE calendar_reservation (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL,
    staff_user_id uuid NOT NULL,
    kind varchar(20) NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_calendar_reservation_id_scope
        UNIQUE (id, branch_id, staff_user_id),
    CONSTRAINT fk_calendar_reservation_staff_branch
        FOREIGN KEY (staff_user_id, branch_id)
        REFERENCES app_user (id, branch_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_calendar_reservation_interval
        CHECK (ends_at > starts_at),
    CONSTRAINT ex_calendar_reservation_staff_time
        EXCLUDE USING gist (
            staff_user_id WITH =,
            tstzrange(starts_at, ends_at, '[)') WITH &&
        )
);

CREATE INDEX ix_calendar_reservation_staff_start
    ON calendar_reservation (staff_user_id, starts_at);

CREATE TABLE availability_block (
    id uuid PRIMARY KEY,

    CONSTRAINT fk_availability_block_reservation
        FOREIGN KEY (id)
        REFERENCES calendar_reservation (id)
        ON DELETE RESTRICT
);

CREATE TABLE appointment (
    id uuid PRIMARY KEY,
    invitation_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    staff_user_id uuid NOT NULL,
    public_reference varchar(14) NOT NULL,
    idempotency_key uuid NOT NULL,
    request_fingerprint varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointment_reservation_scope
        FOREIGN KEY (id, branch_id, staff_user_id)
        REFERENCES calendar_reservation (id, branch_id, staff_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_invitation_scope
        FOREIGN KEY (invitation_id, branch_id, staff_user_id)
        REFERENCES invitation (id, branch_id, staff_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_appointment_invitation UNIQUE (invitation_id),
    CONSTRAINT uq_appointment_public_reference UNIQUE (public_reference),
    CONSTRAINT uq_appointment_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX ix_appointment_staff_created
    ON appointment (staff_user_id, created_at DESC);

COMMENT ON TABLE invitation IS
    'Staff-authored, Branch-bound single-use Guest booking capabilities; raw tokens are never stored.';
COMMENT ON TABLE calendar_reservation IS
    'Authoritative Staff-calendar occupancy using half-open UTC intervals.';
COMMENT ON TABLE availability_block IS
    'Private unavailable time specialized from a calendar reservation.';
COMMENT ON TABLE appointment IS
    'Immutable committed booking facts and successful idempotency record.';
