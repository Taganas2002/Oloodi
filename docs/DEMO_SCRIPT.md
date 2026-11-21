# Demo script (5–7 minutes)

**Prep:** `docker compose up --build` (or local BE `local` + FE). Open two windows: **Manager** and **Worker**. Run `./scripts/reset-demo.sh` once.

---

### 0:00–0:45 — Frame the problem (45s)

> “One shift, three workers. Manager staffs live; workers respond on a phone-like portal. REST is source of truth; SSE only notifies so the UI refetches.”

Point at Live indicator (● Live). Mention no auth by design for the challenge.

---

### 0:45–2:30 — Sarah live lifecycle (no refresh) (~1:45)

1. Worker select **Sarah Johnson**.
2. Manager **Send Invitation** for Sarah.
3. Worker shows INVITED → **Accept Shift** → **Check in** → **Check out**.
4. Keep Manager visible the whole time — statuses and counts move without Refresh.

Say: “Each mutation commits, then AFTER_COMMIT SSE, then GET dashboard.”

---

### 2:30–3:30 — Daniel cancel → accept again (~1:00)

1. Invite Daniel → Accept → **Cancel Shift** → **Accept Shift Again**.
2. Note CANCELLED is recoverable; contrast with decline.

---

### 3:30–4:15 — Alex decline is final (~0:45)

1. Invite Alex → **Decline Shift**.
2. Optional: show API `POST …/accept` → **409** (or skip if time-tight and mention E2E covers it).

---

### 4:15–5:00 — Counts + progress (~0:45)

Target a clear board, e.g. Sarah CHECKED_IN, Daniel CHECKED_OUT, Alex DECLINED:

- filled **2**, remaining **1**, checked in **1**, checked out **1**
- Progress bar **2/3** from backend counts only

---

### 5:00–6:00 — Reconnect catch-up (~1:00)

If time: briefly explain E2E/DEV disconnect → mutate while Reconnecting → restore → LIVE + correct state without manual Refresh.  
Or: toggle network off/on in DevTools if reliable in the room.

---

### 6:00–6:30 — Close (~0:30)

> “Idempotent check-in/out keep first timestamps; illegal transitions 409; Docker one-command demo; tests: 53 BE / 29 FE / 10 Playwright.”

Offer README + `INTERVIEW_QA.md` for deep dives.

---

## Timing tips

- Reset before the demo; don’t invent data mid-talk.
- Prefer UI over Postman unless showing 409.
- If SSE flakes on venue Wi‑Fi, fall back to Refresh once and explain catch-up design — honesty > fake live.
