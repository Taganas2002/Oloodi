# Phase 6 measurement summary (local/demo VM)

Measured at 2026-11-21T14:19:54.174Z on cloud-agent-vm.
**Not production. Code/results live on this cloud VM until pushed to GitHub — not on your laptop yet.**

## REST mutation (median / p95 ms)

| Action | n | median | p95 | min | max |
|--------|---|--------|-----|-----|-----|
| invite | 30 | 6.13 | 10.55 | 3.81 | 11.17 |
| accept | 30 | 5.16 | 10.27 | 3.49 | 15.52 |
| check-in | 30 | 6.39 | 16.67 | 3.06 | 19.08 |
| check-out | 30 | 3.46 | 7.13 | 2.79 | 8.86 |

## SSE propagation (median / p95 ms)

- After HTTP response settle: -0.6 / -0.32 (negative ⇒ SSE beat fetch body completion; see results.json note)
- After occurredAt (pre-commit-biased): 2 / 5
- Payload bytes (median): 193

## Live UI update (mutation → manager INVITED)

- median / p95: 30.51 / 32.67
- dashboard GETs per sample (median): 1

## Reconnect recovery (≥10 runs)

- successful: 12 (failures 0)
- restored → LIVE median/p95: 142.12 / 154.79
- restored → correct UI median/p95: 144.64 / 159.23

## Commands

```bash
npm run measure           # rest + live + reconnect
npm run measure:rest
npm run measure:live
npm run measure:reconnect
```

Requires BE `SPRING_PROFILES_ACTIVE=local` on :8080 and FE with `VITE_API_BASE_URL=http://127.0.0.1:8080` on :5173.

Raw: `measurements/results.json`
