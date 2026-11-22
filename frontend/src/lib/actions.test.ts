import { describe, expect, it } from 'vitest'
import { canManagerInvite, getWorkerActions, workerWaitingMessage } from './actions'

describe('getWorkerActions', () => {
  it('shows waiting only for UNINVITED (no actions)', () => {
    expect(getWorkerActions('UNINVITED', 'NOT_STARTED')).toEqual([])
    expect(workerWaitingMessage('UNINVITED', 'NOT_STARTED')).toMatch(/Waiting/)
  })

  it('maps INVITED to Accept and Decline', () => {
    expect(getWorkerActions('INVITED', 'NOT_STARTED').map((a) => a.action)).toEqual([
      'accept',
      'decline',
    ])
  })

  it('maps ACCEPTED+NOT_STARTED to Check in and Cancel', () => {
    expect(getWorkerActions('ACCEPTED', 'NOT_STARTED').map((a) => a.action)).toEqual([
      'check-in',
      'cancel',
    ])
  })

  it('maps CANCELLED to Accept again', () => {
    expect(getWorkerActions('CANCELLED', 'NOT_STARTED').map((a) => a.action)).toEqual([
      'accept',
    ])
  })

  it('maps CHECKED_IN to Check out only', () => {
    expect(getWorkerActions('ACCEPTED', 'CHECKED_IN').map((a) => a.action)).toEqual([
      'check-out',
    ])
  })

  it('has no actions for final DECLINED and CHECKED_OUT', () => {
    expect(getWorkerActions('DECLINED', 'NOT_STARTED')).toEqual([])
    expect(getWorkerActions('ACCEPTED', 'CHECKED_OUT')).toEqual([])
    expect(workerWaitingMessage('DECLINED', 'NOT_STARTED')).toBe('You declined this shift.')
  })
})

describe('canManagerInvite', () => {
  it('allows invite only when UNINVITED', () => {
    expect(canManagerInvite('UNINVITED')).toBe(true)
    expect(canManagerInvite('INVITED')).toBe(false)
    expect(canManagerInvite('ACCEPTED')).toBe(false)
  })
})
