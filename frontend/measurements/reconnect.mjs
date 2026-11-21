#!/usr/bin/env node
/**
 * C. Reconnect recovery — connectivity restored → EventSource LIVE → REST refetch → correct UI.
 * No manual refresh. Uses DEV __oloodiForceSseDisconnect (same as Phase 5 E2E).
 */
import { chromium } from '@playwright/test'
import {
  BASE_URL,
  SARAH,
  environmentMeta,
  postAction,
  resetDemo,
  summarize,
  writeResults,
} from './stats.mjs'

const RUNS = Number(process.env.MEASURE_RECONNECT_RUNS ?? 12)
const WARMUP = Number(process.env.MEASURE_WARMUP ?? 2)

async function oneRun(page) {
  await resetDemo()
  // Dev reset has no SSE — Refresh so UI matches seed before disconnect.
  await page.getByRole('button', { name: 'Refresh' }).click()
  await page
    .getByTestId(`worker-row-${SARAH.workerId}`)
    .getByTestId('participation-UNINVITED')
    .waitFor({ timeout: 15_000 })
  await page.waitForFunction(
    () => document.querySelector('[data-testid="live-status"]')?.getAttribute('data-state') === 'LIVE',
    null,
    { timeout: 30_000 },
  )

  // Drop open EventSource + block new /events until we restore.
  await page.route('**/events', (route) => route.abort())
  await page.evaluate(() => {
    const w = window
    w.__oloodiForceSseDisconnect?.()
  })
  await page.waitForFunction(
    () =>
      document.querySelector('[data-testid="live-status"]')?.getAttribute('data-state') ===
      'RECONNECTING',
    null,
    { timeout: 10_000 },
  )

  // Mutate while disconnected — manager must stay UNINVITED until catch-up.
  await postAction(SARAH.assignmentId, 'invite')
  await postAction(SARAH.assignmentId, 'accept')
  await page
    .getByTestId(`worker-row-${SARAH.workerId}`)
    .getByTestId('participation-UNINVITED')
    .waitFor({ timeout: 3_000 })

  const t0 = performance.now()
  await page.unroute('**/events')
  // Remount SSE via SPA nav (same as Phase 5) — no hard reload.
  await page.goto(`${BASE_URL}/`)
  await page.goto(`${BASE_URL}/manager`)
  await page.getByRole('heading', { name: 'Manager dashboard' }).waitFor()

  await page.waitForFunction(
    () => document.querySelector('[data-testid="live-status"]')?.getAttribute('data-state') === 'LIVE',
    null,
    { timeout: 30_000 },
  )
  const tLive = performance.now()

  await page
    .getByTestId(`worker-row-${SARAH.workerId}`)
    .getByTestId('participation-ACCEPTED')
    .waitFor({ timeout: 15_000 })
  const tUi = performance.now()

  return {
    toLiveMs: tLive - t0,
    toCorrectUiMs: tUi - t0,
  }
}

async function main() {
  console.log(`Reconnect recovery: warmup=${WARMUP} runs=${RUNS}`)
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext()
  const page = await context.newPage()

  const toLive = []
  const toUi = []
  let failures = 0

  try {
    await page.goto(`${BASE_URL}/manager`)
    await page.getByRole('heading', { name: 'Manager dashboard' }).waitFor()

    for (let i = 0; i < WARMUP + RUNS; i++) {
      try {
        const result = await oneRun(page)
        if (i >= WARMUP) {
          toLive.push(result.toLiveMs)
          toUi.push(result.toCorrectUiMs)
          console.log(
            ` run ${i - WARMUP + 1}/${RUNS}: LIVE ${result.toLiveMs.toFixed(0)}ms UI ${result.toCorrectUiMs.toFixed(0)}ms`,
          )
        }
      } catch (err) {
        failures += 1
        console.error(` run failed:`, err.message ?? err)
        // recover page for next attempt
        try {
          await page.unroute('**/events')
        } catch {
          /* ignore */
        }
        await page.goto(`${BASE_URL}/manager`)
      }
    }
  } finally {
    await browser.close()
  }

  if (toLive.length < 10) {
    throw new Error(`Need ≥10 successful reconnect runs; got ${toLive.length} (failures=${failures})`)
  }

  const reconnect = {
    method:
      'Playwright: force SSE disconnect (DEV hook + abort /events), mutate invite+accept offline, unroute + SPA remount, wait LIVE then ACCEPTED. No manual refresh / no page.reload for assertion.',
    warmup: WARMUP,
    requestedRuns: RUNS,
    successfulRuns: toLive.length,
    failures,
    connectivityRestoredToLiveMs: summarize(toLive),
    connectivityRestoredToCorrectUiMs: summarize(toUi),
  }

  const path = await writeResults({ environment: environmentMeta(), reconnect })
  console.log(JSON.stringify(reconnect, null, 2))
  console.log(`Wrote ${path}`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
