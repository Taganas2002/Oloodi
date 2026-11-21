#!/usr/bin/env node
/**
 * A. REST mutation latency — client start → HTTP response.
 * Warm-up discarded; reset between samples; valid transitions only.
 */
import {
  SARAH,
  environmentMeta,
  postAction,
  resetDemo,
  summarize,
  writeResults,
} from './stats.mjs'

const SAMPLES = Number(process.env.MEASURE_SAMPLES ?? 30)
const WARMUP = Number(process.env.MEASURE_WARMUP ?? 3)

async function measureAction(label, setup, action) {
  const samples = []
  for (let i = 0; i < WARMUP; i++) {
    await resetDemo()
    await setup()
    await postAction(SARAH.assignmentId, action)
  }
  for (let i = 0; i < SAMPLES; i++) {
    await resetDemo()
    await setup()
    const { ms } = await postAction(SARAH.assignmentId, action)
    samples.push(ms)
  }
  return { action: label, ...summarize(samples) }
}

async function main() {
  console.log(`REST latency: warmup=${WARMUP} samples=${SAMPLES}`)
  await resetDemo()

  const invite = await measureAction('invite', async () => {}, 'invite')
  const accept = await measureAction(
    'accept',
    async () => {
      await postAction(SARAH.assignmentId, 'invite')
    },
    'accept',
  )
  const checkIn = await measureAction(
    'check-in',
    async () => {
      await postAction(SARAH.assignmentId, 'invite')
      await postAction(SARAH.assignmentId, 'accept')
    },
    'check-in',
  )
  const checkOut = await measureAction(
    'check-out',
    async () => {
      await postAction(SARAH.assignmentId, 'invite')
      await postAction(SARAH.assignmentId, 'accept')
      await postAction(SARAH.assignmentId, 'check-in')
    },
    'check-out',
  )

  const rest = {
    method:
      'Client performance.now() around fetch POST; warm-up discarded; POST /api/dev/reset between samples; seed Sarah only; no invalid transitions.',
    warmup: WARMUP,
    samplesPerAction: SAMPLES,
    actions: { invite, accept, checkIn, checkOut },
  }

  const path = await writeResults({ environment: environmentMeta(), rest })
  console.log(JSON.stringify(rest.actions, null, 2))
  console.log(`Wrote ${path}`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
