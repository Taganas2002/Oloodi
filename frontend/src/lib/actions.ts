import type {
  AssignmentAction,
  AttendanceStatus,
  ParticipationStatus,
} from '../api/types'
import { ACTION_LABELS, acceptAgainLabel } from './actionCopy'

export type WorkerAction = Extract<
  AssignmentAction,
  'accept' | 'decline' | 'cancel' | 'check-in' | 'check-out'
>

export type ManagerAction = Extract<AssignmentAction, 'invite'>

export interface ActionButton {
  action: WorkerAction | ManagerAction
  label: string
  tone: 'primary' | 'secondary' | 'danger'
}

/**
 * Legal worker actions for the current participation + attendance pair.
 * Mirrors Phase 1 AssignmentTransitionService (backend remains authoritative).
 */
export function getWorkerActions(
  participation: ParticipationStatus,
  attendance: AttendanceStatus,
): ActionButton[] {
  if (participation === 'UNINVITED') {
    return []
  }
  if (participation === 'INVITED' && attendance === 'NOT_STARTED') {
    return [
      { action: 'accept', label: ACTION_LABELS.accept, tone: 'primary' },
      { action: 'decline', label: ACTION_LABELS.decline, tone: 'danger' },
    ]
  }
  if (participation === 'ACCEPTED' && attendance === 'NOT_STARTED') {
    return [
      { action: 'check-in', label: ACTION_LABELS['check-in'], tone: 'primary' },
      { action: 'cancel', label: ACTION_LABELS.cancel, tone: 'secondary' },
    ]
  }
  if (participation === 'CANCELLED' && attendance === 'NOT_STARTED') {
    return [{ action: 'accept', label: acceptAgainLabel(), tone: 'primary' }]
  }
  if (participation === 'ACCEPTED' && attendance === 'CHECKED_IN') {
    return [{ action: 'check-out', label: ACTION_LABELS['check-out'], tone: 'primary' }]
  }
  // DECLINED, CHECKED_OUT, and any unexpected combo: no actions
  return []
}

export function canManagerInvite(participation: ParticipationStatus): boolean {
  return participation === 'UNINVITED'
}

export function workerWaitingMessage(
  participation: ParticipationStatus,
  attendance: AttendanceStatus,
): string | null {
  if (participation === 'UNINVITED') {
    return 'Waiting for an invite from the shift manager.'
  }
  if (participation === 'DECLINED') {
    return 'You declined this shift. This status is final.'
  }
  if (participation === 'ACCEPTED' && attendance === 'CHECKED_OUT') {
    return 'Shift complete — you are checked out.'
  }
  return null
}
