import { useCallback, useEffect, useState } from 'react'
import { DEMO_SHIFT_ID } from '../api/demoIds'
import type { Assignment, ParticipationStatus } from '../api/types'
import { AppShell } from '../components/AppShell'
import { AttendanceBadge, ParticipationBadge } from '../components/StatusBadge'
import { ErrorBanner } from '../components/ErrorBanner'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { LiveStatus } from '../components/LiveStatus'
import { StaffingProgress } from '../components/StaffingProgress'
import { SummaryCards } from '../components/SummaryCards'
import { ToastStack } from '../components/Toast'
import { useAssignmentAction, useDashboard, useDemoReset } from '../hooks/useDashboard'
import { useLiveRestoreToast } from '../hooks/useLiveRestoreToast'
import { useShiftLiveUpdates } from '../hooks/useShiftLiveUpdates'
import { useToasts } from '../hooks/useToasts'
import { ACTION_LABELS, ACTION_PENDING, ACTION_SUCCESS } from '../lib/actionCopy'
import { canManagerInvite } from '../lib/actions'
import { toUserMessage } from '../lib/errors'
import { formatLastSyncLabel } from '../lib/lastSync'
import { formatLocalDate, formatLocalDateTime, formatLocalTime } from '../lib/format'

/** Non-clickable Action-column label when invite is no longer available. */
function managerActionStatusLabel(participation: ParticipationStatus): string | null {
  switch (participation) {
    case 'INVITED':
      return 'Invited'
    case 'CANCELLED':
      return 'Cancelled'
    case 'DECLINED':
      return 'Declined'
    case 'ACCEPTED':
      return 'Accepted'
    default:
      return null
  }
}

export function ManagerPage() {
  const shiftId = DEMO_SHIFT_ID
  const liveState = useShiftLiveUpdates(shiftId)
  const { data, isLoading, isError, error, refetch, isFetching, dataUpdatedAt } =
    useDashboard(shiftId)
  const mutation = useAssignmentAction(shiftId, liveState)
  const resetMutation = useDemoReset(shiftId)
  const [banner, setBanner] = useState<string | null>(null)
  const [pendingId, setPendingId] = useState<string | null>(null)
  const { toasts, pushToast, dismissToast } = useToasts()
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 5_000)
    return () => window.clearInterval(id)
  }, [])

  const onRestore = useCallback(() => {
    pushToast('Live connection restored. Latest staffing data loaded.', 'info')
  }, [pushToast])
  useLiveRestoreToast(liveState, onRestore)

  const lastSyncLabel = formatLastSyncLabel(dataUpdatedAt, now)

  async function invite(assignment: Assignment) {
    setBanner(null)
    setPendingId(assignment.id)
    try {
      await mutation.mutateAsync({ assignmentId: assignment.id, action: 'invite' })
      pushToast(ACTION_SUCCESS.invite, 'success')
    } catch (err) {
      setBanner(toUserMessage(err))
    } finally {
      setPendingId(null)
    }
  }

  async function resetDemo() {
    if (!window.confirm('Reset the demo? All three workers will return to Invited / Not started.')) {
      return
    }
    setBanner(null)
    try {
      const result = await resetMutation.mutateAsync()
      pushToast(`Demo reset. ${result.updatedAssignments} workers are invited.`, 'success')
    } catch (err) {
      setBanner(toUserMessage(err))
    }
  }

  return (
    <AppShell
      title="Manager dashboard"
      subtitle="Invite workers and monitor live staffing for the demo shift."
      layout="saas"
    >
      <ToastStack toasts={toasts} onDismiss={dismissToast} />

      {banner ? <ErrorBanner message={banner} onDismiss={() => setBanner(null)} /> : null}

      {isLoading ? <LoadingSkeleton rows={5} /> : null}

      {isError && !data ? (
        <ErrorBanner
          message={toUserMessage(error)}
          onDismiss={() => {
            void refetch()
          }}
        />
      ) : null}

      {data ? (
        <>
          <section className="shift-panel" aria-labelledby="manager-shift-title">
            <div className="shift-panel__header">
              <div>
                <p className="eyebrow">Active shift</p>
                <h2 id="manager-shift-title">{data.shift.title}</h2>
              </div>
              <div className="shift-panel__tools">
                <LiveStatus state={liveState} lastSyncLabel={lastSyncLabel} />
                <button
                  type="button"
                  className="btn btn--secondary btn--sm"
                  onClick={() => void resetDemo()}
                  disabled={resetMutation.isPending || mutation.isPending}
                  title="Restore all demo workers to Invited / Not started"
                >
                  {resetMutation.isPending ? 'Resetting…' : 'Reset demo'}
                </button>
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
              <div>
                <dt>Required</dt>
                <dd>{data.shift.requiredWorkers} workers</dd>
              </div>
            </dl>
          </section>

          <SummaryCards counts={data.counts} />

          <StaffingProgress
            filled={data.counts.filled}
            remaining={data.counts.remaining}
            required={data.shift.requiredWorkers}
          />

          <section className="panel" aria-labelledby="manager-workers-title">
            <div className="panel__heading">
              <h3 className="panel__title" id="manager-workers-title">
                Workers
              </h3>
              <p className="panel__hint">Participation and attendance are shown separately.</p>
            </div>
            <ul className="worker-list">
              <li className="worker-list__head" aria-hidden="true">
                <span>Worker</span>
                <span>Participation</span>
                <span>Attendance</span>
                <span>Check in</span>
                <span>Check out</span>
                <span>Action</span>
              </li>
              {data.assignments.map((assignment) => {
                const inviting = pendingId === assignment.id && mutation.isPending
                const actionStatus = managerActionStatusLabel(assignment.participationStatus)
                return (
                  <li
                    className="worker-row"
                    key={assignment.id}
                    data-testid={`worker-row-${assignment.workerId}`}
                  >
                    <p className="worker-row__name">{assignment.workerName}</p>
                    <div className="worker-row__status">
                      <span className="worker-row__field-label">Participation</span>
                      <ParticipationBadge status={assignment.participationStatus} />
                    </div>
                    <div className="worker-row__status">
                      <span className="worker-row__field-label">Attendance</span>
                      <AttendanceBadge status={assignment.attendanceStatus} />
                    </div>
                    <div className="worker-row__time">
                      <span className="worker-row__field-label">Check in</span>
                      <span>{formatLocalDateTime(assignment.checkedInAt)}</span>
                    </div>
                    <div className="worker-row__time">
                      <span className="worker-row__field-label">Check out</span>
                      <span>{formatLocalDateTime(assignment.checkedOutAt)}</span>
                    </div>
                    <div className="worker-row__actions">
                      {canManagerInvite(assignment.participationStatus) ? (
                        <button
                          type="button"
                          className="btn btn--primary btn--sm"
                          disabled={mutation.isPending || resetMutation.isPending}
                          onClick={() => void invite(assignment)}
                          aria-label={`Invite ${assignment.workerName}`}
                        >
                          {inviting ? ACTION_PENDING.invite : ACTION_LABELS.invite}
                        </button>
                      ) : actionStatus ? (
                        <button
                          type="button"
                          className="btn btn--ghost btn--sm worker-row__action-status"
                          disabled
                          aria-label={`${assignment.workerName}: ${actionStatus}`}
                        >
                          {actionStatus}
                        </button>
                      ) : null}
                    </div>
                  </li>
                )
              })}
            </ul>
          </section>
        </>
      ) : null}
    </AppShell>
  )
}
