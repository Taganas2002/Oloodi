package com.oloodi.livestaffing.dev;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restores demo seed assignment states for deterministic E2E / local demos.
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

    public DevResetService(ShiftAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public int resetDemoAssignments() {
        Instant stamp = Instant.parse("2026-11-01T12:00:00Z");
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
        log.info("Dev reset restored {} demo assignments to UNINVITED/NOT_STARTED", updated);
        return updated;
    }
}
