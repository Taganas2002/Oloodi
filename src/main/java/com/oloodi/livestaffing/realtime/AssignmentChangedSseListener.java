package com.oloodi.livestaffing.realtime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards assignment-change domain events to SSE subscribers only after a
 * successful transaction commit. Failed / rolled-back mutations never notify.
 */
@Component
public class AssignmentChangedSseListener {

    private final ShiftSseService shiftSseService;

    public AssignmentChangedSseListener(ShiftSseService shiftSseService) {
        this.shiftSseService = shiftSseService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssignmentChanged(AssignmentChangedEvent event) {
        shiftSseService.publish(event);
    }
}
