package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class IpRateLimiterTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final String IP = "203.0.113.10";

    @Test
    void givenFewerThanMaxRequests_whenConsumingWithinWindow_thenAllowed() {
        IpRateLimiter limiter = new IpRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC));

        boolean first = limiter.tryConsume(IP, 3, Duration.ofMinutes(1));
        boolean second = limiter.tryConsume(IP, 3, Duration.ofMinutes(1));

        assertThat(first).isTrue();
        assertThat(second).isTrue();
    }

    @Test
    void givenMoreThanMaxRequests_whenConsumingWithinWindow_thenRejectsExtras() {
        IpRateLimiter limiter = new IpRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC));

        limiter.tryConsume(IP, 2, Duration.ofMinutes(1));
        limiter.tryConsume(IP, 2, Duration.ofMinutes(1));
        boolean third = limiter.tryConsume(IP, 2, Duration.ofMinutes(1));

        assertThat(third).isFalse();
    }

    @Test
    void givenWindowExpired_whenConsumingAgain_thenCounterResets() {
        java.util.concurrent.atomic.AtomicReference<Instant> current =
                new java.util.concurrent.atomic.AtomicReference<>(NOW);
        Clock movingClock = new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return current.get();
            }
        };
        IpRateLimiter limiter = new IpRateLimiter(movingClock);
        limiter.tryConsume(IP, 1, Duration.ofMinutes(1));
        boolean blocked = limiter.tryConsume(IP, 1, Duration.ofMinutes(1));
        current.set(NOW.plus(Duration.ofMinutes(2)));

        boolean allowedAfterReset = limiter.tryConsume(IP, 1, Duration.ofMinutes(1));

        assertThat(blocked).isFalse();
        assertThat(allowedAfterReset).isTrue();
    }
}
