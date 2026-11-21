# Interview Q&A (matches this implementation)

Answers describe **this repo**, not a hypothetical production platform.

---

### Why SSE instead of WebSockets?

We only need **server → browser notifications**. Mutations already go REST. SSE is unidirectional, uses HTTP, and pairs cleanly with “invalidate + refetch.” WebSockets would add duplex complexity we do not use.

### Is the SSE payload the source of truth?

**No.** Payload is `ASSIGNMENT_CHANGED` metadata (`shiftId`, `assignmentId`, `action`, `occurredAt`). UI always **GET /dashboard** after invalidate. That avoids divergent client patches.

### When is the SSE event published?

Inside the mutation TX we publish a Spring application event; `@TransactionalEventListener(AFTER_COMMIT)` calls `ShiftSseService.publish`. Failed commits never notify. `occurredAt` is stamped **inside** the TX (slightly pre-commit relative to the listener).

### What happens if SSE disconnects mid-shift?

EventSource reconnects; `onopen` invalidates for catch-up. UI shows Reconnecting + “Live updates temporarily unavailable.” REST still works. E2E forces disconnect with a DEV helper because aborting the route alone does not close an already-open stream.

### How do you avoid duplicate dashboard GETs?

When connection state is **LIVE**, mutation `onSuccess` skips invalidate — SSE’s `assignment-changed` triggers the single refetch. If not LIVE, mutation invalidates so the actor still updates. 409 always invalidates.

### How is idempotency handled without Idempotency-Key?

`AssignmentTransitionService` returns the current entity unchanged when already in the target state (including preserving `checkedInAt` / `checkedOutAt`). Repeating check-in/out does not move timestamps.

### Why is DECLINED final?

Explicit Phase 1 product assumption. Cancelled can accept again; declined cannot. Illegal paths return **409 ProblemDetail**.

### How are dashboard counts computed?

`DashboardCountsCalculator`: filled = ACCEPTED count; remaining = max(required − filled, 0); checkedIn/Out from attendance. Not persisted — no dual-write.

### N+1 on dashboard?

`findAllByShiftIdWithWorker` uses `@EntityGraph(attributePaths = {"worker", "shift"})` plus one `findById` for the shift ≈ **two** queries for the tiny demo set.

### Optimistic locking?

`ShiftAssignment.version` (`@Version`). Stale conflicts surface as API conflicts; frontend invalidates on 409.

### Why no auth?

Challenge scope: demonstrate live staffing sync. Auth would dominate the demo without proving the core lifecycle. Production would add authz before any public deploy (`/api/dev/reset` is already profile-gated).

### Docker — what’s in the stack?

`postgres` + Spring Boot (`demo` profile, reset on) + nginx serving the Vite build and proxying `/api` (SSE-friendly: buffering off). Healthchecks on all three.

### How do you reset the demo safely?

`POST /api/dev/reset` registered only for profiles `local|demo|test` and `app.dev-reset.enabled=true`. Script: `./scripts/reset-demo.sh`.

### Performance numbers — can I quote them in prod?

**No.** Phase 6 numbers are localhost/VM medians for invite/accept/check-in/out, SSE, UI update, and reconnect. Useful for honesty in interview; not SLAs.

### What would you change first in production?

AuthN/Z, multi-instance SSE fan-out, remove/lock down dev reset, observability, and load testing beyond three assignments.
