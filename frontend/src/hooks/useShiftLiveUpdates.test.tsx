import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { dashboardKey } from './useDashboard'
import { useShiftLiveUpdates } from './useShiftLiveUpdates'

type Handler = ((ev?: MessageEvent) => void) | null

class MockEventSource {
  static instances: MockEventSource[] = []
  url: string
  onopen: Handler = null
  onerror: Handler = null
  listeners = new Map<string, Handler>()
  closed = false

  constructor(url: string) {
    this.url = url
    MockEventSource.instances.push(this)
  }

  addEventListener(type: string, handler: Handler) {
    this.listeners.set(type, handler)
  }

  close() {
    this.closed = true
  }

  emitOpen() {
    this.onopen?.(new Event('open') as unknown as MessageEvent)
  }

  emitError() {
    this.onerror?.(new Event('error') as unknown as MessageEvent)
  }

  emitAssignmentChanged() {
    this.listeners.get('assignment-changed')?.(
      new MessageEvent('assignment-changed', {
        data: JSON.stringify({ type: 'ASSIGNMENT_CHANGED', action: 'invite' }),
      }),
    )
  }
}

describe('useShiftLiveUpdates', () => {
  const shiftId = '11111111-1111-1111-1111-111111111111'
  let queryClient: QueryClient

  beforeEach(() => {
    MockEventSource.instances = []
    vi.stubGlobal('EventSource', MockEventSource as unknown as typeof EventSource)
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    queryClient.clear()
  })

  function wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }

  it('moves to LIVE on open and invalidates dashboard (catch-up)', async () => {
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useShiftLiveUpdates(shiftId), { wrapper })

    expect(result.current).toBe('CONNECTING')
    expect(MockEventSource.instances).toHaveLength(1)

    await act(async () => {
      MockEventSource.instances[0].emitOpen()
    })

    await waitFor(() => expect(result.current).toBe('LIVE'))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: dashboardKey(shiftId) })
  })

  it('invalidates dashboard on assignment-changed without patching cache from payload', async () => {
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useShiftLiveUpdates(shiftId), { wrapper })

    await act(async () => {
      MockEventSource.instances[0].emitOpen()
      MockEventSource.instances[0].emitAssignmentChanged()
    })

    await waitFor(() => expect(result.current).toBe('LIVE'))
    // open + event
    expect(invalidateSpy.mock.calls.length).toBeGreaterThanOrEqual(2)
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: dashboardKey(shiftId) })
  })

  it('moves to RECONNECTING on error after open', async () => {
    const { result } = renderHook(() => useShiftLiveUpdates(shiftId), { wrapper })

    await act(async () => {
      MockEventSource.instances[0].emitOpen()
    })
    await waitFor(() => expect(result.current).toBe('LIVE'))

    await act(async () => {
      MockEventSource.instances[0].emitError()
    })
    await waitFor(() => expect(result.current).toBe('RECONNECTING'))
  })

  it('closes EventSource on unmount', () => {
    const { unmount } = renderHook(() => useShiftLiveUpdates(shiftId), { wrapper })
    const source = MockEventSource.instances[0]
    unmount()
    expect(source.closed).toBe(true)
  })
})
