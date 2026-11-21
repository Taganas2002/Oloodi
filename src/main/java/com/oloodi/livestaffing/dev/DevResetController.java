package com.oloodi.livestaffing.dev;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test/demo/local-only deterministic reset. Never register this in production profiles.
 */
@RestController
@RequestMapping("/api/dev")
@Profile({"local", "demo", "test"})
@ConditionalOnProperty(name = "app.dev-reset.enabled", havingValue = "true", matchIfMissing = true)
public class DevResetController {

    private final DevResetService devResetService;

    public DevResetController(DevResetService devResetService) {
        this.devResetService = devResetService;
    }

    @PostMapping(path = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> reset() {
        int updated = devResetService.resetDemoAssignments();
        return Map.of(
                "status", "ok",
                "updatedAssignments", updated,
                "shiftId", DevResetService.SHIFT_ID.toString());
    }
}
