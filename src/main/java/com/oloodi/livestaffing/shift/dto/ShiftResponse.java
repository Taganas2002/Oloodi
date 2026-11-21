package com.oloodi.livestaffing.shift.dto;

import com.oloodi.livestaffing.shift.domain.Shift;
import java.time.Instant;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        String title,
        Instant startsAt,
        Instant endsAt,
        String location,
        int requiredWorkers,
        Instant createdAt,
        Instant updatedAt) {

    public static ShiftResponse from(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getTitle(),
                shift.getStartsAt(),
                shift.getEndsAt(),
                shift.getLocation(),
                shift.getRequiredWorkers(),
                shift.getCreatedAt(),
                shift.getUpdatedAt());
    }
}
