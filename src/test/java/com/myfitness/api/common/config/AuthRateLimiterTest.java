package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {
    private final AtomicLong now = new AtomicLong(1000000);
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now.get()); }
    };
    @Test void limitsAndRecoversWithoutExtendingBlockedWindow() {
        var limiter = new AuthRateLimiter(2, 1, 10, 10, clock);
        assertThat(limiter.retryAfter("a", false)).isZero();
        assertThat(limiter.retryAfter("a", false)).isZero();
        assertThat(limiter.retryAfter("a", false)).isEqualTo(10);
        now.addAndGet(9999);
        assertThat(limiter.retryAfter("a", false)).isEqualTo(1);
        now.incrementAndGet();
        assertThat(limiter.retryAfter("a", false)).isZero();
    }
    @Test void isolatesClientsAndOperations() {
        var limiter = new AuthRateLimiter(1, 1, 10, 10, clock);
        assertThat(limiter.retryAfter("a", false)).isZero();
        assertThat(limiter.retryAfter("b", false)).isZero();
        assertThat(limiter.retryAfter("a", true)).isZero();
        assertThat(limiter.retryAfter("a", true)).isPositive();
    }
    @Test void capacityFailsClosedWithoutEvictingActiveClientsAndExpires() {
        var limiter = new AuthRateLimiter(1, 1, 10, 1, clock);
        assertThat(limiter.retryAfter("a", false)).isZero();
        assertThat(limiter.retryAfter("b", false)).isPositive();
        assertThat(limiter.retryAfter("a", false)).isPositive();
        now.addAndGet(10000);
        assertThat(limiter.retryAfter("b", false)).isZero();
    }
    @Test void concurrentRequestsDoNotExceedLimit() throws Exception {
        var limiter = new AuthRateLimiter(3, 1, 10, 10, clock);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            var futures = new java.util.ArrayList<Future<Long>>();
            for (int i = 0; i < 32; i++) futures.add(executor.submit(() -> limiter.retryAfter("same", false)));
            int allowed = 0;
            for (var future : futures) if (future.get(5, TimeUnit.SECONDS) == 0) allowed++;
            assertThat(allowed).isEqualTo(3);
        } finally { executor.shutdownNow(); }
    }
    @Test void invalidConfigurationFailsStartup() {
        assertThatThrownBy(() -> new AuthRateLimiter(0, 1, 10, 1, clock)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthRateLimiter(1, 1, 0, 1, clock)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthRateLimiter(1, 1, 10, 100001, clock)).isInstanceOf(IllegalArgumentException.class);
    }
}
