package com.myfitness.api.common.config;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Per-process fixed windows. Full capacity fails closed; active clients are never evicted. */
@Component
public class AuthRateLimiter {
    private record Key(String address, boolean registration) {}
    private static final class Window {
        final long end;
        int attempts;
        Window(long end) { this.end = end; }
    }
    private final Map<Key, Window> windows = new HashMap<>();
    private final Clock clock;
    private final int loginLimit, registrationLimit, capacity;
    private final long durationMillis;
    private long nextCleanup;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthRateLimiter(
            @Value("${myfitness.auth-rate-limit.login-attempts:10}") int loginLimit,
            @Value("${myfitness.auth-rate-limit.registration-attempts:5}") int registrationLimit,
            @Value("${myfitness.auth-rate-limit.window-seconds:300}") long seconds,
            @Value("${myfitness.auth-rate-limit.max-entries:10000}") int capacity) {
        this(loginLimit, registrationLimit, seconds, capacity, Clock.systemUTC());
    }

    AuthRateLimiter(int loginLimit, int registrationLimit, long seconds, int capacity, Clock clock) {
        if (loginLimit < 1 || registrationLimit < 1 || seconds < 1 || seconds > 86400
                || capacity < 1 || capacity > 100000) {
            throw new IllegalArgumentException("Invalid authentication rate-limit configuration");
        }
        this.loginLimit = loginLimit;
        this.registrationLimit = registrationLimit;
        this.durationMillis = seconds * 1000;
        this.capacity = capacity;
        this.clock = clock;
    }

    /** Zero means allowed; otherwise seconds until retry. No credentials or addresses are logged. */
    public synchronized long retryAfter(String address, boolean registration) {
        long now = clock.millis();
        if (now >= nextCleanup) {
            windows.values().removeIf(window -> window.end <= now);
            nextCleanup = now + 1000;
        }
        Key key = new Key(address, registration);
        Window window = windows.get(key);
        if (window == null || window.end <= now) {
            if (window == null && windows.size() >= capacity) {
                return Math.max(1, (nextCleanup - now + 999) / 1000);
            }
            window = new Window(now + durationMillis);
            windows.put(key, window);
        }
        int limit = registration ? registrationLimit : loginLimit;
        if (window.attempts >= limit) return Math.max(1, (window.end - now + 999) / 1000);
        window.attempts++;
        return 0;
    }
}
