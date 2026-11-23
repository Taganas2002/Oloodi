import { expect, test } from '@playwright/test'
import {
  API_URL,
  WORKERS,
  expectCounts,
  getDashboard,
  openManager,
  openWorker,
  postAction,
  resetDemo,
  waitForAttendance,
  waitForManagerWorkerStatus,
  waitForParticipation,
} from './fixtures'

test.describe.configure({ mode: 'serial' })

test.beforeEach(async ({ request }) => {
  await resetDemo(request)
})

test('1-4 live lifecycle Sarah: invite → accept → check-in → check-out (no refresh)', async ({
  browser,
  request,
}) => {
  const manager = await openManager(browser)
  const worker = await openWorker(browser, WORKERS.sarah.name)

  await manager.page.getByRole('button', { name: `Invite ${WORKERS.sarah.name}` }).click()
  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.sarah.id,
    'INVITED',
    'NOT_STARTED',
  )
  await waitForParticipation(worker.page, 'INVITED')

  await worker.page.getByTestId('action-accept').click()
  await waitForParticipation(worker.page, 'ACCEPTED')
  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.sarah.id,
    'ACCEPTED',
    'NOT_STARTED',
  )
  await expectCounts(manager.page, { filled: 1, remaining: 2, checkedIn: 0, checkedOut: 0 })

  await worker.page.getByTestId('action-check-in').click()
  await waitForAttendance(worker.page, 'CHECKED_IN')
  await expect(worker.page.getByTestId('checked-in-at')).not.toHaveText('—')
  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.sarah.id,
    'ACCEPTED',
    'CHECKED_IN',
  )
  await expectCounts(manager.page, { filled: 1, remaining: 2, checkedIn: 1, checkedOut: 0 })

  const checkedInText = await worker.page.getByTestId('checked-in-at').innerText()

  await worker.page.getByTestId('action-check-out').click()
  await waitForAttendance(worker.page, 'CHECKED_OUT')
  await expect(worker.page.getByTestId('checked-out-at')).not.toHaveText('—')
  await expect(worker.page.getByTestId('checked-in-at')).toHaveText(checkedInText)
  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.sarah.id,
    'ACCEPTED',
    'CHECKED_OUT',
  )
  await expectCounts(manager.page, { filled: 1, remaining: 2, checkedIn: 0, checkedOut: 1 })

  const dash = await getDashboard(request)
  const sarah = dash.assignments.find((a: { workerId: string }) => a.workerId === WORKERS.sarah.id)
  expect(sarah.participationStatus).toBe('ACCEPTED')
  expect(sarah.attendanceStatus).toBe('CHECKED_OUT')
  expect(sarah.checkedInAt).toBeTruthy()
  expect(sarah.checkedOutAt).toBeTruthy()
  expect(dash.counts).toEqual({ filled: 1, remaining: 2, checkedIn: 0, checkedOut: 1 })

  await manager.context.close()
  await worker.context.close()
})

test('5 refresh persistence keeps timestamps', async ({ browser, request }) => {
  const manager = await openManager(browser)
  const worker = await openWorker(browser, WORKERS.sarah.name)

  await manager.page.getByRole('button', { name: `Invite ${WORKERS.sarah.name}` }).click()
  await waitForParticipation(worker.page, 'INVITED')
  await worker.page.getByTestId('action-accept').click()
  await worker.page.getByTestId('action-check-in').click()
  await waitForAttendance(worker.page, 'CHECKED_IN')
  await worker.page.getByTestId('action-check-out').click()
  await waitForAttendance(worker.page, 'CHECKED_OUT')

  const before = await getDashboard(request)
  const sarahBefore = before.assignments.find(
    (a: { workerId: string }) => a.workerId === WORKERS.sarah.id,
  )

  // Persistence check — only place we intentionally reload.
  await worker.page.reload()
  await expect(worker.page.getByRole('heading', { name: WORKERS.sarah.name })).toBeVisible()
  await waitForAttendance(worker.page, 'CHECKED_OUT')

  const after = await getDashboard(request)
  const sarahAfter = after.assignments.find(
    (a: { workerId: string }) => a.workerId === WORKERS.sarah.id,
  )
  expect(sarahAfter.checkedInAt).toBe(sarahBefore.checkedInAt)
  expect(sarahAfter.checkedOutAt).toBe(sarahBefore.checkedOutAt)

  await manager.context.close()
  await worker.context.close()
})

test('6 Daniel cancel then accept again', async ({ browser, request }) => {
  const manager = await openManager(browser)
  const worker = await openWorker(browser, WORKERS.daniel.name)

  await manager.page.getByRole('button', { name: `Invite ${WORKERS.daniel.name}` }).click()
  await waitForParticipation(worker.page, 'INVITED')
  await worker.page.getByTestId('action-accept').click()
  await waitForParticipation(worker.page, 'ACCEPTED')
  await worker.page.getByTestId('action-cancel').click()
  await waitForParticipation(worker.page, 'CANCELLED')
  await worker.page.getByTestId('action-accept').click()
  await waitForParticipation(worker.page, 'ACCEPTED')
  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.daniel.id,
    'ACCEPTED',
    'NOT_STARTED',
  )

  const dash = await getDashboard(request)
  const daniel = dash.assignments.find((a: { workerId: string }) => a.workerId === WORKERS.daniel.id)
  expect(daniel.participationStatus).toBe('ACCEPTED')
  expect(daniel.attendanceStatus).toBe('NOT_STARTED')

  await manager.context.close()
  await worker.context.close()
})

test('7 Alex decline is final', async ({ browser, request }) => {
  const manager = await openManager(browser)
  const worker = await openWorker(browser, WORKERS.alex.name)

  await manager.page.getByRole('button', { name: `Invite ${WORKERS.alex.name}` }).click()
  await waitForParticipation(worker.page, 'INVITED')
  await worker.page.getByTestId('action-decline').click()
  await waitForParticipation(worker.page, 'DECLINED')
  await expect(worker.page.getByText('You declined this shift.')).toBeVisible()
  await expect(worker.page.getByTestId('action-accept')).toHaveCount(0)
  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.alex.id,
    'DECLINED',
    'NOT_STARTED',
  )

  const accept = await postAction(request, WORKERS.alex.assignmentId, 'accept')
  expect(accept.status()).toBe(409)

  await manager.context.close()
  await worker.context.close()
})

test('8 illegal transitions via API return 409', async ({ request }) => {
  // UNINVITED → accept
  let res = await postAction(request, WORKERS.sarah.assignmentId, 'accept')
  expect(res.status()).toBe(409)
  let body = await res.json()
  expect(body.status).toBe(409)
  expect(body.detail).toBeTruthy()

  // UNINVITED → check-in
  res = await postAction(request, WORKERS.sarah.assignmentId, 'check-in')
  expect(res.status()).toBe(409)

  await postAction(request, WORKERS.sarah.assignmentId, 'invite')
  await postAction(request, WORKERS.sarah.assignmentId, 'accept')
  await postAction(request, WORKERS.sarah.assignmentId, 'check-in')

  // CHECKED_IN → cancel
  res = await postAction(request, WORKERS.sarah.assignmentId, 'cancel')
  expect(res.status()).toBe(409)
  body = await res.json()
  expect(body.detail).toMatch(/cancel/i)
})

test('9-10 duplicate check-in/out preserve timestamps', async ({ request }) => {
  await postAction(request, WORKERS.sarah.assignmentId, 'invite')
  await postAction(request, WORKERS.sarah.assignmentId, 'accept')

  const in1 = await (await postAction(request, WORKERS.sarah.assignmentId, 'check-in')).json()
  const in2 = await (await postAction(request, WORKERS.sarah.assignmentId, 'check-in')).json()
  expect(in2.checkedInAt).toBe(in1.checkedInAt)
  expect(in2.attendanceStatus).toBe('CHECKED_IN')

  const out1 = await (await postAction(request, WORKERS.sarah.assignmentId, 'check-out')).json()
  const out2 = await (await postAction(request, WORKERS.sarah.assignmentId, 'check-out')).json()
  expect(out2.checkedOutAt).toBe(out1.checkedOutAt)
  expect(out2.checkedInAt).toBe(in1.checkedInAt)

  const dash = await getDashboard(request)
  expect(dash.counts.checkedOut).toBe(1)
  expect(dash.assignments).toHaveLength(3)
})

test('11 same Sarah in two worker contexts + manager sync', async ({ browser, request }) => {
  const manager = await openManager(browser)
  const workerB = await openWorker(browser, WORKERS.sarah.name)
  const workerC = await openWorker(browser, WORKERS.sarah.name)

  await manager.page.getByRole('button', { name: `Invite ${WORKERS.sarah.name}` }).click()
  await waitForParticipation(workerB.page, 'INVITED')
  await waitForParticipation(workerC.page, 'INVITED')

  await workerB.page.getByTestId('action-accept').click()
  await waitForParticipation(workerB.page, 'ACCEPTED')
  await waitForParticipation(workerC.page, 'ACCEPTED')
  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.sarah.id,
    'ACCEPTED',
    'NOT_STARTED',
  )

  await workerC.page.getByTestId('action-check-in').click()
  await waitForAttendance(workerC.page, 'CHECKED_IN')
  await waitForAttendance(workerB.page, 'CHECKED_IN')
  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.sarah.id,
    'ACCEPTED',
    'CHECKED_IN',
  )

  const dash = await getDashboard(request)
  expect(dash.counts.checkedIn).toBe(1)

  await manager.context.close()
  await workerB.context.close()
  await workerC.context.close()
})

test('12 counts match rows across workers', async ({ browser, request }) => {
  // Sarah CHECKED_IN, Daniel CHECKED_OUT, Alex DECLINED → filled 2, rem 1, in 1, out 1
  const manager = await openManager(browser)

  // Sarah → checked in
  await postAction(request, WORKERS.sarah.assignmentId, 'invite')
  await postAction(request, WORKERS.sarah.assignmentId, 'accept')
  await postAction(request, WORKERS.sarah.assignmentId, 'check-in')

  // Daniel → checked out
  await postAction(request, WORKERS.daniel.assignmentId, 'invite')
  await postAction(request, WORKERS.daniel.assignmentId, 'accept')
  await postAction(request, WORKERS.daniel.assignmentId, 'check-in')
  await postAction(request, WORKERS.daniel.assignmentId, 'check-out')

  // Alex → declined
  await postAction(request, WORKERS.alex.assignmentId, 'invite')
  await postAction(request, WORKERS.alex.assignmentId, 'decline')

  // Trigger live catch-up via SSE events already published — wait for manager UI
  await waitForManagerWorkerStatus(manager.page, WORKERS.sarah.id, 'ACCEPTED', 'CHECKED_IN')
  await waitForManagerWorkerStatus(manager.page, WORKERS.daniel.id, 'ACCEPTED', 'CHECKED_OUT')
  await waitForManagerWorkerStatus(manager.page, WORKERS.alex.id, 'DECLINED', 'NOT_STARTED')
  await expectCounts(manager.page, { filled: 2, remaining: 1, checkedIn: 1, checkedOut: 1 })

  const dash = await getDashboard(request)
  expect(dash.counts).toEqual({ filled: 2, remaining: 1, checkedIn: 1, checkedOut: 1 })

  await manager.context.close()
})

test('13 reconnect catch-up via aborted SSE then restore', async ({ browser, request }) => {
  const manager = await openManager(browser)
  const worker = await openWorker(browser, WORKERS.sarah.name)

  // Block new SSE + force-drop the open manager stream (DEV E2E hook).
  await manager.page.route('**/events', (route) => route.abort())
  await manager.page.evaluate(() => {
    const w = window as unknown as { __oloodiForceSseDisconnect?: () => void }
    w.__oloodiForceSseDisconnect?.()
  })
  await expect(manager.page.getByTestId('live-status')).toHaveAttribute(
    'data-state',
    'RECONNECTING',
    { timeout: 10_000 },
  )
  await expect(manager.page.getByTestId('live-unavailable-hint')).toBeVisible()

  // Mutations while manager SSE is down — worker still live
  await postAction(request, WORKERS.sarah.assignmentId, 'invite')
  await postAction(request, WORKERS.sarah.assignmentId, 'accept')
  await postAction(request, WORKERS.sarah.assignmentId, 'check-in')
  await waitForAttendance(worker.page, 'CHECKED_IN')

  // Manager UI should not have caught those events yet
  await expect(
    manager.page.getByTestId(`worker-row-${WORKERS.sarah.id}`).getByTestId('participation-UNINVITED'),
  ).toBeVisible()

  // Restore SSE and remount live hook (SPA nav — no full reload)
  await manager.page.unroute('**/events')
  await manager.page.getByRole('link', { name: 'Worker' }).click()
  await expect(manager.page.getByRole('heading', { name: 'Worker view' })).toBeVisible()
  await manager.page.getByRole('link', { name: 'Manager' }).click()
  await expect(manager.page.getByRole('heading', { name: 'Manager dashboard' })).toBeVisible()
  await expect(manager.page.getByTestId('live-status')).toHaveAttribute('data-state', 'LIVE', {
    timeout: 20_000,
  })

  await waitForManagerWorkerStatus(
    manager.page,
    WORKERS.sarah.id,
    'ACCEPTED',
    'CHECKED_IN',
  )
  await expectCounts(manager.page, { filled: 1, remaining: 2, checkedIn: 1, checkedOut: 0 })

  const dash = await getDashboard(request)
  expect(dash.counts.checkedIn).toBe(1)

  await manager.context.close()
  await worker.context.close()
})

test('demo reset refreshes both live dashboards and is repeatable', async ({ browser, request }) => {
  const manager = await openManager(browser)
  const worker = await openWorker(browser, WORKERS.sarah.name)
  for (const action of ['invite', 'accept', 'check-in', 'check-out']) {
    expect((await postAction(request, WORKERS.sarah.assignmentId, action)).ok()).toBeTruthy()
  }
  await waitForAttendance(worker.page, 'CHECKED_OUT')
  await expectCounts(manager.page, { filled: 1, remaining: 2, checkedIn: 0, checkedOut: 1 })

  const res = await request.post(`${API_URL}/api/dev/reset`)
  expect(res.ok()).toBeTruthy()
  const body = await res.json()
  expect(body.status).toBe('ok')
  expect(body.updatedAssignments).toBe(3)

  await waitForParticipation(worker.page, 'UNINVITED')
  await waitForAttendance(worker.page, 'NOT_STARTED')
  for (const seededWorker of Object.values(WORKERS)) {
    await waitForManagerWorkerStatus(manager.page, seededWorker.id, 'UNINVITED', 'NOT_STARTED')
  }
  await expectCounts(manager.page, { filled: 0, remaining: 3, checkedIn: 0, checkedOut: 0 })

  const restored = await getDashboard(request)
  expect(restored.shift.requiredWorkers).toBe(3)
  expect(restored.assignments).toHaveLength(3)
  for (const assignment of restored.assignments) {
    expect(assignment.checkedInAt).toBeNull()
    expect(assignment.checkedOutAt).toBeNull()
  }
  await resetDemo(request)
  expect(await getDashboard(request)).toEqual(restored)
  await manager.context.close()
  await worker.context.close()
})
