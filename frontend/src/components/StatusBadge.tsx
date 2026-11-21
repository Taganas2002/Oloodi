import { formatAttendance, formatParticipation } from '../lib/format'
import type { AttendanceStatus, ParticipationStatus } from '../api/types'

const participationClass: Record<ParticipationStatus, string> = {
  UNINVITED: 'badge badge--muted',
  INVITED: 'badge badge--info',
  ACCEPTED: 'badge badge--success',
  DECLINED: 'badge badge--danger',
  CANCELLED: 'badge badge--warn',
}

const attendanceClass: Record<AttendanceStatus, string> = {
  NOT_STARTED: 'badge badge--muted',
  CHECKED_IN: 'badge badge--success',
  CHECKED_OUT: 'badge badge--neutral',
}

export function ParticipationBadge({ status }: { status: ParticipationStatus }) {
  return (
    <span className={participationClass[status]} data-testid={`participation-${status}`}>
      {formatParticipation(status)}
    </span>
  )
}

export function AttendanceBadge({ status }: { status: AttendanceStatus }) {
  return (
    <span className={attendanceClass[status]} data-testid={`attendance-${status}`}>
      {formatAttendance(status)}
    </span>
  )
}
