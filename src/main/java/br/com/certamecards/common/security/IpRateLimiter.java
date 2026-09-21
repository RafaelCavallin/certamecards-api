package br.com.certamecards.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class IpRateLimiter {

    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public IpRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryConsume(String key, int maxRequests, Duration window) {
        Instant now = clock.instant();
        Window current = windows.compute(key, (k, existing) -> refresh(existing, now, window));
        return current.count.incrementAndGet() <= maxRequests;
    }

    private Window refresh(Window existing, Instant now, Duration window) {
        if (existing == null || existing.resetAt.isBefore(now)) {
            return new Window(now.plus(window));
        }
        return existing;
    }

    private static final class Window {
        private final Instant resetAt;
        private final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();

        private Window(Instant resetAt) {
            this.resetAt = resetAt;
        }
    }
}
