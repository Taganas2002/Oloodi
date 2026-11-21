package com.oloodi.livestaffing.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Thread-safe per-shift SSE emitter registry. Broken emitters are removed without
 * failing the mutation path or blocking sibling emitters.
 */
@Service
public class ShiftSseService {

    public static final String EVENT_NAME = "assignment-changed";
    /** 30 minutes; heartbeats keep the connection useful sooner. */
    public static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private static final Logger log = LoggerFactory.getLogger(ShiftSseService.class);

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByShift = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final SseTimingHooks timingHooks;

    public ShiftSseService(ObjectMapper objectMapper, SseTimingHooks timingHooks) {
        this.objectMapper = objectMapper;
        this.timingHooks = timingHooks;
    }

    public SseEmitter subscribe(UUID shiftId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        CopyOnWriteArrayList<SseEmitter> emitters =
                emittersByShift.computeIfAbsent(shiftId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        log.info("SSE connect shiftId={} subscribers={}", shiftId, emitters.size());

        Runnable remove = () -> removeEmitter(shiftId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> {
            log.info("SSE timeout shiftId={}", shiftId);
            remove.run();
        });
        emitter.onError(ex -> {
            // Client disconnect / broken pipe is normal for EventSource teardown.
            if (isClientDisconnect(ex)) {
                log.debug("SSE client disconnect shiftId={} reason={}", shiftId, safeReason(ex));
            } else {
                log.info("SSE error shiftId={} reason={}", shiftId, safeReason(ex));
            }
            remove.run();
        });

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ex) {
            if (isClientDisconnect(ex)) {
                log.debug("SSE connect-send failed shiftId={} reason={}", shiftId, safeReason(ex));
            } else {
                log.info("SSE connect-send failed shiftId={} reason={}", shiftId, safeReason(ex));
            }
            remove.run();
        }
        return emitter;
    }

    public void publish(AssignmentChangedEvent event) {
        List<SseEmitter> emitters = emittersByShift.get(event.shiftId());
        if (emitters == null || emitters.isEmpty()) {
            timingHooks.recordPublish(0);
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.warn("SSE serialize failed shiftId={} reason={}", event.shiftId(), safeReason(ex));
            return;
        }

        int subscribers = emitters.size();
        timingHooks.recordPublish(subscribers);
        log.info(
                "SSE publish shiftId={} assignmentId={} action={} subscribers={}",
                event.shiftId(),
                event.assignmentId(),
                event.action(),
                subscribers);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(EVENT_NAME)
                        .id(event.assignmentId() + ":" + event.occurredAt().toEpochMilli())
                        .data(json));
            } catch (Exception ex) {
                timingHooks.recordDeliveryFailure();
                log.info(
                        "SSE delivery fail / stale remove shiftId={} assignmentId={} reason={}",
                        event.shiftId(),
                        event.assignmentId(),
                        safeReason(ex));
                removeEmitter(event.shiftId(), emitter);
                try {
                    emitter.completeWithError(ex);
                } catch (Exception ignored) {
                    // already dead
                }
            }
        }
    }

    /** Comment-only heartbeat — clients must not treat this as an assignment change. */
    public void heartbeat(UUID shiftId) {
        List<SseEmitter> emitters = emittersByShift.get(shiftId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception ex) {
                // Heartbeat against a closed tab → IOException / Broken pipe; drop quietly.
                timingHooks.recordDeliveryFailure();
                removeEmitter(shiftId, emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // already dead
                }
            }
        }
    }

    public int subscriberCount(UUID shiftId) {
        List<SseEmitter> emitters = emittersByShift.get(shiftId);
        return emitters == null ? 0 : emitters.size();
    }

    public Set<UUID> activeShiftIds() {
        return Set.copyOf(emittersByShift.keySet());
    }

    /** Test-only registration of a prebuilt emitter into the registry. */
    void registerForTest(UUID shiftId, SseEmitter emitter) {
        emittersByShift.computeIfAbsent(shiftId, id -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    void removeEmitter(UUID shiftId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByShift.get(shiftId);
        if (emitters == null) {
            return;
        }
        if (emitters.remove(emitter)) {
            log.info("SSE disconnect shiftId={} subscribers={}", shiftId, emitters.size());
        }
        if (emitters.isEmpty()) {
            emittersByShift.remove(shiftId, emitters);
        }
    }

    private static boolean isClientDisconnect(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof IOException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name.contains("ClientAbortException") || name.contains("AsyncRequestNotUsable")) {
                return true;
            }
        }
        return false;
    }

    private static String safeReason(Throwable ex) {
        String msg = ex.getClass().getSimpleName();
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            // Avoid dumping bodies / secrets — class + short message only.
            String detail = ex.getMessage();
            if (detail.length() > 120) {
                detail = detail.substring(0, 120);
            }
            return msg + ": " + detail;
        }
        return msg;
    }
}
