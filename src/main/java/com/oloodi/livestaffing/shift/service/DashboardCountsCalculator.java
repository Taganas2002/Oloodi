package com.oloodi.livestaffing.shift.service;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.shift.dto.DashboardCounts;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DashboardCountsCalculator {

    public DashboardCounts calculate(int requiredWorkers, List<ShiftAssignment> assignments) {
        int filled = 0;
        int checkedIn = 0;
        int checkedOut = 0;

        for (ShiftAssignment assignment : assignments) {
            if (assignment.getParticipationStatus() == ParticipationStatus.ACCEPTED) {
                filled++;
            }
            if (assignment.getAttendanceStatus() == AttendanceStatus.CHECKED_IN) {
                checkedIn++;
            } else if (assignment.getAttendanceStatus() == AttendanceStatus.CHECKED_OUT) {
                checkedOut++;
            }
        }

        int remaining = Math.max(requiredWorkers - filled, 0);
        return new DashboardCounts(filled, remaining, checkedIn, checkedOut);
    }
}
