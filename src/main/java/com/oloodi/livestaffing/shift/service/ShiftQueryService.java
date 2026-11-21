package com.oloodi.livestaffing.shift.service;

import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.assignment.dto.AssignmentResponse;
import com.oloodi.livestaffing.assignment.repository.ShiftAssignmentRepository;
import com.oloodi.livestaffing.common.error.ResourceNotFoundException;
import com.oloodi.livestaffing.shift.domain.Shift;
import com.oloodi.livestaffing.shift.dto.DashboardCounts;
import com.oloodi.livestaffing.shift.dto.DashboardResponse;
import com.oloodi.livestaffing.shift.dto.ShiftResponse;
import com.oloodi.livestaffing.shift.repository.ShiftRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShiftQueryService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final DashboardCountsCalculator countsCalculator;

    public ShiftQueryService(
            ShiftRepository shiftRepository,
            ShiftAssignmentRepository assignmentRepository,
            DashboardCountsCalculator countsCalculator) {
        this.shiftRepository = shiftRepository;
        this.assignmentRepository = assignmentRepository;
        this.countsCalculator = countsCalculator;
    }

    public DashboardResponse getDashboard(UUID shiftId) {
        Shift shift = requireShift(shiftId);
        List<ShiftAssignment> assignments = assignmentRepository.findAllByShiftIdWithWorker(shiftId);
        List<AssignmentResponse> assignmentResponses =
                assignments.stream().map(AssignmentResponse::from).toList();
        DashboardCounts counts = countsCalculator.calculate(shift.getRequiredWorkers(), assignments);
        return new DashboardResponse(ShiftResponse.from(shift), assignmentResponses, counts);
    }

    public List<AssignmentResponse> listAssignments(UUID shiftId) {
        requireShift(shiftId);
        return assignmentRepository.findAllByShiftIdWithWorker(shiftId).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    public AssignmentResponse getAssignment(UUID shiftId, UUID assignmentId) {
        requireShift(shiftId);
        ShiftAssignment assignment = assignmentRepository
                .findByShiftIdAndIdWithWorker(shiftId, assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found: " + assignmentId + " for shift " + shiftId));
        return AssignmentResponse.from(assignment);
    }

    private Shift requireShift(UUID shiftId) {
        return shiftRepository
                .findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + shiftId));
    }
}
