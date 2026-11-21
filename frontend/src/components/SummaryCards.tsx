import type { DashboardCounts } from '../api/types'

const items: { key: keyof DashboardCounts; label: string }[] = [
  { key: 'filled', label: 'Filled' },
  { key: 'remaining', label: 'Remaining' },
  { key: 'checkedIn', label: 'Checked in' },
  { key: 'checkedOut', label: 'Checked out' },
]

export function SummaryCards({ counts }: { counts: DashboardCounts }) {
  return (
    <div className="summary-grid" data-testid="summary-cards">
      {items.map(({ key, label }) => (
        <div className="summary-card" key={key}>
          <p className="summary-card__label">{label}</p>
          <p className="summary-card__value" data-testid={`count-${key}`}>
            {counts[key]}
          </p>
        </div>
      ))}
    </div>
  )
}
