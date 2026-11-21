package com.oloodi.livestaffing.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.assignment.service.AssignmentActionService;
import com.oloodi.livestaffing.assignment.service.IllegalTransitionException;
import com.oloodi.livestaffing.common.error.GlobalExceptionHandler;
import com.oloodi.livestaffing.support.AbstractPostgresIntegrationTest;
import com.oloodi.livestaffing.support.DemoIds;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfigureMockMvc
@DisplayName("Phase 4: stale / concurrent conflict → 409")
class StaleConflictIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssignmentActionService actionService;

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void reset() {
        resetAssignment(DemoIds.ASSIGNMENT_ALEX);
    }

    private void resetAssignment(UUID id) {
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

    private String base(UUID assignmentId) {
        return "/api/shifts/" + DemoIds.SHIFT_ID + "/assignments/" + assignmentId;
    }

    @Test
    void staleCancelAfterCheckInReturns409AndLeavesCheckedIn() throws Exception {
        UUID id = DemoIds.ASSIGNMENT_ALEX;
        // Bring to ACCEPTED + NOT_STARTED
        mockMvc.perform(post(base(id) + "/invite")).andExpect(status().isOk());
        mockMvc.perform(post(base(id) + "/accept")).andExpect(status().isOk());

        // Session A: check-in
        mockMvc.perform(post(base(id) + "/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("CHECKED_IN"));

        // Session B (stale intent): cancel — illegal once checked in
        mockMvc.perform(post(base(id) + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists());

        // Authoritative state remains CHECKED_IN
        mockMvc.perform(post(base(id) + "/check-in")) // idempotent re-read via action
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("CHECKED_IN"))
                .andExpect(jsonPath("$.checkedInAt").isNotEmpty());
    }

    @Test
    void illegalTransitionViaServiceIsConflict() {
        assertThatThrownBy(() -> actionService.cancel(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_ALEX))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void optimisticLockRejectsStaleVersionWrite() {
        // @Version protects concurrent JPA writes even though REST clients do not send version.
        ShiftAssignment first = transactionTemplate.execute(status -> assignmentRepository
                .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_ALEX)
                .orElseThrow());

        transactionTemplate.executeWithoutResult(status -> {
            ShiftAssignment concurrent = assignmentRepository
                    .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_ALEX)
                    .orElseThrow();
            concurrent.applyState(
                    ParticipationStatus.INVITED,
                    AttendanceStatus.NOT_STARTED,
                    null,
                    null,
                    Instant.parse("2026-11-21T14:00:00Z"));
            assignmentRepository.saveAndFlush(concurrent);
        });

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                    entityManager.detach(first);
                    first.applyState(
                            ParticipationStatus.INVITED,
                            AttendanceStatus.NOT_STARTED,
                            null,
                            null,
                            Instant.parse("2026-11-21T14:01:00Z"));
                    assignmentRepository.saveAndFlush(first);
                }))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(GlobalExceptionHandler.OPTIMISTIC_LOCK_MESSAGE)
                .contains("changed by another session");
    }

    @Test
    void concurrentCheckInAndCancelOneSucceedsOneConflicts() throws Exception {
        UUID id = DemoIds.ASSIGNMENT_ALEX;
        actionService.invite(DemoIds.SHIFT_ID, id);
        actionService.accept(DemoIds.SHIFT_ID, id);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> checkInError = new AtomicReference<>();
        AtomicReference<Throwable> cancelError = new AtomicReference<>();

        Future<?> f1 = pool.submit(() -> {
            await(start);
            try {
                actionService.checkIn(DemoIds.SHIFT_ID, id);
            } catch (Throwable t) {
                checkInError.set(t);
            }
        });
        Future<?> f2 = pool.submit(() -> {
            await(start);
            try {
                actionService.cancel(DemoIds.SHIFT_ID, id);
            } catch (Throwable t) {
                cancelError.set(t);
            }
        });
        start.countDown();
        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        // Exactly one logical outcome: either cancel won (NOT_STARTED/CANCELLED) or check-in won.
        // The loser must have failed with IllegalTransition or optimistic lock — never silent overwrite.
        boolean checkInFailed = checkInError.get() != null;
        boolean cancelFailed = cancelError.get() != null;
        assertThat(checkInFailed || cancelFailed).isTrue();

        ShiftAssignment finalState = assignmentRepository
                .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, id)
                .orElseThrow();
        if (finalState.getAttendanceStatus() == AttendanceStatus.CHECKED_IN) {
            assertThat(finalState.getParticipationStatus()).isEqualTo(ParticipationStatus.ACCEPTED);
            assertThat(finalState.getCheckedInAt()).isNotNull();
            assertThat(cancelFailed).isTrue();
        } else {
            assertThat(finalState.getParticipationStatus()).isEqualTo(ParticipationStatus.CANCELLED);
            assertThat(finalState.getAttendanceStatus()).isEqualTo(AttendanceStatus.NOT_STARTED);
            assertThat(checkInFailed).isTrue();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("start latch timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
