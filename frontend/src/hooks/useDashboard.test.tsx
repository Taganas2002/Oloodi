import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/types'
import { dashboardKey, useAssignmentAction } from './useDashboard'

const postAction = vi.fn()

vi.mock('../api/staffingApi', () => ({
  staffingApi: {
    postAction: (...args: unknown[]) => postAction(...args),
    getDashboard: vi.fn(),
  },
}))

describe('useAssignmentAction', () => {
  const shiftId = '11111111-1111-1111-1111-111111111111'
  let queryClient: QueryClient

  beforeEach(() => {
    postAction.mockReset()
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  })

  afterEach(() => {
    queryClient.clear()
  })

  function wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }

  it('invalidates dashboard on 409 conflict', async () => {
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    postAction.mockRejectedValue(
      new ApiError({
        status: 409,
        title: 'Conflict',
        detail: 'Illegal transition: cannot cancel from participation=ACCEPTED attendance=CHECKED_IN',
      }),
    )

    const { result } = renderHook(() => useAssignmentAction(shiftId, 'LIVE'), { wrapper })

    await act(async () => {
      try {
        await result.current.mutateAsync({
          assignmentId: '33333333-3333-3333-3333-333333333301',
          action: 'cancel',
        })
      } catch {
        // expected
      }
    })

    await waitFor(() =>
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: dashboardKey(shiftId) }),
    )
  })

  it('skips success invalidate when LIVE (SSE owns refetch)', async () => {
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    postAction.mockResolvedValue({ id: 'a', participationStatus: 'INVITED' })

    const { result } = renderHook(() => useAssignmentAction(shiftId, 'LIVE'), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({
        assignmentId: '33333333-3333-3333-3333-333333333301',
        action: 'invite',
      })
    })

    expect(invalidateSpy).not.toHaveBeenCalled()
  })

  it('invalidates on success when not LIVE', async () => {
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    postAction.mockResolvedValue({ id: 'a', participationStatus: 'INVITED' })

    const { result } = renderHook(() => useAssignmentAction(shiftId, 'RECONNECTING'), {
      wrapper,
    })

    await act(async () => {
      await result.current.mutateAsync({
        assignmentId: '33333333-3333-3333-3333-333333333301',
        action: 'invite',
      })
    })

    await waitFor(() =>
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: dashboardKey(shiftId) }),
    )
  })

  it('exposes isPending while mutation in flight', async () => {
    let resolve!: (v: unknown) => void
    postAction.mockReturnValue(
      new Promise((r) => {
        resolve = r
      }),
    )

    const { result } = renderHook(() => useAssignmentAction(shiftId), { wrapper })
    expect(result.current.isPending).toBe(false)

    let pendingPromise: Promise<unknown>
    act(() => {
      pendingPromise = result.current.mutateAsync({
        assignmentId: 'a',
        action: 'invite',
      })
    })

    await waitFor(() => expect(result.current.isPending).toBe(true))

    await act(async () => {
      resolve({ id: 'a', participationStatus: 'INVITED' })
      await pendingPromise
    })

    await waitFor(() => expect(result.current.isPending).toBe(false))
  })
})
