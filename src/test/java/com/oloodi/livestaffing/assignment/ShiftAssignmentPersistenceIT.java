package com.oloodi.livestaffing.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.shift.domain.Shift;
import com.oloodi.livestaffing.shift.repository.ShiftRepository;
import com.oloodi.livestaffing.support.AbstractPostgresIntegrationTest;
import com.oloodi.livestaffing.support.DemoIds;
import com.oloodi.livestaffing.worker.domain.Worker;
import com.oloodi.livestaffing.worker.repository.WorkerRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("E. Testcontainers: Flyway, seed, unique constraint, persistence, optimistic lock")
class ShiftAssignmentPersistenceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void resetSeedAssignments() {
        // Shared Postgres across IT classes — restore seed participation for assertions.
        for (UUID id : List.of(
                DemoIds.ASSIGNMENT_SARAH, DemoIds.ASSIGNMENT_DANIEL, DemoIds.ASSIGNMENT_ALEX)) {
            transactionTemplate.executeWithoutResult(status -> {
                ShiftAssignment a = assignmentRepository
                        .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, id)
                        .orElseThrow();
                a.applyState(
                        ParticipationStatus.UNINVITED,
                        AttendanceStatus.NOT_STARTED,
                        null,
                        null,
                        Instant.parse("2026-11-01T12:00:00Z"));
                assignmentRepository.saveAndFlush(a);
            });
        }
    }

    @Test
    void flywaySeedLoadsDemoShiftWorkersAndAssignments() {
        Shift shift = shiftRepository.findById(DemoIds.SHIFT_ID).orElseThrow();
        assertThat(shift.getTitle()).isEqualTo("Oloodi Event Staffing");
        assertThat(shift.getLocation()).isEqualTo("Montréal, QC");
        assertThat(shift.getRequiredWorkers()).isEqualTo(3);

        assertThat(workerRepository.findAll())
                .extracting(Worker::getName)
                .containsExactlyInAnyOrder("Sarah Johnson", "Daniel Martin", "Alex Smith");

        List<ShiftAssignment> assignments =
                assignmentRepository.findAllByShiftIdWithWorker(DemoIds.SHIFT_ID);
        assertThat(assignments).hasSize(3);
        assertThat(assignments)
                .allMatch(a -> a.getParticipationStatus() == ParticipationStatus.UNINVITED)
                .allMatch(a -> a.getAttendanceStatus() == AttendanceStatus.NOT_STARTED)
                .allMatch(a -> a.getCheckedInAt() == null)
                .allMatch(a -> a.getCheckedOutAt() == null);
    }

    @Test
    void uniqueConstraintOnShiftAndWorker() {
        Instant now = Instant.parse("2026-11-21T12:00:00Z");
        Shift shift = shiftRepository.findById(DemoIds.SHIFT_ID).orElseThrow();
        Worker worker = workerRepository.findById(DemoIds.WORKER_SARAH).orElseThrow();

        ShiftAssignment duplicate = new ShiftAssignment(
                UUID.randomUUID(),
                shift,
                worker,
                ParticipationStatus.UNINVITED,
                AttendanceStatus.NOT_STARTED,
                now,
                now);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                    assignmentRepository.saveAndFlush(duplicate);
                }))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistenceRoundTripUpdatesExistingAssignmentOnly() {
        transactionTemplate.executeWithoutResult(status -> {
            ShiftAssignment a = assignmentRepository
                    .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_DANIEL)
                    .orElseThrow();
            a.applyState(
                    ParticipationStatus.INVITED,
                    AttendanceStatus.NOT_STARTED,
                    null,
                    null,
                    Instant.parse("2026-11-21T12:00:00Z"));
            assignmentRepository.saveAndFlush(a);
        });

        assertThat(assignmentRepository.findAllByShiftIdWithWorker(DemoIds.SHIFT_ID)).hasSize(3);

        ShiftAssignment reloaded = assignmentRepository
                .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_DANIEL)
                .orElseThrow();
        assertThat(reloaded.getParticipationStatus()).isEqualTo(ParticipationStatus.INVITED);

        // reset for other tests sharing the container
        transactionTemplate.executeWithoutResult(status -> {
            ShiftAssignment a = assignmentRepository
                    .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_DANIEL)
                    .orElseThrow();
            a.applyState(
                    ParticipationStatus.UNINVITED,
                    AttendanceStatus.NOT_STARTED,
                    null,
                    null,
                    Instant.parse("2026-11-01T12:00:00Z"));
            assignmentRepository.saveAndFlush(a);
        });
    }

    @Test
    void optimisticLockingRejectsStaleVersion() {
        ShiftAssignment first = transactionTemplate.execute(status -> assignmentRepository
                .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_ALEX)
                .orElseThrow());

        // Concurrent update bumps version
        transactionTemplate.executeWithoutResult(status -> {
            ShiftAssignment concurrent = assignmentRepository
                    .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_ALEX)
                    .orElseThrow();
            concurrent.applyState(
                    ParticipationStatus.INVITED,
                    AttendanceStatus.NOT_STARTED,
                    null,
                    null,
                    Instant.parse("2026-11-21T12:01:00Z"));
            assignmentRepository.saveAndFlush(concurrent);
        });

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                    // Detached entity with stale version
                    entityManager.detach(first);
                    first.applyState(
                            ParticipationStatus.INVITED,
                            AttendanceStatus.NOT_STARTED,
                            null,
                            null,
                            Instant.parse("2026-11-21T12:02:00Z"));
                    assignmentRepository.saveAndFlush(first);
                }))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // cleanup
        transactionTemplate.executeWithoutResult(status -> {
            ShiftAssignment a = assignmentRepository
                    .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_ALEX)
                    .orElseThrow();
            a.applyState(
                    ParticipationStatus.UNINVITED,
                    AttendanceStatus.NOT_STARTED,
                    null,
                    null,
                    Instant.parse("2026-11-01T12:00:00Z"));
            assignmentRepository.saveAndFlush(a);
        });
    }
}
