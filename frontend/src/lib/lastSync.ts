/** Human label from a real react-query dataUpdatedAt epoch ms. Never invents sync. */
export function formatLastSyncLabel(dataUpdatedAt: number | undefined, now = Date.now()): string | null {
  if (!dataUpdatedAt || dataUpdatedAt <= 0) {
    return null
  }
  const ageMs = Math.max(0, now - dataUpdatedAt)
  if (ageMs < 8_000) {
    return 'Last synchronized just now'
  }
  if (ageMs < 60_000) {
    const secs = Math.max(1, Math.round(ageMs / 1000))
    return `Last synchronized ${secs}s ago`
  }
  const mins = Math.max(1, Math.round(ageMs / 60_000))
  return `Last synchronized ${mins}m ago`
}
