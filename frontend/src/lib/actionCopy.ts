import type { AssignmentAction } from '../api/types'

/** Idle button labels (clear, verb-forward). */
export const ACTION_LABELS: Record<AssignmentAction, string> = {
  invite: 'Send Invitation',
  accept: 'Accept Shift',
  decline: 'Decline Shift',
  cancel: 'Cancel Shift',
  'check-in': 'Check in',
  'check-out': 'Check out',
}

/** Pending button text while mutation in flight. */
export const ACTION_PENDING: Record<AssignmentAction, string> = {
  invite: 'Sending invitation…',
  accept: 'Accepting…',
  decline: 'Declining…',
  cancel: 'Cancelling…',
  'check-in': 'Checking in…',
  'check-out': 'Checking out…',
}

/** Success toast copy after a confirmed mutation. */
export const ACTION_SUCCESS: Record<AssignmentAction, string> = {
  invite: 'Invitation sent',
  accept: 'Shift accepted',
  decline: 'Shift declined',
  cancel: 'Shift cancelled',
  'check-in': 'Checked in',
  'check-out': 'Checked out',
}

export function acceptAgainLabel(): string {
  return 'Accept Shift Again'
}
