# Live Staffing Frontend (Phase 2)

React + TypeScript + Vite + React Router + TanStack Query UI for the Phase 1 REST API.

## Scripts

```bash
npm install
npm run dev      # :5173 with /api → localhost:8080 proxy
npm test
npm run build
```

## Routes

- `/` — landing links to manager/worker
- `/manager` — shift details, counts, invite when UNINVITED
- `/worker` — pick seeded worker, legal actions only

No SSE, WebSockets, JWT, or auth.
