/** Shared stats helpers for Phase 6 local/demo measurements. */

export const API_URL = process.env.E2E_API_URL ?? 'http://127.0.0.1:8080'
export const BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173'
export const SHIFT_ID = '11111111-1111-1111-1111-111111111111'
export const SARAH = {
  assignmentId: '33333333-3333-3333-3333-333333333301',
  workerId: '22222222-2222-2222-2222-222222222201',
  name: 'Sarah Johnson',
}

export function percentile(sorted, p) {
  if (sorted.length === 0) return null
  if (sorted.length === 1) return sorted[0]
  const idx = (p / 100) * (sorted.length - 1)
  const lo = Math.floor(idx)
  const hi = Math.ceil(idx)
  if (lo === hi) return sorted[lo]
  const w = idx - lo
  return sorted[lo] * (1 - w) + sorted[hi] * w
}

export function summarize(samples) {
  const sorted = [...samples].sort((a, b) => a - b)
  const sum = sorted.reduce((a, b) => a + b, 0)
  return {
    n: sorted.length,
    minMs: round(sorted[0]),
    maxMs: round(sorted[sorted.length - 1]),
    medianMs: round(percentile(sorted, 50)),
    p95Ms: round(percentile(sorted, 95)),
    meanMs: round(sum / sorted.length),
    samplesMs: sorted.map(round),
  }
}

export function round(n) {
  return Math.round(n * 100) / 100
}

export async function resetDemo() {
  const res = await fetch(`${API_URL}/api/dev/reset`, { method: 'POST' })
  if (!res.ok) {
    throw new Error(`dev reset failed: ${res.status} ${await res.text()}`)
  }
}

export async function postAction(assignmentId, action) {
  const t0 = performance.now()
  const res = await fetch(
    `${API_URL}/api/shifts/${SHIFT_ID}/assignments/${assignmentId}/${action}`,
    { method: 'POST', headers: { Accept: 'application/json' } },
  )
  const t1 = performance.now()
  const text = await res.text()
  let body = null
  try {
    body = text ? JSON.parse(text) : null
  } catch {
    body = text
  }
  if (!res.ok) {
    throw new Error(`${action} failed: ${res.status} ${text}`)
  }
  return { ms: t1 - t0, status: res.status, body, httpEndEpochMs: Date.now() }
}

export async function getDashboard() {
  const res = await fetch(`${API_URL}/api/shifts/${SHIFT_ID}/dashboard`, {
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) throw new Error(`dashboard ${res.status}`)
  return res.json()
}

export function environmentMeta() {
  return {
    measuredAt: new Date().toISOString(),
    host: 'cloud-agent-vm',
    apiUrl: API_URL,
    baseUrl: BASE_URL,
    note: 'Local/demo only — not production. Values from this VM, not a laptop.',
    node: process.version,
    platform: process.platform,
  }
}

export async function writeResults(partial) {
  const { writeFile, mkdir, readFile } = await import('node:fs/promises')
  const { dirname, join } = await import('node:path')
  const { fileURLToPath } = await import('node:url')
  const dir = dirname(fileURLToPath(import.meta.url))
  const path = join(dir, 'results.json')
  let existing = {}
  try {
    existing = JSON.parse(await readFile(path, 'utf8'))
  } catch {
    // first write
  }
  const next = {
    ...existing,
    ...partial,
    environment: {
      ...(existing.environment ?? {}),
      ...(partial.environment ?? environmentMeta()),
    },
  }
  await mkdir(dir, { recursive: true })
  await writeFile(path, JSON.stringify(next, null, 2) + '\n')
  return path
}
