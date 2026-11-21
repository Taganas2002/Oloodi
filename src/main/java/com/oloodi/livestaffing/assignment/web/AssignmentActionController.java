package com.oloodi.livestaffing.assignment.web;

import com.oloodi.livestaffing.assignment.dto.AssignmentResponse;
import com.oloodi.livestaffing.assignment.service.AssignmentActionService;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts/{shiftId}/assignments/{assignmentId}")
public class AssignmentActionController {

    private final AssignmentActionService assignmentActionService;

    public AssignmentActionController(AssignmentActionService assignmentActionService) {
        this.assignmentActionService = assignmentActionService;
    }

    @PostMapping("/invite")
    public AssignmentResponse invite(
            @PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        return assignmentActionService.invite(shiftId, assignmentId);
    }

    @PostMapping("/accept")
    public AssignmentResponse accept(
            @PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        return assignmentActionService.accept(shiftId, assignmentId);
    }

    @PostMapping("/decline")
    public AssignmentResponse decline(
            @PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        return assignmentActionService.decline(shiftId, assignmentId);
    }

    @PostMapping("/cancel")
    public AssignmentResponse cancel(
            @PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        return assignmentActionService.cancel(shiftId, assignmentId);
    }

    @PostMapping("/check-in")
    public AssignmentResponse checkIn(
            @PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        return assignmentActionService.checkIn(shiftId, assignmentId);
    }

    @PostMapping("/check-out")
    public AssignmentResponse checkOut(
            @PathVariable UUID shiftId, @PathVariable UUID assignmentId) {
        return assignmentActionService.checkOut(shiftId, assignmentId);
    }
}
