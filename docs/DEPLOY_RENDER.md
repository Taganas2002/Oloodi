# Free interview demo on Render

Short-lived **public** demo only — not production. Preferred shape on the free tier:

| Piece | Render type | Why |
|-------|-------------|-----|
| API | Free **Web Service** (Docker → root `Dockerfile`) | Spring Boot + SSE + Flyway |
| UI | Free **Static Site** (`frontend/`) | Manager `/manager`, Worker `/worker` |
| DB | Free **Postgres** | Assignments persist across API cold starts |

Same-origin nginx (Compose’s `frontend/Dockerfile`) is **not** used on Render: that image proxies to `host.docker.internal:8080`, which does not exist there. Static Site + CORS is the minimal change.

Repo helpers: root [`render.yaml`](../render.yaml) (Blueprint) and this doc. Merge the branch that adds `server.port=${PORT:8080}` and `APP_CORS_ALLOWED_ORIGIN` before deploy.

---

## Option A — Blueprint (fewest clicks once branch is on GitHub)

1. Open [https://dashboard.render.com](https://dashboard.render.com) and sign in.
2. **New → Blueprint**.
3. Connect GitHub repo **`Taganas2002/Oloodi`**, branch with `render.yaml` (e.g. `cursor/render-free-deploy-4ab1` or `main` after merge).
4. Apply the Blueprint. When prompted (`sync: false`):
   - **`DB_URL`**: from the Postgres **Info** panel, take **Internal Database URL** pieces and set  
     `jdbc:postgresql://<internal-hostname>:5432/<database>`  
     (use `postgresql` in the JDBC scheme, not `postgres`).  
     `DB_USER` / `DB_PASSWORD` are wired automatically from the Blueprint.
   - **`VITE_API_BASE_URL`**: leave blank on first pass **or** set after the API service exists (see step 6).
   - **`APP_CORS_ALLOWED_ORIGIN`**: leave blank on first pass.
5. Wait until **Postgres** is available and **oloodi-demo-api** finishes its first Docker build (Maven — often several minutes).
6. Copy public URLs from each service’s page:
   - API → `https://oloodi-demo-api.onrender.com` (name may differ if Render suffixes it)
   - Web → `https://oloodi-demo-web.onrender.com`
7. **oloodi-demo-web → Environment →** set build env  
   `VITE_API_BASE_URL=https://<api-host>` (no trailing slash) → **Save** → **Manual Deploy**.
8. **oloodi-demo-api → Environment →** set  
   `APP_CORS_ALLOWED_ORIGIN=https://<web-host>` (exact origin, no path) → **Save** → **Manual Deploy**.
9. Open `https://<web-host>/manager` and `https://<web-host>/worker`.  
   First request after idle may show Render’s wake page (~1 minute).

---

## Option B — Manual click path (no Blueprint)

### 1. Postgres

1. **New → PostgreSQL** → plan **Free** → region you will reuse for the API.
2. Create. Note **Internal Database URL** / hostname, database name, user, password.  
   Do **not** commit these.

### 2. API Web Service

1. **New → Web Service** → connect **`Taganas2002/Oloodi`** → branch with PORT/CORS deploy commits.
2. Runtime **Docker**, Dockerfile path `/Dockerfile`, context `/`, plan **Free**.
3. Health check path:  
   `/api/shifts/11111111-1111-1111-1111-111111111111/dashboard`
4. Environment:

| Key | Value |
|-----|--------|
| `SPRING_PROFILES_ACTIVE` | `demo` |
| `DB_URL` | `jdbc:postgresql://<internal-host>:5432/<database-name>` |
| `DB_USER` | Render DB user |
| `DB_PASSWORD` | Render DB password |
| `APP_CORS_ALLOWED_ORIGIN` | *(set after Static Site exists)* exact `https://….onrender.com` |

Render injects `PORT`; the app binds with `server.port=${PORT:8080}`. Flyway seeds the demo shift on first boot.

### 3. Static Site

1. **New → Static Site** → same repo/branch.
2. Root Directory `frontend`, Build `npm ci && npm run build`, Publish `dist`.
3. Build env: `VITE_API_BASE_URL=https://<api-public-host>` (no trailing slash).  
   Vite bakes this into the bundle — change ⇒ rebuild.
4. Rewrite: Source `/*` → Destination `/index.html` → **Rewrite**.
5. Copy the Static Site URL into the API’s `APP_CORS_ALLOWED_ORIGIN` and redeploy the API.

---

## SSE / proxy notes

| Topic | Behavior on this setup |
|-------|-------------------------|
| EventSource URL | `${VITE_API_BASE_URL}/api/shifts/{id}/events` (cross-origin to API) |
| CORS | Demo profile allows localhost patterns **plus** `APP_CORS_ALLOWED_ORIGIN` |
| Heartbeats | Comment-only ~20s while API is **awake** |
| Free spin-down | After ~**15 minutes** with **no inbound HTTP or WebSocket messages**, Free web services sleep. **Open SSE alone does not count** as keep-alive (SSE is not WebSocket). |
| After sleep | EventSource errors until the API wakes (~1 min on next HTTP). Client auto-reconnects; invalidate-on-open still catch-up once LIVE. |
| Compose nginx | Local Docker only (`proxy_buffering off`, long read timeouts). Not used on Render Static Site. |

Before a live interview: open Manager + Worker once, wait for wake, optionally hit **Reset demo**, then start the script.

---

## Demo reset & security

Manager → **Reset demo** restores the seeded shift/assignments (synthetic data only).  
`POST /api/dev/reset` is enabled on the `demo` profile. **No auth** — anyone with the URLs can mutate and reset. Do not attach real people or production DBs.

---

## Free-tier limits (honest)

- **750 Free instance hours / workspace / month** (running time only; spun-down does not burn hours).
- **Cold start ~1 minute** after spin-down.
- **Spin-down ~15 min** idle → live SSE breaks until wake.
- **Free Postgres**: 1 GB, **expires 30 days** after creation (+14 day upgrade grace), no backups, one free DB per workspace.
- Bandwidth / build pipeline minutes count against workspace included usage.

Source: [Render free instance docs](https://render.com/docs/free).

---

## URLs (fill after deploy)

| Role | URL |
|------|-----|
| Manager | `https://<web>/manager` |
| Worker | `https://<web>/worker` |
| API | `https://<api>/` |

*(Not deployed from this agent — requires your Render login.)*

## Blockers this agent cannot clear

1. Render Dashboard login / Blueprint apply (needs your account).
2. Filling `sync: false` secrets and cross-linking the two public URLs.
3. GitHub branch merge of PORT + CORS + `render.yaml` if not yet on `main`.
