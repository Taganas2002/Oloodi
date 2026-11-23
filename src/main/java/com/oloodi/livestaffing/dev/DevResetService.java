package com.oloodi.livestaffing.dev;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.realtime.AssignmentChangedEvent;
import com.oloodi.livestaffing.shift.domain.Shift;
import com.oloodi.livestaffing.shift.repository.ShiftRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
    private static final int REQUIRED_WORKERS = 3;
    private static final List<UUID> ASSIGNMENT_IDS = List.of(
            UUID.fromString("33333333-3333-3333-3333-333333333301"),
            UUID.fromString("33333333-3333-3333-3333-333333333302"),
            UUID.fromString("33333333-3333-3333-3333-333333333303"));

    private final ShiftAssignmentRepository assignmentRepository;
    private final ShiftRepository shiftRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public DevResetService(
            ShiftAssignmentRepository assignmentRepository,
            ShiftRepository shiftRepository,
            Clock clock,
            ApplicationEventPublisher eventPublisher) {
        this.assignmentRepository = assignmentRepository;
        this.shiftRepository = shiftRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public int resetDemoAssignments() {
        Instant stamp = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Shift shift = shiftRepository
                .findById(SHIFT_ID)
                .orElseThrow(() -> new IllegalStateException("Missing seed shift " + SHIFT_ID));
        DemoShiftSchedule.Window window = DemoShiftSchedule.windowFor(Clock.fixed(stamp, clock.getZone()));
        if (!window.startsAt().equals(shift.getStartsAt())
                || !window.endsAt().equals(shift.getEndsAt())
                || shift.getRequiredWorkers() != REQUIRED_WORKERS) {
            shift.restoreDemoSchedule(window.startsAt(), window.endsAt(), REQUIRED_WORKERS, stamp);
        }

        for (UUID id : ASSIGNMENT_IDS) {
            ShiftAssignment assignment = assignmentRepository
                    .findByShiftIdAndIdWithWorker(SHIFT_ID, id)
                    .orElseThrow(() -> new IllegalStateException("Missing seed assignment " + id));
            if (assignment.getParticipationStatus() != ParticipationStatus.UNINVITED
                    || assignment.getAttendanceStatus() != AttendanceStatus.NOT_STARTED
                    || assignment.getCheckedInAt() != null
                    || assignment.getCheckedOutAt() != null) {
                assignment.applyState(ParticipationStatus.UNINVITED, AttendanceStatus.NOT_STARTED,
                        null, null, stamp);
            }
        }
        // Clients refetch the whole shift. One event refreshes both views after commit.
        eventPublisher.publishEvent(AssignmentChangedEvent.of(SHIFT_ID, ASSIGNMENT_IDS.getFirst(), "reset", stamp));
        log.info(
                "Dev reset restored {} demo assignments to UNINVITED/NOT_STARTED; shift window {} – {}",
                ASSIGNMENT_IDS.size(),
                window.startsAt(),
                window.endsAt());
        return ASSIGNMENT_IDS.size();
    }
}
