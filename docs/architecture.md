# Architecture notes

Companion to the root README — kept short on purpose.

## Packages (backend)

```
com.oloodi.livestaffing
  assignment/   domain, transitions, actions, REST
  shift/        dashboard query + counts
  worker/       worker entity
  realtime/     SSE registry, AFTER_COMMIT listener, heartbeat
  common/       Clock, ProblemDetail handler
  dev/          reset + local CORS (profile-gated)
```

## Live update sequence

1. Client `POST …/invite` (etc.)
2. `AssignmentActionService` loads assignment (EntityGraph), applies `AssignmentTransitionService`, saves in a TX
3. Application event published in-TX
4. After commit → `ShiftSseService.publish` to per-shift emitters
5. Browser `EventSource` receives `assignment-changed`
6. `useShiftLiveUpdates` → `invalidateQueries(['dashboard', shiftId])`
7. `GET /dashboard` redraws Manager/Worker

## Frontend

- Routes: `/` landing, `/manager`, `/worker`
- `useDashboard` — TanStack Query; no polling
- `useAssignmentAction(shiftId, liveState)` — LIVE skips success invalidate
- Toasts for mutation success + reconnect restore (UX only)

## Deployment shapes

| Mode | How |
|------|-----|
| Docker Compose | nginx:3000 → static + `/api` proxy → backend:8080 → postgres |
| Dev | Vite:5173 (proxy or `VITE_API_BASE_URL`) + `mvn spring-boot:run` |

## Out of scope here

Multi-region, message bus, auth, mobile apps, multi-shift admin, payroll.
