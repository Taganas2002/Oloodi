# AI usage — Oloodi Live Staffing Dashboard

## Ownership

This submission was built with **Cursor Cloud Agents** (Composer) under human direction. The human owns product assumptions, acceptance criteria, and final review. AI assisted with scaffolding, tests, docs, and iteration — it did not replace judgment on architecture trade-offs.

## Tools actually used

| Tool | Role |
|------|------|
| **Cursor** (Cloud Agent / Composer) | Implementation across Phases 1–8, tests, Docker, documentation |
| Local toolchain | Java 21 / Maven, Node 22 / npm, Playwright, Docker Compose, PostgreSQL, Testcontainers |

**Not used:** Copilot, ChatGPT web UI as the primary coder, Claude Projects outside Cursor, external codegen SaaS, or automated “agent swarms” beyond Cursor’s own subagents for this fork.

## Human decisions (kept even when AI could have “added more”)

| Decision | Rationale |
|----------|-----------|
| **SSE instead of WebSockets** | One-way notify + REST refetch is enough; simpler ops story for the challenge |
| **No JWT / auth** | Explicitly out of scope; keep the demo open |
| **Derived dashboard counts** | Avoid dual-write / drift vs assignment rows |
| **Natural idempotency** (no Idempotency-Key) | State machine returns current state when already at target |
| **DECLINED is FINAL** | Product assumption documented in Phase 1 |
| **Invite updates seed rows** | No insert-on-invite; stable UUIDs for E2E |
| **No Redis / Kafka / polling** | Unnecessary for single-node demo size |
| **Dev `/api/dev/reset` only on local/demo/test** | Never ship reset in production profiles |
| **UI polish without redesigning the domain** | Phase 7 = presentation only |

## What AI helped with

- Spring Boot domain, Flyway, transition service, ProblemDetail errors
- React Manager/Worker UI, TanStack Query + SSE hook
- Playwright multi-context E2E and measurement scripts
- Reliability hardening (AFTER_COMMIT, reconnect catch-up, 409 paths)
- Docker Compose packaging and Phase 8 documentation drafts

## What humans must still verify

- Challenge rubric vs this README
- That claimed test counts were actually executed on the submission environment
- That Docker Compose works on the reviewer’s machine
- Interview answers match **this** codebase (see `INTERVIEW_QA.md`)
