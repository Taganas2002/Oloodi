package com.oloodi.livestaffing.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oloodi.livestaffing.assignment.domain.AttendanceStatus;
import com.oloodi.livestaffing.assignment.domain.ParticipationStatus;
import com.oloodi.livestaffing.assignment.domain.ShiftAssignment;
import com.oloodi.livestaffing.shift.domain.Shift;
import com.oloodi.livestaffing.worker.domain.Worker;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssignmentTransitionServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-21T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-21T12:05:00Z");
    private static final Instant T2 = Instant.parse("2026-11-21T12:10:00Z");

    private MutableClock clock;
    private AssignmentTransitionService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(T0);
        service = new AssignmentTransitionService(clock);
    }

    @Nested
    @DisplayName("A. Legal transitions")
    class LegalTransitions {

        @Test
        void uninvitedToInvited() {
            ShiftAssignment a = uninvited();
            service.invite(a);
            assertState(a, ParticipationStatus.INVITED, AttendanceStatus.NOT_STARTED);
        }

        @Test
        void invitedToAccepted() {
            ShiftAssignment a = invited();
            service.accept(a);
            assertState(a, ParticipationStatus.ACCEPTED, AttendanceStatus.NOT_STARTED);
        }

        @Test
        void invitedToDeclined() {
            ShiftAssignment a = invited();
            service.decline(a);
            assertState(a, ParticipationStatus.DECLINED, AttendanceStatus.NOT_STARTED);
        }

        @Test
        void acceptedNotStartedToCancelled() {
            ShiftAssignment a = accepted();
            service.cancel(a);
            assertState(a, ParticipationStatus.CANCELLED, AttendanceStatus.NOT_STARTED);
        }

        @Test
        void cancelledNotStartedToAccepted() {
            ShiftAssignment a = cancelled();
            service.accept(a);
            assertState(a, ParticipationStatus.ACCEPTED, AttendanceStatus.NOT_STARTED);
        }

        @Test
        void acceptedNotStartedToCheckedIn() {
            ShiftAssignment a = accepted();
            clock.setInstant(T1);
            service.checkIn(a);
            assertState(a, ParticipationStatus.ACCEPTED, AttendanceStatus.CHECKED_IN);
            assertThat(a.getCheckedInAt()).isEqualTo(T1);
            assertThat(a.getCheckedOutAt()).isNull();
        }

        @Test
        void acceptedCheckedInToCheckedOut() {
            ShiftAssignment a = accepted();
            clock.setInstant(T1);
            service.checkIn(a);
            clock.setInstant(T2);
            service.checkOut(a);
            assertState(a, ParticipationStatus.ACCEPTED, AttendanceStatus.CHECKED_OUT);
            assertThat(a.getCheckedInAt()).isEqualTo(T1);
            assertThat(a.getCheckedOutAt()).isEqualTo(T2);
        }
    }

    @Nested
    @DisplayName("B. Illegal transitions")
    class IllegalTransitions {

        @Test
        void uninvitedToAccepted() {
            assertIllegal(uninvited(), service::accept, "accept");
        }

        @Test
        void uninvitedToCheckedIn() {
            assertIllegal(uninvited(), service::checkIn, "check-in");
        }

        @Test
        void declinedToCheckedIn() {
            ShiftAssignment a = invited();
            service.decline(a);
            assertIllegal(a, service::checkIn, "check-in");
        }

        @Test
        void notStartedToCheckedOut() {
            assertIllegal(accepted(), service::checkOut, "check-out");
        }

        @Test
        void checkedOutToCheckedIn() {
            ShiftAssignment a = checkedOut();
            assertIllegal(a, service::checkIn, "check-in");
        }

        @Test
        void checkedInToCancelled() {
            ShiftAssignment a = accepted();
            service.checkIn(a);
            assertIllegal(a, service::cancel, "cancel");
        }

        @Test
        void checkedOutToCancelled() {
            assertIllegal(checkedOut(), service::cancel, "cancel");
        }
    }

    @Nested
    @DisplayName("C. Idempotency")
    class Idempotency {

        @Test
        void inviteTwiceKeepsInvited() {
            ShiftAssignment a = uninvited();
            service.invite(a);
            Instant updated = a.getUpdatedAt();
            long versionBefore = a.getVersion();
            service.invite(a);
            assertState(a, ParticipationStatus.INVITED, AttendanceStatus.NOT_STARTED);
            assertThat(a.getUpdatedAt()).isEqualTo(updated);
            assertThat(a.getVersion()).isEqualTo(versionBefore);
        }

        @Test
        void checkInTwiceKeepsTimestamp() {
            ShiftAssignment a = accepted();
            clock.setInstant(T1);
            service.checkIn(a);
            Instant first = a.getCheckedInAt();
            clock.setInstant(T2);
            service.checkIn(a);
            assertThat(a.getCheckedInAt()).isEqualTo(first).isEqualTo(T1);
            assertState(a, ParticipationStatus.ACCEPTED, AttendanceStatus.CHECKED_IN);
        }

        @Test
        void checkOutTwiceKeepsTimestamp() {
            ShiftAssignment a = accepted();
            clock.setInstant(T1);
            service.checkIn(a);
            clock.setInstant(T2);
            service.checkOut(a);
            Instant firstOut = a.getCheckedOutAt();
            clock.setInstant(Instant.parse("2026-11-21T13:00:00Z"));
            service.checkOut(a);
            assertThat(a.getCheckedOutAt()).isEqualTo(firstOut).isEqualTo(T2);
            assertState(a, ParticipationStatus.ACCEPTED, AttendanceStatus.CHECKED_OUT);
        }
    }

    private void assertIllegal(
            ShiftAssignment assignment,
            java.util.function.Consumer<ShiftAssignment> action,
            String expectedAction) {
        assertThatThrownBy(() -> action.accept(assignment))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining(expectedAction);
    }

    private static void assertState(
            ShiftAssignment a, ParticipationStatus p, AttendanceStatus attendance) {
        assertThat(a.getParticipationStatus()).isEqualTo(p);
        assertThat(a.getAttendanceStatus()).isEqualTo(attendance);
    }

    private ShiftAssignment uninvited() {
        return base(ParticipationStatus.UNINVITED, AttendanceStatus.NOT_STARTED);
    }

    private ShiftAssignment invited() {
        return base(ParticipationStatus.INVITED, AttendanceStatus.NOT_STARTED);
    }

    private ShiftAssignment accepted() {
        return base(ParticipationStatus.ACCEPTED, AttendanceStatus.NOT_STARTED);
    }

    private ShiftAssignment cancelled() {
        return base(ParticipationStatus.CANCELLED, AttendanceStatus.NOT_STARTED);
    }

    private ShiftAssignment checkedOut() {
        ShiftAssignment a = accepted();
        clock.setInstant(T1);
        service.checkIn(a);
        clock.setInstant(T2);
        service.checkOut(a);
        return a;
    }

    private ShiftAssignment base(ParticipationStatus p, AttendanceStatus a) {
        Instant now = Instant.now(clock);
        Shift shift = new Shift(
                UUID.randomUUID(),
                "Test",
                now,
                now.plusSeconds(3600),
                "Montréal, QC",
                3,
                now,
                now);
        Worker worker = new Worker(UUID.randomUUID(), "Test Worker", now);
        return new ShiftAssignment(UUID.randomUUID(), shift, worker, p, a, now, now);
    }

    /** Simple mutable clock for controlling Instant.now in unit tests. */
    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
