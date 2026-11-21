import { expect, type APIRequestContext, type Browser, type BrowserContext, type Page } from '@playwright/test'

export const SHIFT_ID = '11111111-1111-1111-1111-111111111111'
export const WORKERS = {
  sarah: {
    id: '22222222-2222-2222-2222-222222222201',
    name: 'Sarah Johnson',
    assignmentId: '33333333-3333-3333-3333-333333333301',
  },
  daniel: {
    id: '22222222-2222-2222-2222-222222222202',
    name: 'Daniel Martin',
    assignmentId: '33333333-3333-3333-3333-333333333302',
  },
  alex: {
    id: '22222222-2222-2222-2222-222222222203',
    name: 'Alex Smith',
    assignmentId: '33333333-3333-3333-3333-333333333303',
  },
} as const

export const API_URL = process.env.E2E_API_URL ?? 'http://127.0.0.1:8080'

export async function resetDemo(request: APIRequestContext) {
  const res = await request.post(`${API_URL}/api/dev/reset`)
  expect(res.ok(), `dev reset failed: ${res.status()} ${await res.text()}`).toBeTruthy()
}

export async function getDashboard(request: APIRequestContext) {
  const res = await request.get(`${API_URL}/api/shifts/${SHIFT_ID}/dashboard`)
  expect(res.ok()).toBeTruthy()
  return res.json()
}

export async function postAction(
  request: APIRequestContext,
  assignmentId: string,
  action: string,
) {
  return request.post(
    `${API_URL}/api/shifts/${SHIFT_ID}/assignments/${assignmentId}/${action}`,
  )
}

export async function openManager(browser: Browser): Promise<{ context: BrowserContext; page: Page }> {
  const context = await browser.newContext()
  const page = await context.newPage()
  attachGuards(page, 'manager')
  await page.goto('/manager')
  await expect(page.getByRole('heading', { name: 'Manager dashboard' })).toBeVisible()
  await expect(page.getByTestId('live-status')).toHaveAttribute('data-state', 'LIVE', {
    timeout: 30_000,
  })
  return { context, page }
}

export async function openWorker(
  browser: Browser,
  workerName: string,
): Promise<{ context: BrowserContext; page: Page }> {
  const context = await browser.newContext()
  const page = await context.newPage()
  attachGuards(page, `worker:${workerName}`)
  await page.goto('/worker')
  await expect(page.getByRole('heading', { name: 'Worker view' })).toBeVisible()
  await page.getByLabel('Worker').selectOption({ label: workerName })
  await expect(page.getByRole('heading', { name: workerName })).toBeVisible()
  await expect(page.getByTestId('live-status')).toHaveAttribute('data-state', 'LIVE', {
    timeout: 30_000,
  })
  return { context, page }
}

export async function waitForParticipation(page: Page, status: string) {
  await expect(page.getByTestId(`participation-${status}`)).toBeVisible()
}

export async function waitForAttendance(page: Page, status: string) {
  await expect(page.getByTestId(`attendance-${status}`)).toBeVisible()
}

export async function waitForManagerWorkerStatus(
  page: Page,
  workerId: string,
  participation: string,
  attendance: string,
) {
  const row = page.getByTestId(`worker-row-${workerId}`)
  await expect(row.getByTestId(`participation-${participation}`)).toBeVisible()
  await expect(row.getByTestId(`attendance-${attendance}`)).toBeVisible()
}

export async function expectCounts(
  page: Page,
  counts: { filled: number; remaining: number; checkedIn: number; checkedOut: number },
) {
  await expect(page.getByTestId('count-filled')).toHaveText(String(counts.filled))
  await expect(page.getByTestId('count-remaining')).toHaveText(String(counts.remaining))
  await expect(page.getByTestId('count-checkedIn')).toHaveText(String(counts.checkedIn))
  await expect(page.getByTestId('count-checkedOut')).toHaveText(String(counts.checkedOut))
}

function attachGuards(page: Page, label: string) {
  page.on('pageerror', (err) => {
    console.error(`[${label}] pageerror: ${err.message}`)
  })
  page.on('response', (res) => {
    // SSE EventSource may briefly fail/retry through the dev proxy — ignore /events 5xx.
    if (
      res.status() >= 500 &&
      res.url().includes('/api/') &&
      !res.url().includes('/events')
    ) {
      console.error(`[${label}] API 5xx ${res.status()} ${res.url()}`)
    }
  })
}
