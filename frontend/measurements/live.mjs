#!/usr/bin/env node
/**
 * B. SSE propagation + end-to-end live UI update latency.
 *
 * Labels (do not conflate):
 * - sseAfterHttpMs: EventSource message receipt − HTTP response end (mutation client)
 * - sseAfterOccurredAtMs: EventSource receipt − payload.occurredAt
 *   NOTE: occurredAt is stamped inside the TX before commit; AFTER_COMMIT publishes SSE.
 *   Treat sseAfterOccurredAtMs as approximate / slightly inflated vs true post-commit delay.
 * - uiUpdateMs: API mutation submit start → manager UI shows new participation (separate metric)
 */
import { chromium } from '@playwright/test'
import {
  API_URL,
  BASE_URL,
  SARAH,
  SHIFT_ID,
  environmentMeta,
  postAction,
  resetDemo,
  summarize,
  writeResults,
} from './stats.mjs'

const SAMPLES = Number(process.env.MEASURE_SAMPLES ?? 25)
const WARMUP = Number(process.env.MEASURE_WARMUP ?? 3)

async function measureSsePropagation(browser) {
  const sseAfterHttp = []
  const sseAfterOccurredAt = []
  const payloadBytes = []

  async function oneSample() {
    await resetDemo()
    const context = await browser.newContext()
    const page = await context.newPage()
    try {
      // Same browser clock for HTTP end + SSE receipt (avoids Node/browser skew).
      // Origin Vite :5173 so LocalCorsConfig allows EventSource → :8080.
      await page.goto(`${BASE_URL}/`)
      const result = await page.evaluate(
        async ({ apiUrl, shiftId, assignmentId }) => {
          const es = new EventSource(`${apiUrl}/api/shifts/${shiftId}/events`)
          await new Promise((resolve, reject) => {
            const t = setTimeout(() => reject(new Error('SSE open timeout')), 15_000)
            es.onopen = () => {
              clearTimeout(t)
              resolve()
            }
            es.onerror = () => {
              /* EventSource retries */
            }
          })

          const invitePromise = new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
              es.close()
              reject(new Error('SSE invite timeout'))
            }, 15_000)
            es.addEventListener('assignment-changed', (ev) => {
              const receiptPerf = performance.now()
              const receiptEpochMs = Date.now()
              let payload = null
              try {
                payload = JSON.parse(ev.data)
              } catch {
                /* ignore */
              }
              if (payload?.action !== 'invite') return
              clearTimeout(timer)
              es.close()
              resolve({
                receiptPerf,
                receiptEpochMs,
                payload,
                rawBytes: new TextEncoder().encode(ev.data ?? '').length,
              })
            })
          })

          const tHttp0 = performance.now()
          const res = await fetch(
            `${apiUrl}/api/shifts/${shiftId}/assignments/${assignmentId}/invite`,
            { method: 'POST', headers: { Accept: 'application/json' } },
          )
          const httpEndPerf = performance.now()
          const httpEndEpochMs = Date.now()
          if (!res.ok) {
            es.close()
            throw new Error(`invite failed ${res.status}`)
          }
          const sse = await invitePromise
          return {
            sseAfterHttpMs: sse.receiptPerf - httpEndPerf,
            httpDurationMs: httpEndPerf - tHttp0,
            httpEndEpochMs,
            receiptEpochMs: sse.receiptEpochMs,
            payload: sse.payload,
            rawBytes: sse.rawBytes,
          }
        },
        { apiUrl: API_URL, shiftId: SHIFT_ID, assignmentId: SARAH.assignmentId },
      )
      return result
    } finally {
      await context.close()
    }
  }

  for (let i = 0; i < WARMUP; i++) {
    await oneSample()
  }

  for (let i = 0; i < SAMPLES; i++) {
    const sample = await oneSample()
    sseAfterHttp.push(sample.sseAfterHttpMs)
    payloadBytes.push(sample.rawBytes)
    if (sample.payload?.occurredAt) {
      sseAfterOccurredAt.push(sample.receiptEpochMs - Date.parse(sample.payload.occurredAt))
    }
  }

  return {
    method:
      'Playwright Chromium: same-page EventSource + fetch invite (single performance.now clock); warm-up discarded; no fixed sleeps.',
    occurredAtCaveat:
      'occurredAt is Instant.now() inside the transactional mutate() before commit; SSE publish is AFTER_COMMIT. sseAfterOccurredAtMs is therefore pre-commit-biased (includes commit + listener), not a pure post-commit clock.',
    warmup: WARMUP,
    samples: SAMPLES,
    sseAfterHttpMs: summarize(sseAfterHttp),
    sseAfterOccurredAtMs: summarize(sseAfterOccurredAt),
    payloadBytes: summarize(payloadBytes),
  }
}

async function measureUiUpdate(browser) {
  const context = await browser.newContext()
  const page = await context.newPage()
  const samples = []
  const dashboardGets = []

  try {
    await page.goto(`${BASE_URL}/manager`)
    await page.getByRole('heading', { name: 'Manager dashboard' }).waitFor()
    await page.getByTestId('live-status').waitFor({ state: 'visible' })
    await page.waitForFunction(
      () =>
        document.querySelector('[data-testid="live-status"]')?.getAttribute('data-state') ===
        'LIVE',
      null,
      { timeout: 30_000 },
    )

    for (let i = 0; i < WARMUP + SAMPLES; i++) {
      await resetDemo()
      // Dev reset does not emit SSE — click Refresh for authoritative REST catch-up.
      await page.getByRole('button', { name: 'Refresh' }).click()
      await page
        .getByTestId(`worker-row-${SARAH.workerId}`)
        .getByTestId('participation-UNINVITED')
        .waitFor({ timeout: 15_000 })

      let gets = 0
      const onRes = (res) => {
        if (
          res.url().includes(`/api/shifts/${SHIFT_ID}/dashboard`) &&
          res.request().method() === 'GET'
        ) {
          gets += 1
        }
      }
      page.on('response', onRes)

      const t0 = performance.now()
      await postAction(SARAH.assignmentId, 'invite')
      await page
        .getByTestId(`worker-row-${SARAH.workerId}`)
        .getByTestId('participation-INVITED')
        .waitFor({ timeout: 15_000 })
      const t1 = performance.now()

      page.off('response', onRes)
      if (i >= WARMUP) {
        samples.push(t1 - t0)
        dashboardGets.push(gets)
      }
    }
  } finally {
    await context.close()
  }

  return {
    label: 'end-to-end live UI update (mutation submit → manager UI INVITED)',
    method:
      'Playwright manager page LIVE; API invite from Node; wait for participation-INVITED; no page.reload.',
    warmup: WARMUP,
    samples: SAMPLES,
    uiUpdateMs: summarize(samples),
    dashboardGetCountPerSample: summarize(dashboardGets),
    expectedRefetch:
      'Observer manager: 1 SSE event → 1 invalidate → ~1 dashboard GET (Phase 6 skips actor duplicate when LIVE).',
  }
}

async function main() {
  console.log(`Live/SSE: warmup=${WARMUP} samples=${SAMPLES}`)
  const browser = await chromium.launch({ headless: true })
  try {
    const sse = await measureSsePropagation(browser)
    console.log('SSE after HTTP median/p95:', sse.sseAfterHttpMs.medianMs, sse.sseAfterHttpMs.p95Ms)
    const ui = await measureUiUpdate(browser)
    console.log('UI update median/p95:', ui.uiUpdateMs.medianMs, ui.uiUpdateMs.p95Ms)

    const path = await writeResults({
      environment: environmentMeta(),
      ssePropagation: sse,
      liveUiUpdate: ui,
    })
    console.log(`Wrote ${path}`)
  } finally {
    await browser.close()
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
