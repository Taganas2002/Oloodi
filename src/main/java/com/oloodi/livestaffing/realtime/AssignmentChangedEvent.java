package com.oloodi.livestaffing.realtime;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification that an assignment changed after a successful commit.
 * Payload is intentionally small — clients must refetch via REST.
 */
public record AssignmentChangedEvent(
        String type,
        UUID shiftId,
        UUID assignmentId,
        String action,
        Instant occurredAt) {

    public static final String TYPE = "ASSIGNMENT_CHANGED";

    public static AssignmentChangedEvent of(UUID shiftId, UUID assignmentId, String action, Instant occurredAt) {
        return new AssignmentChangedEvent(TYPE, shiftId, assignmentId, action, occurredAt);
    }
}
