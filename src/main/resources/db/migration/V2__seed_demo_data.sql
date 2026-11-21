-- Fixed UUIDs for reproducible demo / tests
-- Shift: Oloodi Event Staffing in Montréal, QC — requiredWorkers = 3
-- Demo window: 2026-12-15 14:00–22:00 UTC

INSERT INTO shifts (id, title, starts_at, ends_at, location, required_workers, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Oloodi Event Staffing',
    '2026-12-15 14:00:00+00',
    '2026-12-15 22:00:00+00',
    'Montréal, QC',
    3,
    '2026-11-01 12:00:00+00',
    '2026-11-01 12:00:00+00'
);

INSERT INTO workers (id, name, created_at) VALUES
    ('22222222-2222-2222-2222-222222222201', 'Sarah Johnson', '2026-11-01 12:00:00+00'),
    ('22222222-2222-2222-2222-222222222202', 'Daniel Martin', '2026-11-01 12:00:00+00'),
    ('22222222-2222-2222-2222-222222222203', 'Alex Smith',    '2026-11-01 12:00:00+00');

INSERT INTO shift_assignments (
    id, shift_id, worker_id,
    participation_status, attendance_status,
    checked_in_at, checked_out_at,
    version, created_at, updated_at
) VALUES
    (
        '33333333-3333-3333-3333-333333333301',
        '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222201',
        'UNINVITED', 'NOT_STARTED',
        NULL, NULL,
        0, '2026-11-01 12:00:00+00', '2026-11-01 12:00:00+00'
    ),
    (
        '33333333-3333-3333-3333-333333333302',
        '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222202',
        'UNINVITED', 'NOT_STARTED',
        NULL, NULL,
        0, '2026-11-01 12:00:00+00', '2026-11-01 12:00:00+00'
    ),
    (
        '33333333-3333-3333-3333-333333333303',
        '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222203',
        'UNINVITED', 'NOT_STARTED',
        NULL, NULL,
        0, '2026-11-01 12:00:00+00', '2026-11-01 12:00:00+00'
    );
