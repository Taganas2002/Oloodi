import { useEffect, useRef } from 'react'
import type { LiveConnectionState } from './useShiftLiveUpdates'

/** Fires once when SSE recovers RECONNECTING → LIVE (real transition only). */
export function useLiveRestoreToast(
  liveState: LiveConnectionState,
  onRestore: () => void,
) {
  const prev = useRef<LiveConnectionState>(liveState)

  useEffect(() => {
    if (prev.current === 'RECONNECTING' && liveState === 'LIVE') {
      onRestore()
    }
    prev.current = liveState
  }, [liveState, onRestore])
}
