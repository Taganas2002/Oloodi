package com.oloodi.livestaffing.assignment.service;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Centralized legal state transitions for ShiftAssignment.
 *
 * <p>Assumption: {@link ParticipationStatus#DECLINED} is FINAL — there is no
 * DECLINED → ACCEPTED (or any other) path. Documented as a product assumption
 * for Phase 1; do not invent extra transitions.
 *
 * <p>Idempotency: if the assignment is already in the target state for an action,
 * the current state is returned unchanged (including timestamps).
 */
@Component
public class AssignmentTransitionService {

    private final Clock clock;

    public AssignmentTransitionService(Clock clock) {
        this.clock = clock;
    }

    public ShiftAssignment invite(ShiftAssignment assignment) {
        if (assignment.getParticipationStatus() == ParticipationStatus.INVITED
                && assignment.getAttendanceStatus() == AttendanceStatus.NOT_STARTED) {
            return assignment;
        }
        require(
                assignment,
                ParticipationStatus.UNINVITED,
                AttendanceStatus.NOT_STARTED,
                "invite");
        return apply(
                assignment,
                ParticipationStatus.INVITED,
                AttendanceStatus.NOT_STARTED,
                assignment.getCheckedInAt(),
                assignment.getCheckedOutAt());
    }

    public ShiftAssignment accept(ShiftAssignment assignment) {
        ParticipationStatus p = assignment.getParticipationStatus();
        AttendanceStatus a = assignment.getAttendanceStatus();

        if (p == ParticipationStatus.ACCEPTED && a == AttendanceStatus.NOT_STARTED) {
            return assignment;
        }
        if (p == ParticipationStatus.INVITED && a == AttendanceStatus.NOT_STARTED) {
            return apply(
                    assignment,
                    ParticipationStatus.ACCEPTED,
                    AttendanceStatus.NOT_STARTED,
                    assignment.getCheckedInAt(),
                    assignment.getCheckedOutAt());
        }
        if (p == ParticipationStatus.CANCELLED && a == AttendanceStatus.NOT_STARTED) {
            return apply(
                    assignment,
                    ParticipationStatus.ACCEPTED,
                    AttendanceStatus.NOT_STARTED,
                    assignment.getCheckedInAt(),
                    assignment.getCheckedOutAt());
        }
        throw illegal(assignment, "accept");
    }

    public ShiftAssignment decline(ShiftAssignment assignment) {
        if (assignment.getParticipationStatus() == ParticipationStatus.DECLINED
                && assignment.getAttendanceStatus() == AttendanceStatus.NOT_STARTED) {
            return assignment;
        }
        require(
                assignment,
                ParticipationStatus.INVITED,
                AttendanceStatus.NOT_STARTED,
                "decline");
        return apply(
                assignment,
                ParticipationStatus.DECLINED,
                AttendanceStatus.NOT_STARTED,
                assignment.getCheckedInAt(),
                assignment.getCheckedOutAt());
    }

    public ShiftAssignment cancel(ShiftAssignment assignment) {
        if (assignment.getParticipationStatus() == ParticipationStatus.CANCELLED
                && assignment.getAttendanceStatus() == AttendanceStatus.NOT_STARTED) {
            return assignment;
        }
        require(
                assignment,
                ParticipationStatus.ACCEPTED,
                AttendanceStatus.NOT_STARTED,
                "cancel");
        return apply(
                assignment,
                ParticipationStatus.CANCELLED,
                AttendanceStatus.NOT_STARTED,
                assignment.getCheckedInAt(),
                assignment.getCheckedOutAt());
    }

    public ShiftAssignment checkIn(ShiftAssignment assignment) {
        if (assignment.getParticipationStatus() == ParticipationStatus.ACCEPTED
                && assignment.getAttendanceStatus() == AttendanceStatus.CHECKED_IN) {
            return assignment;
        }
        require(
                assignment,
                ParticipationStatus.ACCEPTED,
                AttendanceStatus.NOT_STARTED,
                "check-in");
        // Truncate to micros to match PostgreSQL timestamptz precision (stable idempotent reads).
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Instant checkedInAt =
                assignment.getCheckedInAt() != null ? assignment.getCheckedInAt() : now;
        return apply(
                assignment,
                ParticipationStatus.ACCEPTED,
                AttendanceStatus.CHECKED_IN,
                checkedInAt,
                assignment.getCheckedOutAt());
    }

    public ShiftAssignment checkOut(ShiftAssignment assignment) {
        if (assignment.getParticipationStatus() == ParticipationStatus.ACCEPTED
                && assignment.getAttendanceStatus() == AttendanceStatus.CHECKED_OUT) {
            return assignment;
        }
        require(
                assignment,
                ParticipationStatus.ACCEPTED,
                AttendanceStatus.CHECKED_IN,
                "check-out");
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Instant checkedOutAt =
                assignment.getCheckedOutAt() != null ? assignment.getCheckedOutAt() : now;
        return apply(
                assignment,
                ParticipationStatus.ACCEPTED,
                AttendanceStatus.CHECKED_OUT,
                assignment.getCheckedInAt(),
                checkedOutAt);
    }

    private void require(
            ShiftAssignment assignment,
            ParticipationStatus expectedParticipation,
            AttendanceStatus expectedAttendance,
            String action) {
        if (assignment.getParticipationStatus() != expectedParticipation
                || assignment.getAttendanceStatus() != expectedAttendance) {
            throw illegal(assignment, action);
        }
    }

    private IllegalTransitionException illegal(ShiftAssignment assignment, String action) {
        return new IllegalTransitionException(
                assignment.getParticipationStatus(), assignment.getAttendanceStatus(), action);
    }

    private ShiftAssignment apply(
            ShiftAssignment assignment,
            ParticipationStatus participationStatus,
            AttendanceStatus attendanceStatus,
            Instant checkedInAt,
            Instant checkedOutAt) {
        assignment.applyState(
                participationStatus,
                attendanceStatus,
                checkedInAt,
                checkedOutAt,
                Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        return assignment;
    }
}
