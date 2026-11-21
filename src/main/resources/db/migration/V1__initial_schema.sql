CREATE TABLE workers (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE shifts (
    id               UUID PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    starts_at        TIMESTAMPTZ  NOT NULL,
    ends_at          TIMESTAMPTZ  NOT NULL,
    location         VARCHAR(255) NOT NULL,
    required_workers INTEGER      NOT NULL CHECK (required_workers >= 0),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE shift_assignments (
    id                   UUID PRIMARY KEY,
    shift_id             UUID         NOT NULL REFERENCES shifts (id),
    worker_id            UUID         NOT NULL REFERENCES workers (id),
    participation_status VARCHAR(32)  NOT NULL,
    attendance_status    VARCHAR(32)  NOT NULL,
    checked_in_at        TIMESTAMPTZ,
    checked_out_at       TIMESTAMPTZ,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_shift_assignments_shift_worker UNIQUE (shift_id, worker_id),
    CONSTRAINT chk_participation_status CHECK (
        participation_status IN ('UNINVITED', 'INVITED', 'ACCEPTED', 'DECLINED', 'CANCELLED')
    ),
    CONSTRAINT chk_attendance_status CHECK (
        attendance_status IN ('NOT_STARTED', 'CHECKED_IN', 'CHECKED_OUT')
    )
);

CREATE INDEX idx_shift_assignments_shift_id ON shift_assignments (shift_id);
CREATE INDEX idx_shift_assignments_worker_id ON shift_assignments (worker_id);
