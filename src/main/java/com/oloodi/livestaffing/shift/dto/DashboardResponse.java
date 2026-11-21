package com.oloodi.livestaffing.shift.dto;

import com.oloodi.livestaffing.assignment.dto.AssignmentResponse;
import java.util.List;

public record DashboardResponse(
        ShiftResponse shift, List<AssignmentResponse> assignments, DashboardCounts counts) {
}
