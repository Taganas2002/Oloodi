---
cursor:
  subagentId: "bc-0f35d76a-9bd9-5dea-9722-26198f66dec9"
---

# Phase 3 SSE Report — Oloodi Live Staffing Dashboard

## Summary

Phase 3 live sync is complete: Spring MVC `SseEmitter` + AFTER_COMMIT domain events on the backend; `useShiftLiveUpdates` (EventSource → TanStack Query invalidate) on Manager/Worker. **No WebSockets, JWT, polling intervals, optimistic SSE patching, or Phase 4+.**

| Check | Result |
|-------|--------|
| Backend tests | `mvn test` → **46 passed**, 0 failures |
| Frontend tests | `npm test` → **17 passed** (5 files) |
| Frontend build | `npm run build` → **SUCCESS** |
| Multi-session SSE | **PASS** (3 concurrent streams; illegal mutation = 0 events) |
| UI Live indicator | Manager + Worker show `LIVE` |

**Git:** `cursor/phase-3-sse-7889` (from Phase 2). No GitHub remote on this run.

---

## Files created / changed

### Backend (`realtime/`)

| File | Role |
|------|------|
| `AssignmentChangedEvent.java` | Small notification DTO |
| `ShiftSseService.java` | Per-shift emitter registry + publish/heartbeat |
| `AssignmentChangedSseListener.java` | `@TransactionalEventListener(AFTER_COMMIT)` |
| `ShiftSseController.java` | `GET /api/shifts/{shiftId}/events` |
| `SseHeartbeatScheduler.java` | ~20s comment heartbeat (no refetch) |
| `AssignmentActionService.java` | Publishes event inside TX after successful transition |
| Tests | `ShiftSseServiceTest`, `AssignmentChangedNotifyIT` |
| `AbstractPostgresIntegrationTest` | Shared container singleton (no stop between IT classes) |

### Frontend

| File | Role |
|------|------|
| `hooks/useShiftLiveUpdates.ts` (+ test) | EventSource, CONNECTING/LIVE/RECONNECTING, invalidate on open+event |
| `components/LiveStatus.tsx` | Live / Reconnecting pill |
| `pages/ManagerPage.tsx`, `WorkerPage.tsx` | Subscribe + indicator |
| `vite.config.ts` | Proxy timeout 0 for SSE |
| `index.css` | Live status styles |
| `README.md` | Phase 3 live sync section |

### Media / docs

- `/cursor/stores/self/docs/phase-3-sse-report.md`
- `/agent/live-staffing-dashboard/docs/phase-3-sse-report.md`
- `/cursor/stores/self/media/phase-3/{manager-live,worker-live,manager-live-390}.png`

---

## Backend SSE architecture

```
REST mutation (@Transactional)
  → save assignment
  → ApplicationEventPublisher.publish(AssignmentChangedEvent)
  → commit
  → @TransactionalEventListener(AFTER_COMMIT) → ShiftSseService.publish
  → each per-shift SseEmitter gets event:assignment-changed
```

- Thread-safe `ConcurrentHashMap` + `CopyOnWriteArrayList` registry
- Remove on complete / timeout / error / send failure
- Broken emitter removed without failing mutation or other emitters
- Heartbeat = SSE **comment** only (`:heartbeat`) — not `ASSIGNMENT_CHANGED`
- Logs: connect / disconnect / publish / stale remove (no heartbeat INFO spam)

---

## Frontend live architecture

- One `EventSource` per `shiftId` in `useShiftLiveUpdates`
- On `open` → state `LIVE` + invalidate dashboard (catch-up)
- On `assignment-changed` → invalidate only (never patch from payload)
- On `error` → `RECONNECTING` (browser auto-reconnect)
- Cleanup `close()` on unmount
- No `refetchInterval`, no `WebSocket`, no `location.reload`, no JWT

---

## Connection states

| State | When |
|-------|------|
| CONNECTING | Initial mount / before first open |
| LIVE | `onopen` fired |
| RECONNECTING | `onerror` after a prior successful open |

---

## Event format example

```
event:assignment-changed
id:33333333-3333-3333-3333-333333333301:1763727220000
data:{"type":"ASSIGNMENT_CHANGED","shiftId":"11111111-1111-1111-1111-111111111111","assignmentId":"33333333-3333-3333-3333-333333333301","action":"invite","occurredAt":"2026-11-21T13:33:40.002141Z"}
```

Not a full dashboard — clients refetch via REST.

---

## Tests (exact counts)

### Backend — `mvn test`

```
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Includes prior Phase 1 (39) + SSE:

- `ShiftSseServiceTest` (4): registry cleanup, publish, broken-emitter isolation, heartbeat isolation
- `AssignmentChangedNotifyIT` (3): AFTER_COMMIT notify on invite; illegal accept → no notify; subscribe cleanup

### Frontend — `npm test`

```
Test Files  5 passed (5)
     Tests  17 passed (17)
```

Phase 2 (13) + `useShiftLiveUpdates` (4): open→LIVE+invalidate; event→invalidate; error→RECONNECTING; unmount closes source.

### Build

```
npm run build → SUCCESS
```

---

## Manual multi-session sequences observed

Three concurrent SSE clients (Manager, Worker, Worker2) against live `:8080`:

| Step | Observation |
|------|-------------|
| A Invite Sarah | All 3 streams received `action:invite`; Worker would see UNINVITED→INVITED after refetch |
| B Accept | All 3 received `accept`; dashboard `filled: 0→1` |
| C Check-in / check-out | All 3 received both; counts `checkedOut: 1` |
| D Second worker session | Worker2 received the same 4 events as Manager/Worker |
| Persist | GET assignment after refresh → `ACCEPTED` / `CHECKED_OUT` |
| Illegal accept (Daniel UNINVITED) | HTTP 409; **0** new SSE events |

UI: two browser contexts at `/manager` and `/worker` both showed **Live** indicator (`data-state=LIVE`). Screenshots under `media/phase-3/`.

Confirmed: only intentional `EventSource` usage; no WebSocket / JWT / `refetchInterval`.

---

## Failure behavior

- Illegal / rolled-back mutations never publish AFTER_COMMIT events
- IOException on one emitter → remove that emitter; siblings still receive
- Frontend 409 still invalidates via existing mutation `onError` (Phase 2)

---

## Emitter cleanup

- `onCompletion` / `onTimeout` / `onError` → remove from registry
- Empty shift lists dropped from map
- Heartbeat also prunes broken emitters

---

## Assumptions / issues

- DECLINED remains FINAL (unchanged Phase 1 rules)
- Native EventSource auto-reconnect is sufficient (no custom backoff UI beyond RECONNECTING)
- Vite proxy `timeout: 0` needed so SSE is not cut by the dev proxy
- Shared Testcontainers singleton used so IT classes don't lose the DB mid-suite
- No GitHub remote — local branch only

---

## Regression Phase 1 / 2

- All prior BE transition/API/idempotency/count tests still pass (included in 46)
- All prior FE action/API/banner/count tests still pass (included in 17)
- Transitions / REST contracts unchanged; only additive SSE notify + FE subscribe

---

## Next phase (do not implement)

**Phase 4 (logical only):** reliability hardening — Idempotency-Key, latency metrics, Playwright E2E, Docker Compose, final README polish. Not started.
