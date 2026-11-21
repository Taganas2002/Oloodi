import type { DashboardCounts } from '../api/types'

/**
 * Progress derived only from backend counts + requiredWorkers.
 * filled / required → percent; remaining from backend counts.remaining.
 */
export function StaffingProgress({
  filled,
  remaining,
  required,
}: {
  filled: number
  remaining: number
  required: number
} & Partial<DashboardCounts>) {
  const safeRequired = Math.max(required, 0)
  const pct =
    safeRequired === 0 ? 0 : Math.min(100, Math.round((filled / safeRequired) * 100))
  const fullyStaffed = remaining === 0 && safeRequired > 0
  const label = fullyStaffed
    ? 'Fully staffed'
    : `${remaining} remaining`

  return (
    <section className="staffing-progress" aria-label="Staffing progress" data-testid="staffing-progress">
      <div className="staffing-progress__header">
        <p className="staffing-progress__title">Staffing progress</p>
        <p className="staffing-progress__meta" data-testid="staffing-progress-label">
          <span data-testid="staffing-progress-fraction">
            {filled}/{safeRequired} filled
          </span>
          <span aria-hidden="true"> · </span>
          <span>{pct}%</span>
          <span aria-hidden="true"> · </span>
          <strong>{label}</strong>
        </p>
      </div>
      <div
        className="staffing-progress__track"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={pct}
        aria-label={`${filled} of ${safeRequired} positions filled`}
      >
        <div className="staffing-progress__fill" style={{ width: `${pct}%` }} />
      </div>
    </section>
  )
}
