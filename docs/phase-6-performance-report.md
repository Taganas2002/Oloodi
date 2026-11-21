---
cursor:
  subagentId: "bc-b9008e26-a743-5ae9-b69c-51b3fe2b886f"
---

# Phase 6 Performance Report — Oloodi Live Staffing Dashboard

## Summary

Local/demo-only latency evidence collected on this cloud VM (not production, not the user’s laptop). Scripts: `npm run measure` / `measure:rest` / `measure:live` / `measure:reconnect`. Raw numbers: `frontend/measurements/results.json` + `SUMMARY.md`.

| Suite | Result |
|-------|--------|
| Backend `mvn test` | **53 passed** |
| Frontend `npm test` | **24 passed** |
| Frontend `npm run build` | **SUCCESS** |
| Playwright E2E | **10 passed** |
| Measurements | REST 30×4 · SSE/UI 25 · reconnect 12/12 |

**Git:** `cursor/phase-6-performance-886f`. Pushing to GitHub is separate — code is not on the laptop yet.

---

## Environment

- Host: cloud-agent VM (linux), Node v22.23.4, OpenJDK 21
- API: `http://127.0.0.1:8080` with `SPRING_PROFILES_ACTIVE=local` (dev reset + CORS)
- UI: `http://127.0.0.1:5173` with `VITE_API_BASE_URL=http://127.0.0.1:8080`
- DB: local Postgres `live_staffing` (3 seeded assignments)
- Measured at: see `results.json` → `environment.measuredAt`
- **Claim boundary:** local/demo reproducibility only — no production SLA claims

---

## REST mutation latency (A)

Client `performance.now()` around `fetch` POST; warm-up 3 discarded; `POST /api/dev/reset` between samples; Sarah valid transitions only.

| Action | n | median | p95 | min | max |
|--------|---|--------|-----|-----|-----|
| invite | 30 | **6.13 ms** | 10.55 | 3.81 | 11.17 |
| accept | 30 | **5.16 ms** | 10.27 | 3.49 | 15.52 |
| check-in | 30 | **6.39 ms** | 16.67 | 3.06 | 19.08 |
| check-out | 30 | **3.46 ms** | 7.13 | 2.79 | 8.86 |

---

## SSE propagation (B)

Same Chromium page: EventSource open → `fetch` invite → measure with one `performance.now()` clock.

| Metric | median | p95 | note |
|--------|--------|-----|------|
| SSE after HTTP settle | **−0.6 ms** | −0.32 | Negative = SSE handled before `fetch()` body settle on localhost |
| SSE after `occurredAt` | **2 ms** | 5 | **Pre-commit-biased** — `occurredAt` stamped inside TX; publish is AFTER_COMMIT |
| Payload size | **193 bytes** | 193 | Notification-only JSON |

### Separate: end-to-end live UI update

Mutation submit (Node API) → manager UI shows INVITED (no reload).

| Metric | median | p95 |
|--------|--------|-----|
| UI update | **30.51 ms** | 32.67 |
| Dashboard GETs / sample | **1** | 1 |

---

## Reconnect recovery (C)

12/12 successful (≥10 required). Force disconnect (DEV hook + abort `/events`) → mutate offline → unroute + SPA remount → LIVE → ACCEPTED. No manual refresh.

| Metric | median | p95 | n |
|--------|--------|-----|---|
| Restored → LIVE | **142 ms** | 155 | 12 |
| Restored → correct UI | **145 ms** | 159 | 12 |

---

## Method

1. **REST:** Node `fetch` + `performance.now`; reset between samples.
2. **SSE:** Playwright Chromium EventSource + same-page fetch (shared clock).
3. **UI E2E:** Manager LIVE; API invite; wait `participation-INVITED`; count dashboard GETs.
4. **Reconnect:** Phase 5-compatible DEV disconnect; SPA remount (not `page.reload` for assertion).
5. Warm-ups discarded; no arbitrary sleeps; wait on real open/events/UI.

---

## Commands

```bash
# Backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# Frontend
cd frontend && VITE_API_BASE_URL=http://127.0.0.1:8080 npm run dev

# Measure
cd frontend && npm run measure
# or: npm run measure:rest | measure:live | measure:reconnect
```

Outputs: `frontend/measurements/results.json`, `SUMMARY.md`.

---

## Efficiency review

| Topic | Finding | Action |
|-------|---------|--------|
| Dashboard N+1 | `getDashboard` ≈ **2 queries** (`findById` + `@EntityGraph` assignments+worker+shift) | None — already correct for tiny dataset |
| Duplicate refetch | Actor used to invalidate on success **and** on SSE | **Fixed:** skip success invalidate when `liveState === 'LIVE'`; measured observer GETs = **1** |
| SSE payload | ~193 B notification; no dashboard dump | Keep |
| Heartbeat | Comment-only ~20s; no refetch | Keep |
| Notify | One AFTER_COMMIT publish per mutation | Keep |
| Redis/Kafka/WS/cache/indexes | Not needed | Not introduced |

---

## Optimizations

Only the smallest justified fix: **LIVE-path skip of mutation success invalidate** to avoid duplicate dashboard GET. No other perf refactors.

---

## Regression

- BE 53 / FE 24 / build OK / Playwright 10 — all green after Phase 6 changes.
- No JWT/WS/Redis/Kafka/polling/business-rule changes beyond the refetch skip + measurement scripts.

---

## Limitations

- Localhost VM numbers; laptop/CI/prod will differ.
- `occurredAt`-based SSE delay is pre-commit-biased (documented).
- Negative SSE-after-HTTP is honest localhost race (SSE vs HTTP body completion).
- Reconnect path uses DEV `__oloodiForceSseDisconnect` (same as Phase 5 E2E).
- Dev reset does not emit SSE — measurement loops Refresh before timing windows when needed.
- Serial demo DB; not a load test.

---

## Files

| Path | Purpose |
|------|---------|
| `frontend/measurements/{rest,live,reconnect,run-all,stats}.mjs` | Measurement runners |
| `frontend/measurements/results.json` | Real numbers + environment |
| `frontend/measurements/SUMMARY.md` | Short human summary |
| `frontend/package.json` | `measure*` scripts |
| `frontend/src/hooks/useDashboard.ts` | LIVE skip duplicate invalidate |
| `ManagerPage.tsx` / `WorkerPage.tsx` | Pass `liveState` into mutation hook |
| `useDashboard.test.tsx` | +2 unit tests for LIVE/RECONNECTING invalidate |

Report copy: `/agent/live-staffing-dashboard/docs/phase-6-performance-report.md`

---

## Next phase (do not implement)

**Phase 7:** UI polish only. Not started.
