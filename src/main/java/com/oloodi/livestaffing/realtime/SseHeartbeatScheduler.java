package com.oloodi.livestaffing.realtime;

import java.util.UUID;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Heartbeat decision (Phase 4): KEEP.
 *
 * <p>Rationale: idle SSE connections are often closed by reverse proxies / load balancers /
 * browsers when no bytes flow. A ~20s <em>comment-only</em> heartbeat keeps the TCP stream
 * alive and lets {@link ShiftSseService} prune dead emitters on send failure — without
 * emitting {@code ASSIGNMENT_CHANGED} (so clients must not refetch on heartbeat).
 *
 * <p>Logs stay quiet: failures go through emitter removal; no INFO per tick.
 */
@Component
@EnableScheduling
public class SseHeartbeatScheduler {

    private final ShiftSseService shiftSseService;

    public SseHeartbeatScheduler(ShiftSseService shiftSseService) {
        this.shiftSseService = shiftSseService;
    }

    @Scheduled(fixedRateString = "${app.sse.heartbeat-ms:20000}")
    public void heartbeat() {
        for (UUID shiftId : shiftSseService.activeShiftIds()) {
            shiftSseService.heartbeat(shiftId);
        }
    }
}
