import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { dashboardKey } from './useDashboard'

export type LiveConnectionState = 'CONNECTING' | 'LIVE' | 'RECONNECTING'

function eventsUrl(shiftId: string): string {
  const base = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ?? ''
  return `${base}/api/shifts/${shiftId}/events`
}

/**
 * One EventSource per shift. On assignment-changed (and on open for catch-up),
 * invalidates the dashboard query so REST remains authoritative.
 * Never patches UI state from the SSE payload.
 */
export function useShiftLiveUpdates(shiftId: string): LiveConnectionState {
  const queryClient = useQueryClient()
  const [state, setState] = useState<LiveConnectionState>('CONNECTING')
  const openedOnce = useRef(false)

  useEffect(() => {
    openedOnce.current = false
    setState('CONNECTING')

    const source = new EventSource(eventsUrl(shiftId))

    const invalidate = () => {
      void queryClient.invalidateQueries({ queryKey: dashboardKey(shiftId) })
    }

    source.onopen = () => {
      setState('LIVE')
      // Catch-up refetch on (re)connect — covers events missed while disconnected.
      invalidate()
      openedOnce.current = true
    }

    source.addEventListener('assignment-changed', () => {
      invalidate()
    })

    source.onerror = () => {
      // Browser EventSource auto-reconnects; surface RECONNECTING after first open.
      setState(openedOnce.current ? 'RECONNECTING' : 'CONNECTING')
    }

    if (import.meta.env.DEV) {
      // E2E helper: simulate a dropped stream without killing REST.
      const w = window as unknown as {
        __oloodiSse?: EventSource
        __oloodiForceSseDisconnect?: () => void
      }
      w.__oloodiSse = source
      w.__oloodiForceSseDisconnect = () => {
        source.close()
        setState('RECONNECTING')
      }
    }

    return () => {
      source.close()
      if (import.meta.env.DEV) {
        const w = window as unknown as {
          __oloodiSse?: EventSource
          __oloodiForceSseDisconnect?: () => void
        }
        if (w.__oloodiSse === source) {
          delete w.__oloodiSse
          delete w.__oloodiForceSseDisconnect
        }
      }
    }
  }, [shiftId, queryClient])

  return state
}
