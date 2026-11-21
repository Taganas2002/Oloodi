import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LiveStatus } from './LiveStatus'

describe('LiveStatus', () => {
  it('shows Live when LIVE', () => {
    render(<LiveStatus state="LIVE" lastSyncLabel="Last synchronized just now" />)
    expect(screen.getByTestId('live-status')).toHaveAttribute('data-state', 'LIVE')
    expect(screen.getByTestId('live-status')).toHaveTextContent(/Live/)
    expect(screen.queryByTestId('live-unavailable-hint')).not.toBeInTheDocument()
    expect(screen.getByTestId('last-sync')).toHaveTextContent(/just now/)
  })

  it('shows Reconnecting hint when RECONNECTING', () => {
    render(<LiveStatus state="RECONNECTING" />)
    expect(screen.getByTestId('live-status')).toHaveAttribute('data-state', 'RECONNECTING')
    expect(screen.getByTestId('live-status')).toHaveTextContent(/Reconnecting/)
    expect(screen.getByTestId('live-unavailable-hint')).toHaveTextContent(
      /Live updates temporarily unavailable/,
    )
  })
})
