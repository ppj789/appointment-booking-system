package com.github.ppj789.booking.identity.security;

import com.github.ppj789.booking.identity.config.IdentitySecurityProperties.RateLimit;
import com.github.ppj789.booking.identity.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FixedWindowRateLimiter {

    static final int MAXIMUM_WINDOWS = 10_000;

    private final Clock clock;
    private final SecretHasher secretHasher;
    private final Map<String, Window> windows = new HashMap<>();

    public synchronized void requirePermit(String scope, String untrustedKey, RateLimit limit) {
        Instant now = clock.instant();
        String key = scope + ':' + secretHasher.sha256(untrustedKey);
        if (!windows.containsKey(key) && windows.size() >= MAXIMUM_WINDOWS) {
            windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
            if (windows.size() >= MAXIMUM_WINDOWS) {
                Instant retryAt = windows.values().stream()
                        .map(Window::expiresAt)
                        .min(Instant::compareTo)
                        .orElse(now.plus(limit.window()));
                throw new RateLimitExceededException(retryAt);
            }
        }
        Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.expiresAt())) {
                return new Window(1, now.plus(limit.window()));
            }
            return new Window(current.attempts() + 1, current.expiresAt());
        });
        if (updated.attempts() > limit.attempts()) {
            throw new RateLimitExceededException(updated.expiresAt());
        }
    }

    private record Window(int attempts, Instant expiresAt) {
    }
}
