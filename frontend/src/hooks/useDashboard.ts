import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { staffingApi } from '../api/staffingApi'
import type { AssignmentAction } from '../api/types'
import { isConflictError } from '../lib/errors'
import type { LiveConnectionState } from './useShiftLiveUpdates'

export const dashboardKey = (shiftId: string) => ['dashboard', shiftId] as const

export function useDashboard(shiftId: string) {
  return useQuery({
    queryKey: dashboardKey(shiftId),
    queryFn: () => staffingApi.getDashboard(shiftId),
    // Authoritative REST catch-up comes from SSE open/events — never poll.
  })
}

/**
 * Mutation helper: 409 always invalidates so the UI shows server truth.
 * On success, skip invalidate when LIVE — SSE assignment-changed already
 * triggers one refetch (avoids duplicate GET on the acting client).
 * When not LIVE, invalidate so the actor still updates without waiting for reconnect.
 * Buttons should use `isPending` to disable double-clicks (UX only; backend is idempotent).
 */
export function useAssignmentAction(
  shiftId: string,
  liveState: LiveConnectionState = 'CONNECTING',
) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      assignmentId,
      action,
    }: {
      assignmentId: string
      action: AssignmentAction
    }) => staffingApi.postAction(shiftId, assignmentId, action),
    onSuccess: async () => {
      if (liveState !== 'LIVE') {
        await queryClient.invalidateQueries({ queryKey: dashboardKey(shiftId) })
      }
    },
    onError: async (error) => {
      if (isConflictError(error)) {
        await queryClient.invalidateQueries({ queryKey: dashboardKey(shiftId) })
      }
    },
  })
}
