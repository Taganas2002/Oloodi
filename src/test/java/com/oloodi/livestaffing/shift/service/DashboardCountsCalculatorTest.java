package com.oloodi.livestaffing.shift.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.shift.domain.Shift;
import com.oloodi.livestaffing.shift.dto.DashboardCounts;
import com.oloodi.livestaffing.worker.domain.Worker;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DashboardCountsCalculatorTest {

    private final DashboardCountsCalculator calculator = new DashboardCountsCalculator();

    @Test
    void countsMatchAssignmentList() {
        Instant now = Instant.parse("2026-11-21T12:00:00Z");
        Shift shift = new Shift(UUID.randomUUID(), "S", now, now.plusSeconds(1), "X", 3, now, now);

        List<ShiftAssignment> assignments = List.of(
                assignment(shift, ParticipationStatus.ACCEPTED, AttendanceStatus.CHECKED_IN, now),
                assignment(shift, ParticipationStatus.ACCEPTED, AttendanceStatus.NOT_STARTED, now),
                assignment(shift, ParticipationStatus.INVITED, AttendanceStatus.NOT_STARTED, now),
                assignment(shift, ParticipationStatus.ACCEPTED, AttendanceStatus.CHECKED_OUT, now),
                assignment(shift, ParticipationStatus.DECLINED, AttendanceStatus.NOT_STARTED, now));

        DashboardCounts counts = calculator.calculate(3, assignments);

        assertThat(counts.filled()).isEqualTo(3); // three ACCEPTED
        assertThat(counts.remaining()).isEqualTo(0); // max(3-3,0)
        assertThat(counts.checkedIn()).isEqualTo(1);
        assertThat(counts.checkedOut()).isEqualTo(1);
    }

    @Test
    void remainingNeverNegative() {
        Instant now = Instant.parse("2026-11-21T12:00:00Z");
        Shift shift = new Shift(UUID.randomUUID(), "S", now, now.plusSeconds(1), "X", 1, now, now);
        List<ShiftAssignment> assignments = List.of(
                assignment(shift, ParticipationStatus.ACCEPTED, AttendanceStatus.NOT_STARTED, now),
                assignment(shift, ParticipationStatus.ACCEPTED, AttendanceStatus.NOT_STARTED, now));

        DashboardCounts counts = calculator.calculate(1, assignments);
        assertThat(counts.filled()).isEqualTo(2);
        assertThat(counts.remaining()).isEqualTo(0);
    }

    private static ShiftAssignment assignment(
            Shift shift, ParticipationStatus p, AttendanceStatus a, Instant now) {
        Worker worker = new Worker(UUID.randomUUID(), "W", now);
        return new ShiftAssignment(UUID.randomUUID(), shift, worker, p, a, now, now);
    }
}
