package com.oloodi.livestaffing.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ShiftSseServiceTest {

    private ShiftSseService service;
    private ObjectMapper mapper;
    private UUID shiftId;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new ShiftSseService(mapper, new SseTimingHooks());
        shiftId = UUID.randomUUID();
    }

    @Test
    void subscribeRegistersAndCleanupRemovesEmitter() {
        SseEmitter emitter = service.subscribe(shiftId);
        assertThat(service.subscriberCount(shiftId)).isEqualTo(1);
        assertThat(service.activeShiftIds()).contains(shiftId);

        // Simulate completion callback (SseEmitter.complete() may not fire outside a request).
        service.removeEmitter(shiftId, emitter);
        assertThat(service.subscriberCount(shiftId)).isZero();
        assertThat(service.activeShiftIds()).doesNotContain(shiftId);
    }

    @Test
    void publishDeliversToHealthySubscribers() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger sends = new AtomicInteger();
        RecordingEmitter emitter = new RecordingEmitter(false, latch, sends, new CopyOnWriteArrayList<>());
        service.registerForTest(shiftId, emitter);

        AssignmentChangedEvent event = AssignmentChangedEvent.of(
                shiftId,
                UUID.fromString("33333333-3333-3333-3333-333333333301"),
                "invite",
                Instant.parse("2026-11-21T13:00:00Z"));

        // Event JSON shape (authoritative payload content)
        String json = mapper.writeValueAsString(event);
        assertThat(json)
                .contains("\"type\":\"ASSIGNMENT_CHANGED\"")
                .contains("\"action\":\"invite\"")
                .contains("33333333-3333-3333-3333-333333333301");

        service.publish(event);
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(sends.get()).isEqualTo(1);
        assertThat(service.subscriberCount(shiftId)).isEqualTo(1);
    }

    @Test
    void failedEmitterDoesNotBreakOthers() throws Exception {
        CountDownLatch goodLatch = new CountDownLatch(1);
        AtomicInteger goodSends = new AtomicInteger();
        RecordingEmitter broken =
                new RecordingEmitter(true, new CountDownLatch(0), new AtomicInteger(), new CopyOnWriteArrayList<>());
        RecordingEmitter good =
                new RecordingEmitter(false, goodLatch, goodSends, new CopyOnWriteArrayList<>());
        service.registerForTest(shiftId, broken);
        service.registerForTest(shiftId, good);
        assertThat(service.subscriberCount(shiftId)).isEqualTo(2);

        AssignmentChangedEvent event = AssignmentChangedEvent.of(
                shiftId, UUID.randomUUID(), "accept", Instant.parse("2026-11-21T13:00:00Z"));

        assertThatCode(() -> service.publish(event)).doesNotThrowAnyException();
        assertThat(goodLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(goodSends.get()).isEqualTo(1);
        assertThat(service.subscriberCount(shiftId)).isEqualTo(1);
    }

    @Test
    void heartbeatIsolatesBrokenEmitters() {
        RecordingEmitter broken =
                new RecordingEmitter(true, new CountDownLatch(0), new AtomicInteger(), new CopyOnWriteArrayList<>());
        RecordingEmitter good =
                new RecordingEmitter(false, new CountDownLatch(0), new AtomicInteger(), new CopyOnWriteArrayList<>());
        service.registerForTest(shiftId, broken);
        service.registerForTest(shiftId, good);

        assertThatCode(() -> service.heartbeat(shiftId)).doesNotThrowAnyException();
        assertThat(service.subscriberCount(shiftId)).isEqualTo(1);
    }

    static final class RecordingEmitter extends SseEmitter {
        private final boolean fail;
        private final CountDownLatch latch;
        private final AtomicInteger sends;
        private final List<String> ignored;

        RecordingEmitter(boolean fail, CountDownLatch latch, AtomicInteger sends, List<String> ignored) {
            super(0L);
            this.fail = fail;
            this.latch = latch;
            this.sends = sends;
            this.ignored = ignored;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (fail) {
                throw new IOException("broken emitter");
            }
            // Touch builder so Spring's contract is exercised
            ignored.add(String.valueOf(builder.build().size()));
            sends.incrementAndGet();
            latch.countDown();
        }
    }
}
