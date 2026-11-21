---
cursor:
  subagentId: "bc-1d5b61a1-1a88-5038-a8cd-af0bd224dd31"
---

# Phase 8 Submission Report — Oloodi Live Staffing Dashboard

## Final verdict

**READY WITH LIMITATIONS**

The challenge scope (live staffing lifecycle, SSE + authoritative REST, tests, Docker demo, docs, interview pack) is implemented and **executed** on this VM. Limitations below are honest (no auth, single-node SSE, nested-Docker networking workaround, no GitHub remote on this run).

---

## Architecture

Browser REST mutation → Postgres commit → `@TransactionalEventListener(AFTER_COMMIT)` → SSE `assignment-changed` → TanStack Query invalidate → `GET /dashboard`. SSE is notification-only. See root `README.md` Mermaid and `docs/architecture.md`.

## Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot 3.3, JPA, Flyway, PostgreSQL |
| Frontend | React 19, Vite, TypeScript, TanStack Query |
| Live | SSE (`EventSource`), no WebSockets |
| Tests | JUnit/Testcontainers, Vitest, Playwright |
| Package | Docker Compose (postgres + backend + nginx UI) |

## Test numbers (executed)

| Suite | Result |
|-------|--------|
| `mvn test` | **53 passed** |
| `npm test` | **29 passed** |
| `npm run build` | **SUCCESS** |
| Playwright `npm run test:e2e` | **10 passed** (Vite DEV + Docker API) |

## E2E

Scenarios 1–13 covered: Sarah live lifecycle (no refresh), persistence reload, Daniel cancel→accept, Alex decline+409, illegal API 409, dup check-in/out timestamps, dual Sarah contexts, counts, reconnect catch-up, `/api/dev/reset`.

## Performance

Phase 6 VM numbers retained in `frontend/measurements/results.json` (not remeasured after Phase 8 packaging/CORS-only change):

- REST median invite/accept/check-in/check-out ≈ **6.1 / 5.2 / 6.4 / 3.5 ms** (n=30)
- Live UI update median ≈ **30.5 ms**; reconnect → LIVE ≈ **142 ms** (n=12)
- **Local/demo only — not production SLAs**

## Docker

Verified: `docker compose down -v && docker compose up --build` → all **healthy**.

- UI http://localhost:3000 · API http://localhost:8080 · Postgres host **5433**
- Uses `host.docker.internal` because this VM’s Docker bridge blocks container↔container TCP
- Demo CORS includes `:3000` and `:5173` (fixed browser POST 403 through nginx)

## Reset

- `POST /api/dev/reset` on profiles `local|demo|test` only
- `./scripts/reset-demo.sh` verified against Compose backend

## Live / reconnect

- Docker UI smoke: Manager+Worker LIVE → invite/accept/check-in **without refresh** (screenshots under media/phase-8)
- Playwright reconnect catch-up **passed** (DEV disconnect hook on Vite)

## Retry / idempotency

Duplicate check-in preserves `checkedInAt` (API acceptance + E2E). Illegal transitions → 409 ProblemDetail.

## Counts

Derived calculator; acceptance board filled=2, remaining=1, checkedIn=1, checkedOut=1 verified. Dashboard ≈ 2 SQL via EntityGraph (no N+1).

## Security hygiene

- No JWT (intentional); no real secrets committed
- `.env.example` placeholders; `.gitignore` excludes `.env`, `target/`, `node_modules/`, Playwright artifacts
- Dev reset **not** on default/prod profile
- Stack traces excluded from API error bodies

## Docs

| Path | Status |
|------|--------|
| `README.md` | Final — overview, arch, API, tests, perf, Docker, limits |
| `docs/AI_USAGE.md` | Cursor ownership + human decisions |
| `docs/DEMO_SCRIPT.md` | 5–7 min |
| `docs/INTERVIEW_QA.md` | Implementation-matched Q&A |
| `docs/architecture.md` | Short companion |

## AI usage

Built with **Cursor Cloud Agents**; human decisions: SSE not WS, no JWT, derived counts, natural idempotency, DECLINED final, profile-gated reset. Details: `docs/AI_USAGE.md`.

## Limitations

1. No authentication / authorization  
2. Single-node in-memory SSE registry  
3. Compose networking via host-gateway (nested Docker bridge limitation on this VM)  
4. Open demo surface — not production-hardened  
5. **No GitHub remote** on this agent run — code remains **VM-only** until the user pushes  

## Files (Phase 8)

`docker-compose.yml`, `Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`, `.env.example`, `.dockerignore`, `scripts/reset-demo.sh`, docs above, README rewrite, CORS `:3000` allowlist.

## Commands

```bash
docker compose down -v && docker compose up --build
./scripts/reset-demo.sh
mvn test
cd frontend && npm test && npm run build && npm run test:e2e
```

## Manager / Worker URLs

| Mode | Manager | Worker |
|------|---------|--------|
| Docker | http://localhost:3000/manager | http://localhost:3000/worker |
| Dev | http://localhost:5173/manager | http://localhost:5173/worker |

## Demo sequence

Reset → Sarah invite/accept/check-in/out (live) → Daniel cancel/accept again → Alex decline → show counts/progress → mention reconnect/E2E. Full: `docs/DEMO_SCRIPT.md`.

## Interview Qs

See `docs/INTERVIEW_QA.md` (SSE vs WS, AFTER_COMMIT, idempotency, counts, Docker, prod next steps).

## Blockers

- **None for challenge demo** on this VM  
- External: no `origin` remote — user must push for GitHub submission  
- Reviewers on normal Docker Desktop may still use the same Compose file; host-gateway pattern is portable  

## Media

`/cursor/stores/self/media/phase-8/docker-manager-live.png`, `docker-worker-390-live.png`

Report copy: `/agent/live-staffing-dashboard/docs/phase-8-submission-report.md`
