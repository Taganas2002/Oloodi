import type { LiveConnectionState } from '../hooks/useShiftLiveUpdates'

/**
 * Live sync indicator only — not a full "application offline" banner.
 * REST may still succeed while SSE is reconnecting.
 * lastSyncLabel is optional and must reflect a real query update time.
 */
export function LiveStatus({
  state,
  lastSyncLabel,
}: {
  state: LiveConnectionState
  lastSyncLabel?: string | null
}) {
  const isLive = state === 'LIVE'
  const isReconnecting = state === 'RECONNECTING'
  const label = isLive ? 'Live' : isReconnecting ? 'Reconnecting…' : 'Connecting…'

  return (
    <div className="live-status-wrap" data-testid="live-status-wrap">
      <span
        className={`live-status live-status--${state.toLowerCase()}`}
        data-testid="live-status"
        data-state={state}
        role="status"
        aria-live="polite"
        title={
          isLive
            ? 'Receiving live assignment updates'
            : 'Live stream interrupted — dashboard will catch up on reconnect'
        }
      >
        <span className="live-status__dot" aria-hidden="true">
          {isLive ? '●' : isReconnecting ? '⚠' : '○'}
        </span>
        {label}
      </span>
      {isReconnecting ? (
        <p className="live-status__hint" data-testid="live-unavailable-hint">
          Live updates temporarily unavailable.
        </p>
      ) : null}
      {isLive && lastSyncLabel ? (
        <p className="live-status__sync" data-testid="last-sync">
          {lastSyncLabel}
        </p>
      ) : null}
    </div>
  )
}
