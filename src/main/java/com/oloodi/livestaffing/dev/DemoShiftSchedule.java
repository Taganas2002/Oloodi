package com.oloodi.livestaffing.dev;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Demo shift window: current calendar day in America/Montreal, 09:00–17:00.
 * Centralized so reset and tests share one deterministic rule (inject a fixed {@link Clock}).
 */
public final class DemoShiftSchedule {

    public static final ZoneId DEMO_ZONE = ZoneId.of("America/Montreal");
    public static final LocalTime START = LocalTime.of(9, 0);
    public static final LocalTime END = LocalTime.of(17, 0);

    private DemoShiftSchedule() {
    }

    public record Window(Instant startsAt, Instant endsAt) {
    }

    public static Window windowFor(Clock clock) {
        LocalDate day = LocalDate.now(clock.withZone(DEMO_ZONE));
        Instant startsAt = day.atTime(START).atZone(DEMO_ZONE).toInstant();
        Instant endsAt = day.atTime(END).atZone(DEMO_ZONE).toInstant();
        return new Window(startsAt, endsAt);
    }
}
