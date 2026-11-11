package com.myfitness.api.common.health;

import java.util.Map;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal process/traffic probes. No database queries, internals, or management APIs. */
@RestController
public class HealthController {
    private final ApplicationAvailability availability;

    public HealthController(ApplicationAvailability availability) {
        this.availability = availability;
    }

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, String>> live() {
        return response(availability.getLivenessState() == LivenessState.CORRECT);
    }

    @GetMapping({"/health", "/health/ready"})
    public ResponseEntity<Map<String, String>> ready() {
        return response(availability.getLivenessState() == LivenessState.CORRECT
                && availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC);
    }

    private ResponseEntity<Map<String, String>> response(boolean up) {
        return ResponseEntity.status(up ? 200 : 503).cacheControl(CacheControl.noStore())
                .body(Map.of("status", up ? "UP" : "DOWN"));
    }
}
