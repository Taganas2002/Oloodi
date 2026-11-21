package com.oloodi.livestaffing.assignment.dto;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID shiftId,
        UUID workerId,
        String workerName,
        ParticipationStatus participationStatus,
        AttendanceStatus attendanceStatus,
        Instant checkedInAt,
        Instant checkedOutAt,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static AssignmentResponse from(ShiftAssignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getShift().getId(),
                assignment.getWorker().getId(),
                assignment.getWorker().getName(),
                assignment.getParticipationStatus(),
                assignment.getAttendanceStatus(),
                assignment.getCheckedInAt(),
                assignment.getCheckedOutAt(),
                assignment.getVersion(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }
}
