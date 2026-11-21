package com.oloodi.livestaffing.assignment.service;

import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.dto.AssignmentResponse;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.common.error.ResourceNotFoundException;
import com.oloodi.livestaffing.realtime.AssignmentChangedEvent;
import com.oloodi.livestaffing.shift.repository.ShiftRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentActionService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final AssignmentTransitionService transitionService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AssignmentActionService(
            ShiftRepository shiftRepository,
            ShiftAssignmentRepository assignmentRepository,
            AssignmentTransitionService transitionService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.shiftRepository = shiftRepository;
        this.assignmentRepository = assignmentRepository;
        this.transitionService = transitionService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public AssignmentResponse invite(UUID shiftId, UUID assignmentId) {
        return mutate(shiftId, assignmentId, "invite", transitionService::invite);
    }

    @Transactional
    public AssignmentResponse accept(UUID shiftId, UUID assignmentId) {
        return mutate(shiftId, assignmentId, "accept", transitionService::accept);
    }

    @Transactional
    public AssignmentResponse decline(UUID shiftId, UUID assignmentId) {
        return mutate(shiftId, assignmentId, "decline", transitionService::decline);
    }

    @Transactional
    public AssignmentResponse cancel(UUID shiftId, UUID assignmentId) {
        return mutate(shiftId, assignmentId, "cancel", transitionService::cancel);
    }

    @Transactional
    public AssignmentResponse checkIn(UUID shiftId, UUID assignmentId) {
        return mutate(shiftId, assignmentId, "check-in", transitionService::checkIn);
    }

    @Transactional
    public AssignmentResponse checkOut(UUID shiftId, UUID assignmentId) {
        return mutate(shiftId, assignmentId, "check-out", transitionService::checkOut);
    }

    private AssignmentResponse mutate(
            UUID shiftId,
            UUID assignmentId,
            String action,
            UnaryOperator<ShiftAssignment> transition) {
        requireShift(shiftId);
        ShiftAssignment assignment = assignmentRepository
                .findByShiftIdAndIdWithWorker(shiftId, assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found: " + assignmentId + " for shift " + shiftId));
        ShiftAssignment updated = transition.apply(assignment);
        assignmentRepository.save(updated);
        // Published inside the TX; AFTER_COMMIT listener delivers SSE only if commit succeeds.
        eventPublisher.publishEvent(AssignmentChangedEvent.of(
                shiftId, assignmentId, action, Instant.now(clock).truncatedTo(ChronoUnit.MICROS)));
        return AssignmentResponse.from(updated);
    }

    private void requireShift(UUID shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new ResourceNotFoundException("Shift not found: " + shiftId);
        }
    }
}
