package com.oloodi.livestaffing.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class DemoShiftScheduleTest {

    @Test
    void windowUsesMontrealCalendarDayAtNineToFive() {
        // 2026-11-21 15:30 UTC → still 2026-11-21 10:30 America/Montreal
        Clock clock = Clock.fixed(Instant.parse("2026-11-21T15:30:00Z"), ZoneId.of("UTC"));

        DemoShiftSchedule.Window window = DemoShiftSchedule.windowFor(clock);

        assertThat(window.startsAt()).isEqualTo(Instant.parse("2026-11-21T14:00:00Z"));
        assertThat(window.endsAt()).isEqualTo(Instant.parse("2026-11-21T22:00:00Z"));
    }

    @Test
    void windowFollowsMontrealDateAcrossUtcMidnight() {
        // 2026-11-22 03:00 UTC → 2026-11-21 22:00 America/Montreal
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T03:00:00Z"), ZoneId.of("UTC"));

        DemoShiftSchedule.Window window = DemoShiftSchedule.windowFor(clock);

        assertThat(window.startsAt()).isEqualTo(Instant.parse("2026-11-21T14:00:00Z"));
        assertThat(window.endsAt()).isEqualTo(Instant.parse("2026-11-21T22:00:00Z"));
    }
}
