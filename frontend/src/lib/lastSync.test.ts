import { describe, expect, it } from 'vitest'
import { formatLastSyncLabel } from './lastSync'

describe('formatLastSyncLabel', () => {
  it('returns null without a real timestamp', () => {
    expect(formatLastSyncLabel(undefined)).toBeNull()
    expect(formatLastSyncLabel(0)).toBeNull()
  })

  it('says just now for recent updates', () => {
    const now = 1_000_000
    expect(formatLastSyncLabel(now - 1_000, now)).toBe('Last synchronized just now')
  })

  it('uses seconds then minutes for older updates', () => {
    const now = 1_000_000
    expect(formatLastSyncLabel(now - 20_000, now)).toBe('Last synchronized 20s ago')
    expect(formatLastSyncLabel(now - 120_000, now)).toBe('Last synchronized 2m ago')
  })
})
