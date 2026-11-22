package com.oloodi.livestaffing.dev;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.shift.domain.Shift;
import com.oloodi.livestaffing.shift.repository.ShiftRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restores demo seed assignment states for deterministic E2E / local demos.
 * Also aligns the demo shift window to the current America/Montreal calendar day
 * (09:00–17:00) so check-in/out timestamps share the same date as the shift UI.
 * Only exposed via {@link DevResetController} when the local/demo/test profile is active.
 */
@Service
public class DevResetService {

    private static final Logger log = LoggerFactory.getLogger(DevResetService.class);

    static final UUID SHIFT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final List<UUID> ASSIGNMENT_IDS = List.of(
            UUID.fromString("33333333-3333-3333-3333-333333333301"),
            UUID.fromString("33333333-3333-3333-3333-333333333302"),
            UUID.fromString("33333333-3333-3333-3333-333333333303"));

    private final ShiftAssignmentRepository assignmentRepository;
    private final ShiftRepository shiftRepository;
    private final Clock clock;

    public DevResetService(
            ShiftAssignmentRepository assignmentRepository,
            ShiftRepository shiftRepository,
            Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.shiftRepository = shiftRepository;
        this.clock = clock;
    }

    @Transactional
    public int resetDemoAssignments() {
        Instant stamp = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Shift shift = shiftRepository
                .findById(SHIFT_ID)
                .orElseThrow(() -> new IllegalStateException("Missing seed shift " + SHIFT_ID));
        DemoShiftSchedule.Window window = DemoShiftSchedule.windowFor(clock);
        shift.reschedule(window.startsAt(), window.endsAt(), stamp);
        shiftRepository.save(shift);

        int updated = 0;
        for (UUID id : ASSIGNMENT_IDS) {
            ShiftAssignment assignment = assignmentRepository
                    .findByShiftIdAndIdWithWorker(SHIFT_ID, id)
                    .orElseThrow(() -> new IllegalStateException("Missing seed assignment " + id));
            assignment.applyState(
                    ParticipationStatus.UNINVITED,
                    AttendanceStatus.NOT_STARTED,
                    null,
                    null,
                    stamp);
            assignmentRepository.save(assignment);
            updated++;
        }
        log.info(
                "Dev reset restored {} demo assignments to UNINVITED/NOT_STARTED; shift window {} – {}",
                updated,
                window.startsAt(),
                window.endsAt());
        return updated;
    }
}
