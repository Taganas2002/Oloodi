/** Format an ISO instant in the viewer's local timezone. Never invents timestamps. */
export function formatLocalDateTime(iso: string | null | undefined): string {
  if (!iso) {
    return '—'
  }
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function formatLocalDate(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'full' }).format(date)
}

export function formatLocalTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return '—'
  }
  return new Intl.DateTimeFormat(undefined, { timeStyle: 'short' }).format(date)
}

export function formatParticipation(status: string): string {
  return status.replaceAll('_', ' ')
}

export function formatAttendance(status: string): string {
  return status.replaceAll('_', ' ')
}
