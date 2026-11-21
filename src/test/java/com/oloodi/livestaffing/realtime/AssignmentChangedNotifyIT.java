package com.oloodi.livestaffing.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.assignment.service.AssignmentActionService;
import com.oloodi.livestaffing.assignment.service.IllegalTransitionException;
import com.oloodi.livestaffing.support.AbstractPostgresIntegrationTest;
import com.oloodi.livestaffing.support.DemoIds;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AssignmentChangedNotifyIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private AssignmentActionService actionService;

    @Autowired
    private ShiftSseService shiftSseService;

    @Autowired
    private ShiftAssignmentRepository assignmentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void resetAssignments() {
        reset(DemoIds.ASSIGNMENT_SARAH);
        reset(DemoIds.ASSIGNMENT_DANIEL);
        reset(DemoIds.ASSIGNMENT_ALEX);
    }

    private void reset(UUID assignmentId) {
        transactionTemplate.executeWithoutResult(status -> {
            ShiftAssignment a = assignmentRepository
                    .findByShiftIdAndIdWithWorker(DemoIds.SHIFT_ID, assignmentId)
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
    void successfulMutationPublishesAfterCommit() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger sends = new AtomicInteger();
        ShiftSseServiceTest.RecordingEmitter emitter =
                new ShiftSseServiceTest.RecordingEmitter(
                        false, latch, sends, new CopyOnWriteArrayList<>());
        shiftSseService.registerForTest(DemoIds.SHIFT_ID, emitter);

        actionService.invite(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_SARAH);

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(sends.get()).isEqualTo(1);
    }

    @Test
    void illegalMutationDoesNotPublishSuccessEvent() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger sends = new AtomicInteger();
        ShiftSseServiceTest.RecordingEmitter emitter =
                new ShiftSseServiceTest.RecordingEmitter(
                        false, latch, sends, new CopyOnWriteArrayList<>());
        shiftSseService.registerForTest(DemoIds.SHIFT_ID, emitter);

        assertThatThrownBy(() -> actionService.accept(DemoIds.SHIFT_ID, DemoIds.ASSIGNMENT_SARAH))
                .isInstanceOf(IllegalTransitionException.class);

        assertThat(latch.await(300, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(sends.get()).isZero();
    }

    @Test
    void subscribeViaServiceTracksCleanup() {
        int before = shiftSseService.subscriberCount(DemoIds.SHIFT_ID);
        SseEmitter emitter = shiftSseService.subscribe(DemoIds.SHIFT_ID);
        assertThat(shiftSseService.subscriberCount(DemoIds.SHIFT_ID)).isEqualTo(before + 1);
        shiftSseService.removeEmitter(DemoIds.SHIFT_ID, emitter);
        assertThat(shiftSseService.subscriberCount(DemoIds.SHIFT_ID)).isEqualTo(before);
    }
}
