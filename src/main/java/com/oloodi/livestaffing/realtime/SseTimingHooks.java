package com.oloodi.livestaffing.realtime;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Lightweight counters/timings for later Phase 6 latency analysis.
 * No performance claims in Phase 4 — instrumentation only.
 */
@Component
public class SseTimingHooks {

    private final AtomicLong publishCount = new AtomicLong();
    private final AtomicLong deliveryFailureCount = new AtomicLong();
    private final AtomicLong lastPublishEpochMs = new AtomicLong();
    private final AtomicLong lastDeliveryFailureEpochMs = new AtomicLong();
    private final AtomicLong lastPublishSubscriberCount = new AtomicLong();

    public void recordPublish(int subscriberCount) {
        publishCount.incrementAndGet();
        lastPublishEpochMs.set(System.currentTimeMillis());
        lastPublishSubscriberCount.set(subscriberCount);
    }

    public void recordDeliveryFailure() {
        deliveryFailureCount.incrementAndGet();
        lastDeliveryFailureEpochMs.set(System.currentTimeMillis());
    }

    public long publishCount() {
        return publishCount.get();
    }

    public long deliveryFailureCount() {
        return deliveryFailureCount.get();
    }

    public long lastPublishEpochMs() {
        return lastPublishEpochMs.get();
    }

    public long lastPublishSubscriberCount() {
        return lastPublishSubscriberCount.get();
    }
}
