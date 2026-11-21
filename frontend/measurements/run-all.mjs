#!/usr/bin/env node
import { spawn } from 'node:child_process'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { writeFile } from 'node:fs/promises'
import { environmentMeta, writeResults } from './stats.mjs'

const dir = dirname(fileURLToPath(import.meta.url))

function run(script) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [join(dir, script)], {
      stdio: 'inherit',
      env: process.env,
    })
    child.on('exit', (code) => {
      if (code === 0) resolve()
      else reject(new Error(`${script} exited ${code}`))
    })
  })
}

async function main() {
  await run('rest.mjs')
  await run('live.mjs')
  await run('reconnect.mjs')

  const summaryPath = join(dir, 'SUMMARY.md')
  // results.json already merged by each script; append a short human pointer
  const { readFile } = await import('node:fs/promises')
  const results = JSON.parse(await readFile(join(dir, 'results.json'), 'utf8'))
  const r = results.rest?.actions
  const s = results.ssePropagation
  const u = results.liveUiUpdate
  const c = results.reconnect

  const md = `# Phase 6 measurement summary (local/demo VM)

Measured at ${results.environment?.measuredAt ?? 'n/a'} on ${results.environment?.host ?? 'vm'}.
**Not production. Code/results live on this cloud VM until pushed.**

## REST mutation (median / p95 ms)

| Action | n | median | p95 | min | max |
|--------|---|--------|-----|-----|-----|
| invite | ${r?.invite?.n} | ${r?.invite?.medianMs} | ${r?.invite?.p95Ms} | ${r?.invite?.minMs} | ${r?.invite?.maxMs} |
| accept | ${r?.accept?.n} | ${r?.accept?.medianMs} | ${r?.accept?.p95Ms} | ${r?.accept?.minMs} | ${r?.accept?.maxMs} |
| check-in | ${r?.checkIn?.n} | ${r?.checkIn?.medianMs} | ${r?.checkIn?.p95Ms} | ${r?.checkIn?.minMs} | ${r?.checkIn?.maxMs} |
| check-out | ${r?.checkOut?.n} | ${r?.checkOut?.medianMs} | ${r?.checkOut?.p95Ms} | ${r?.checkOut?.minMs} | ${r?.checkOut?.maxMs} |

## SSE propagation (median / p95 ms)

- After HTTP response: ${s?.sseAfterHttpMs?.medianMs} / ${s?.sseAfterHttpMs?.p95Ms}
- After occurredAt (pre-commit-biased): ${s?.sseAfterOccurredAtMs?.medianMs} / ${s?.sseAfterOccurredAtMs?.p95Ms}
- Payload bytes (median): ${s?.payloadBytes?.medianMs} (field name medianMs = bytes in this block)

## Live UI update (mutation → manager INVITED)

- median / p95: ${u?.uiUpdateMs?.medianMs} / ${u?.uiUpdateMs?.p95Ms}
- dashboard GETs per sample (median): ${u?.dashboardGetCountPerSample?.medianMs}

## Reconnect recovery (≥10 runs)

- successful: ${c?.successfulRuns} (failures ${c?.failures})
- restored → LIVE median/p95: ${c?.connectivityRestoredToLiveMs?.medianMs} / ${c?.connectivityRestoredToLiveMs?.p95Ms}
- restored → correct UI median/p95: ${c?.connectivityRestoredToCorrectUiMs?.medianMs} / ${c?.connectivityRestoredToCorrectUiMs?.p95Ms}

Raw: \`measurements/results.json\`
`

  await writeFile(summaryPath, md)
  await writeResults({ environment: environmentMeta(), summaryGenerated: true })
  console.log(`All measurements complete. Summary: ${summaryPath}`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
