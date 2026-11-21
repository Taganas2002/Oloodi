---
cursor:
  subagentId: "bc-ad30a3cf-dee1-5af4-89de-21eaf6f24321"
---

# Phase 4 Reliability Report — Oloodi Live Staffing Dashboard

## Summary

Phase 4 hardens reconnect catch-up, conflict/idempotency behavior, emitter isolation, and Live/Reconnecting UX. **No Playwright E2E, perf study, UI polish pass, or final README** (later phases).

| Check | Result |
|-------|--------|
| Backend tests | `mvn test` → **52 passed**, 0 failures |
| Frontend tests | `npm test` → **22 passed** (7 files) |
| Frontend build | `npm run build` → **SUCCESS** |
| Manual A reconnect catch-up | **PASS** |
| Manual B stale cancel | **PASS** |

**Git:** `cursor/phase-4-reliability-7889` (from Phase 3). No GitHub remote on this run.

---

## Files created / changed

### Frontend

| Path | Change |
|------|--------|
| `frontend/src/components/LiveStatus.tsx` | ● Live / ⚠ Reconnecting… + “Live updates temporarily unavailable.” |
| `frontend/src/components/LiveStatus.test.tsx` | New |
| `frontend/src/lib/errors.ts` | Clearer 409 concurrency fallback |
| `frontend/src/hooks/useDashboard.ts` | Documented no-poll; 409 invalidate |
| `frontend/src/hooks/useDashboard.test.tsx` | 409→invalidate; `isPending` |
| `frontend/src/components/ErrorBanner.test.tsx` | Optimistic-lock copy |
| `frontend/src/index.css` | Live status wrap / hint styles |

### Backend

| Path | Change |
|------|--------|
| `realtime/SseTimingHooks.java` | Lightweight counters for Phase 6 (no perf claims) |
| `realtime/ShiftSseService.java` | Timing hooks; clearer delivery-fail / error logs |
| `realtime/SseHeartbeatScheduler.java` | Explicit KEEP decision documented |
| `common/error/GlobalExceptionHandler.java` | INFO logs for illegal-transition / optimistic-lock conflicts |
| `assignment/IdempotencyHttpIT.java` | Duplicate invite / check-in / check-out |
| `assignment/StaleConflictIT.java` | Stale cancel 409; @Version; concurrent check-in vs cancel |
| `assignment/ShiftAssignmentPersistenceIT.java` | Reset seed before assertions (shared DB) |

### Docs / media

- `/cursor/stores/self/docs/phase-4-reliability-report.md`
- `/agent/live-staffing-dashboard/docs/phase-4-reliability-report.md`
- `/cursor/stores/self/media/phase-4/{manager-live,worker-live,reconnecting-indicator}.png`

---

## Reconnect architecture

1. Native `EventSource` auto-reconnects after stream errors.
2. `onerror` → UI `RECONNECTING` (after first successful open) + hint text.
3. `onopen` (including reopen) → `LIVE` + **invalidate/refetch** dashboard (REST catch-up).
4. Missed SSE events while disconnected are **not** replayed from a bus — recovery is always authoritative REST refetch on reconnect.
5. No `refetchInterval`, WebSocket, JWT, or local state patch from SSE payloads.

---

## Catch-up

On every SSE `open`/`reopen`, `useShiftLiveUpdates` calls `queryClient.invalidateQueries(dashboardKey)`. That refetches shift + assignments + derived counts. Heartbeat comments never trigger invalidate.

---

## Conflict handling

| Mechanism | What it protects | Client sees |
|-----------|------------------|-------------|
| Illegal transition (service rules) | Stale *logical* intent (e.g. cancel after check-in) | HTTP **409** ProblemDetail with transition detail |
| `@Version` optimistic lock | Concurrent JPA writes on same row | HTTP **409** “changed by another session…” |

**Important:** REST action POSTs do **not** send client version tokens. `@Version` still protects two server transactions that load the same assignment, mutate, and flush — the second flush fails instead of silently overwriting. Cross-session “stale UI button” cases usually hit **illegal transition** 409 after the first session changed state, then FE invalidates and refetches.

FE: 409 → user-facing ProblemDetail (or concurrency fallback) + invalidate/refetch. Mutation buttons use `disabled={mutation.isPending}` (UX only).

---

## Idempotency

Natural only (no Idempotency-Key header):

- Duplicate invite → still INVITED, still 3 assignment rows
- Duplicate check-in / check-out → same timestamps, counts unchanged

Verified in `IdempotencyHttpIT` + manual regression.

---

## Tests (exact counts)

### Backend — `mvn test`

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Phase 4 additions include: `IdempotencyHttpIT` (2), `StaleConflictIT` (4), plus prior Phase 1–3 coverage (SSE isolation already in `ShiftSseServiceTest`).

### Frontend — `npm test`

```
Test Files  7 passed (7)
     Tests  22 passed (22)
```

Includes: LiveStatus LIVE/RECONNECTING, useShiftLiveUpdates open/error/event, useAssignmentAction 409+pending, ErrorBanner/ProblemDetail.

### Build

```
npm run build → SUCCESS
```

---

## Manual reconnect (Scenario A) — steps + result

1. Open Manager + Worker SSE streams (both “connected”).
2. **Destroy Manager SSE only** (simulates Reconnecting on that client).
3. While manager down: invite → accept → check-in Sarah via REST (worker path).
4. Observe: worker stream received 3 events; manager stream received **0** while down.
5. Reopen Manager SSE + REST dashboard refetch (same as FE `onopen` invalidate).

**Result: PASS** — manager catch-up showed `ACCEPTED` / `CHECKED_IN` / timestamp `2026-11-21T13:50:49.992640Z` / counts `{filled:1, remaining:2, checkedIn:1, checkedOut:0}` **without** relying on missed event replay.

---

## Manual stale-session (Scenario B) — result

1. Daniel → invite + accept (`ACCEPTED` + `NOT_STARTED`).
2. Session A: check-in → `CHECKED_IN`.
3. Session B: cancel → **409** `Illegal transition: cannot cancel from participation=ACCEPTED attendance=CHECKED_IN`.
4. Refetch → still `CHECKED_IN` with stable timestamp.
5. Duplicate check-in → **same** `checkedInAt`.

**Result: PASS**

---

## Heartbeat decision

**KEEP** (~20s, configurable `app.sse.heartbeat-ms`).

Rationale: idle SSE is often killed by proxies/NATs; comment-only heartbeat keeps the stream alive and surfaces dead emitters on send failure **without** emitting `ASSIGNMENT_CHANGED` (no dashboard refetch). No INFO spam per tick — failures prune emitters quietly aside from disconnect/delivery-fail logs.

---

## Known limitations

- No event replay buffer / Last-Event-ID catch-up beyond REST refetch on reconnect.
- Clients do not submit `@Version`; optimistic lock covers concurrent *server* writers, while most multi-tab races surface as illegal-transition 409.
- No Idempotency-Key subsystem (natural idempotency sufficient for demonstrated failures).
- Reconnecting screenshot is illustrative of indicator copy; LIVE screenshots from running app; reconnect behavior proven via SSE destroy + unit tests.

---

## Regression

- Phase 1 transitions / ProblemDetail / counts still covered in the 52 BE tests.
- Phase 2 manager/worker actions + dual badges unchanged.
- Phase 3 SSE notify path unchanged (AFTER_COMMIT → invalidate).
- Scan: no `refetchInterval` / WebSocket / JWT / `location.reload` in `frontend/src`.

---

## Next phase (do not implement)

**Phase 5 (logical only):** Playwright (or similar) multi-browser E2E for live sync / reconnect / conflict flows. Not started.
