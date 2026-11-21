export type ParticipationStatus =
  | 'UNINVITED'
  | 'INVITED'
  | 'ACCEPTED'
  | 'DECLINED'
  | 'CANCELLED'

export type AttendanceStatus = 'NOT_STARTED' | 'CHECKED_IN' | 'CHECKED_OUT'

export type AssignmentAction =
  | 'invite'
  | 'accept'
  | 'decline'
  | 'cancel'
  | 'check-in'
  | 'check-out'

export interface Shift {
  id: string
  title: string
  startsAt: string
  endsAt: string
  location: string
  requiredWorkers: number
  createdAt: string
  updatedAt: string
}

/** Seeded worker as shown in the worker picker (derived from assignments). */
export interface Worker {
  id: string
  name: string
}

export interface Assignment {
  id: string
  shiftId: string
  workerId: string
  workerName: string
  participationStatus: ParticipationStatus
  attendanceStatus: AttendanceStatus
  checkedInAt: string | null
  checkedOutAt: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface DashboardCounts {
  filled: number
  remaining: number
  checkedIn: number
  checkedOut: number
}

export interface DashboardResponse {
  shift: Shift
  assignments: Assignment[]
  counts: DashboardCounts
}

/** Spring ProblemDetail subset returned by Phase 1 GlobalExceptionHandler. */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  path?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed (${problem.status})`)
    this.name = 'ApiError'
    this.status = problem.status
    this.problem = problem
  }

  get isConflict(): boolean {
    return this.status === 409
  }
}
