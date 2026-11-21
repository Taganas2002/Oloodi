package com.oloodi.livestaffing.shift.web;

import com.oloodi.livestaffing.assignment.dto.AssignmentResponse;
import com.oloodi.livestaffing.shift.dto.DashboardResponse;
import com.oloodi.livestaffing.shift.service.ShiftQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftQueryService shiftQueryService;

    public ShiftController(ShiftQueryService shiftQueryService) {
        this.shiftQueryService = shiftQueryService;
    }

    @GetMapping("/{shiftId}/dashboard")
    public DashboardResponse dashboard(@PathVariable UUID shiftId) {
        return shiftQueryService.getDashboard(shiftId);
    }

    @GetMapping("/{shiftId}/assignments")
    public List<AssignmentResponse> listAssignments(@PathVariable UUID shiftId) {
        return shiftQueryService.listAssignments(shiftId);
    }

    @GetMapping("/{shiftId}/assignments/{assignmentId}")
    public AssignmentResponse getAssignment(
            @PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        return shiftQueryService.getAssignment(shiftId, assignmentId);
    }
}
