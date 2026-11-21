---
cursor:
  subagentId: "bc-abed1a09-ba15-50cc-a919-39922b9d7889"
---

# Phase 1 Backend Report — Oloodi Live Staffing Dashboard

## Summary

Phase 1 backend is complete under `/agent/live-staffing-dashboard`: domain, Flyway schema+seed, centralized transitions, REST API (DTOs only), ProblemDetail errors, and tests A–E. **No frontend, SSE, Docker Compose, or section 18.**

**Tests executed:** `mvn test` → **39 tests, 0 failures, 0 errors, BUILD SUCCESS.**

**Git:** branch `cursor/phase-1-backend-7889` (local commit). No GitHub remote on this run — PR not opened.

---

## Files created

Project root: `/agent/live-staffing-dashboard/`

| Area | Paths |
|------|--------|
| Build | `pom.xml`, `.gitignore`, `README.md` |
| App | `src/main/java/com/oloodi/livestaffing/LiveStaffingApplication.java` |
| Common | `common/config/ClockConfig.java`, `common/error/{GlobalExceptionHandler,ResourceNotFoundException,ConflictException}.java` |
| Shift | `shift/domain/Shift.java`, `shift/dto/*`, `shift/repository/ShiftRepository.java`, `shift/service/{ShiftQueryService,DashboardCountsCalculator}.java`, `shift/web/ShiftController.java` |
| Worker | `worker/domain/Worker.java`, `worker/repository/WorkerRepository.java` |
| Assignment | `assignment/domain/{ShiftAssignment,ParticipationStatus,AttendanceStatus}.java`, `assignment/dto/AssignmentResponse.java`, `assignment/repository/ShiftAssignmentRepository.java`, `assignment/service/{AssignmentTransitionService,AssignmentActionService,IllegalTransitionException}.java`, `assignment/web/AssignmentActionController.java` |
| Flyway | `src/main/resources/db/migration/V1__initial_schema.sql`, `V2__seed_demo_data.sql` |
| Config | `src/main/resources/application.yml` |
| Tests | `AssignmentTransitionServiceTest`, `DashboardCountsCalculatorTest`, `ShiftAssignmentPersistenceIT`, `AssignmentApiIT`, `support/{AbstractPostgresIntegrationTest,DemoIds}` |

Also: this report at `/cursor/stores/self/docs/phase-1-backend-report.md` and a copy at `/agent/live-staffing-dashboard/docs/phase-1-backend-report.md`.

---

## Architecture

Feature-oriented packages: `shift/`, `worker/`, `assignment/`, `common/`.

- **Domain** — JPA entities; enums kept separate (`ParticipationStatus`, `AttendanceStatus`).
- **Transitions** — `AssignmentTransitionService` is the single legal-transition authority (invite/accept/decline/cancel/check-in/check-out), including natural idempotency.
- **Mutations** — `AssignmentActionService` loads assignment (EntityGraph), applies transition, `@Transactional` save.
- **Queries** — `ShiftQueryService` dashboard/list/get; counts derived in `DashboardCountsCalculator` (not persisted).
- **API** — Controllers return DTOs only; action endpoints are dedicated POSTs (no generic PUT status).
- **Errors** — `GlobalExceptionHandler` → Spring `ProblemDetail` (400/404/409/500), no stack traces in body.

---

## Database schema

**workers:** `id`, `name`, `created_at`  
**shifts:** `id`, `title`, `starts_at`, `ends_at`, `location`, `required_workers`, `created_at`, `updated_at`  
**shift_assignments:** `id`, `shift_id`, `worker_id`, `participation_status`, `attendance_status`, `checked_in_at`, `checked_out_at`, `version` (`@Version`), `created_at`, `updated_at`  
- `UNIQUE (shift_id, worker_id)`  
- CHECK constraints on enum string values  

**Seed (V2):** shift `Oloodi Event Staffing` / `Montréal, QC` / requiredWorkers=3; workers Sarah Johnson, Daniel Martin, Alex Smith; three assignments `UNINVITED`/`NOT_STARTED` with null check times. Invite updates existing rows only.

Stable demo UUIDs documented in README.

---

## State-transition table

| From (participation + attendance) | Action | To |
|-----------------------------------|--------|-----|
| UNINVITED + NOT_STARTED | invite | INVITED + NOT_STARTED |
| INVITED + NOT_STARTED | accept | ACCEPTED + NOT_STARTED |
| INVITED + NOT_STARTED | decline | DECLINED + NOT_STARTED (**FINAL**) |
| ACCEPTED + NOT_STARTED | cancel | CANCELLED + NOT_STARTED |
| CANCELLED + NOT_STARTED | accept | ACCEPTED + NOT_STARTED |
| ACCEPTED + NOT_STARTED | check-in | ACCEPTED + CHECKED_IN (sets `checkedInAt` once) |
| ACCEPTED + CHECKED_IN | check-out | ACCEPTED + CHECKED_OUT (sets `checkedOutAt` once) |

Idempotent no-ops when already in target state (timestamps unchanged). All other transitions → 409.

---

## REST API

| Method | Path |
|--------|------|
| GET | `/api/shifts/{shiftId}/dashboard` |
| GET | `/api/shifts/{shiftId}/assignments` |
| GET | `/api/shifts/{shiftId}/assignments/{assignmentId}` |
| POST | `.../invite`, `/accept`, `/decline`, `/cancel`, `/check-in`, `/check-out` |

Dashboard: shift + assignments + counts `{filled, remaining, checkedIn, checkedOut}`  
- filled = ACCEPTED count; remaining = max(required−filled, 0); checkedIn/Out by attendance.

---

## Reliability decisions

- Server-owned `Instant` for check-in/out; client never submits those fields.
- Timestamps truncated to **microseconds** (PostgreSQL `timestamptz` precision) for stable idempotent responses.
- Natural idempotency only (no Idempotency-Key header).
- `@Version` optimistic locking → HTTP 409 with message: *"The assignment was changed by another session. Reload the latest state."*
- `@Transactional` on mutation methods.
- DB creds via `DB_URL` / `DB_USER` / `DB_PASSWORD` env vars; no secrets committed.
- No JWT/auth (per Phase 1 hygiene).

---

## Query behavior (N+1)

`ShiftAssignmentRepository` uses `@EntityGraph(attributePaths = {"worker", "shift"})` on list and single-assignment queries so dashboard/list avoid N+1 worker loads. No pagination/Redis.

---

## Tests (executed)

```
mvn test
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Category | Coverage | Count (approx) |
|----------|----------|----------------|
| A. Legal transitions | Unit + REST happy paths (invite→accept→check-in→check-out, decline, cancel→accept) | 7 unit + 3 REST |
| B. Illegal | UNINVITED→ACCEPTED/CHECK_IN, DECLINED→CHECK_IN, NOT_STARTED→CHECK_OUT, CHECKED_OUT→CHECK_IN, CHECKED_IN/OUT→CANCEL | 7 unit + 7 REST |
| C. Idempotency | invite/check-in/check-out twice; timestamps stable; assignment count stays 3 | 3 unit + 2 REST |
| D. Counts vs list | Dashboard counts match assignment list derivation | 2 unit + 1 REST (+ related API 404/400/DTO) |
| E. Testcontainers | Flyway+seed, unique constraint, persistence (update not insert), optimistic lock | 4 IT |

---

## Assumptions

1. **DECLINED is FINAL** — no `DECLINED → ACCEPTED` (or other) transition; product assumption for Phase 1.
2. Seed creates all assignment rows up front; invite never inserts.
3. Participation remains `ACCEPTED` during check-in/out.
4. Cancel only before check-in (`ACCEPTED + NOT_STARTED`).

---

## Issues / environment notes

- This cloud run had **no linked GitHub repo** — code is committed locally on `cursor/phase-1-backend-7889`; push/PR when a remote is available.
- Docker Engine 29 defaults to min API 1.40; Testcontainers 1.20.x negotiates ~1.32. For local IT runs on Docker 29, set daemon `"min-api-version": "1.24"` (used successfully in this environment) or upgrade Testcontainers.
- `TESTCONTAINERS_RYUK_DISABLED=true` set in Surefire for this environment.

---

## How to run

```bash
export DB_URL=jdbc:postgresql://localhost:5432/live_staffing
export DB_USER=oloodi
export DB_PASSWORD=oloodi
cd live-staffing-dashboard
mvn spring-boot:run   # app
mvn test              # requires Docker for Testcontainers
```

---

## Next step (Phase 2 — do not implement)

Logical Phase 2 only: frontend live dashboard UI, SSE (or similar) for real-time updates, and any Compose/ops packaging from later spec sections — **not started**.
