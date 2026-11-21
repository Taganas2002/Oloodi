import { useCallback, useEffect, useMemo, useState } from 'react'
import { DEMO_SHIFT_ID, DEMO_WORKERS } from '../api/demoIds'
import type { AssignmentAction } from '../api/types'
import { AppShell } from '../components/AppShell'
import { AttendanceBadge, ParticipationBadge } from '../components/StatusBadge'
import { ErrorBanner } from '../components/ErrorBanner'
import { LiveStatus } from '../components/LiveStatus'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { ToastStack } from '../components/Toast'
import { useAssignmentAction, useDashboard } from '../hooks/useDashboard'
import { useLiveRestoreToast } from '../hooks/useLiveRestoreToast'
import { useShiftLiveUpdates } from '../hooks/useShiftLiveUpdates'
import { useToasts } from '../hooks/useToasts'
import { ACTION_PENDING, ACTION_SUCCESS } from '../lib/actionCopy'
import { getWorkerActions, workerWaitingMessage } from '../lib/actions'
import { toUserMessage } from '../lib/errors'
import { formatLastSyncLabel } from '../lib/lastSync'
import {
  formatLocalDate,
  formatLocalDateTime,
  formatLocalTime,
} from '../lib/format'

export function WorkerPage() {
  const shiftId = DEMO_SHIFT_ID
  const liveState = useShiftLiveUpdates(shiftId)
  const [workerId, setWorkerId] = useState<string>(DEMO_WORKERS[0].id)
  const [banner, setBanner] = useState<string | null>(null)
  const [pendingAction, setPendingAction] = useState<AssignmentAction | null>(null)
  const { toasts, pushToast, dismissToast } = useToasts()
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 5_000)
    return () => window.clearInterval(id)
  }, [])

  const { data, isLoading, isError, error, refetch, isFetching, dataUpdatedAt } =
    useDashboard(shiftId)
  const mutation = useAssignmentAction(shiftId, liveState)

  const onRestore = useCallback(() => {
    pushToast('Live connection restored. Latest staffing data loaded.', 'info')
  }, [pushToast])
  useLiveRestoreToast(liveState, onRestore)

  const lastSyncLabel = formatLastSyncLabel(dataUpdatedAt, now)

  const assignment = useMemo(
    () => data?.assignments.find((a) => a.workerId === workerId) ?? null,
    [data, workerId],
  )

  const actions = assignment
    ? getWorkerActions(assignment.participationStatus, assignment.attendanceStatus)
    : []
  const waiting = assignment
    ? workerWaitingMessage(assignment.participationStatus, assignment.attendanceStatus)
    : null

  async function runAction(action: AssignmentAction) {
    if (!assignment) return
    setBanner(null)
    setPendingAction(action)
    try {
      await mutation.mutateAsync({ assignmentId: assignment.id, action })
      pushToast(ACTION_SUCCESS[action], 'success')
    } catch (err) {
      setBanner(toUserMessage(err))
    } finally {
      setPendingAction(null)
    }
  }

  return (
    <AppShell
      title="Worker view"
      subtitle="Your mobile shift portal — respond to invites and manage check-in."
      layout="portal"
    >
      <ToastStack toasts={toasts} onDismiss={dismissToast} />

      {banner ? <ErrorBanner message={banner} onDismiss={() => setBanner(null)} /> : null}

      <div className="portal-card field">
        <label htmlFor="worker-select">Worker</label>
        <select
          id="worker-select"
          value={workerId}
          aria-label="Worker"
          onChange={(e) => {
            setBanner(null)
            setWorkerId(e.target.value)
          }}
        >
          {DEMO_WORKERS.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name}
            </option>
          ))}
        </select>
      </div>

      {isLoading ? <LoadingSkeleton rows={4} /> : null}

      {isError && !data ? (
        <ErrorBanner message={toUserMessage(error)} onDismiss={() => void refetch()} />
      ) : null}

      {data && assignment ? (
        <>
          <section className="shift-panel portal-card" aria-labelledby="worker-shift-title">
            <div className="shift-panel__header">
              <div>
                <p className="eyebrow">Your shift</p>
                <h2 id="worker-shift-title">{data.shift.title}</h2>
              </div>
              <div className="shift-panel__tools">
                <LiveStatus state={liveState} lastSyncLabel={lastSyncLabel} />
                <button
                  type="button"
                  className="btn btn--ghost btn--sm"
                  onClick={() => void refetch()}
                  disabled={isFetching}
                >
                  {isFetching ? 'Refreshing…' : 'Refresh'}
                </button>
              </div>
            </div>
            <dl className="meta-grid">
              <div>
                <dt>Date</dt>
                <dd>{formatLocalDate(data.shift.startsAt)}</dd>
              </div>
              <div>
                <dt>Start</dt>
                <dd>{formatLocalTime(data.shift.startsAt)}</dd>
              </div>
              <div>
                <dt>End</dt>
                <dd>{formatLocalTime(data.shift.endsAt)}</dd>
              </div>
              <div>
                <dt>Location</dt>
                <dd>{data.shift.location || '—'}</dd>
              </div>
            </dl>
          </section>

          <section className="panel portal-card portal-card--focus" aria-labelledby="worker-status-title">
            <h3 className="panel__title" id="worker-status-title">
              {assignment.workerName}
            </h3>

            <div className="status-groups status-groups--stack">
              <div className="status-group">
                <span className="status-group__label">Participation</span>
                <ParticipationBadge status={assignment.participationStatus} />
              </div>
              <div className="status-group">
                <span className="status-group__label">Attendance</span>
                <AttendanceBadge status={assignment.attendanceStatus} />
              </div>
            </div>

            <dl className="meta-grid meta-grid--compact">
              <div>
                <dt>Checked in</dt>
                <dd data-testid="checked-in-at">{formatLocalDateTime(assignment.checkedInAt)}</dd>
              </div>
              <div>
                <dt>Checked out</dt>
                <dd data-testid="checked-out-at">
                  {formatLocalDateTime(assignment.checkedOutAt)}
                </dd>
              </div>
            </dl>

            {waiting ? <p className="waiting-note">{waiting}</p> : null}

            {actions.length > 0 ? (
              <div className="action-row action-row--portal">
                {actions.map((btn) => {
                  const busy = mutation.isPending && pendingAction === btn.action
                  return (
                    <button
                      key={btn.action}
                      type="button"
                      className={`btn btn--${btn.tone} btn--block`}
                      disabled={mutation.isPending}
                      onClick={() => void runAction(btn.action)}
                      data-testid={`action-${btn.action}`}
                      aria-label={`${btn.label} for ${assignment.workerName}`}
                    >
                      {busy ? ACTION_PENDING[btn.action] : btn.label}
                    </button>
                  )
                })}
              </div>
            ) : null}
          </section>
        </>
      ) : null}
    </AppShell>
  )
}
