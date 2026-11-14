package com.github.ppj789.booking.identity.security;

import com.github.ppj789.booking.identity.config.IdentitySecurityProperties.RateLimit;
import com.github.ppj789.booking.identity.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FixedWindowRateLimiter {

    static final int MAXIMUM_WINDOWS_PER_SCOPE = 10_000;

    private final Clock clock;
    private final SecretHasher secretHasher;
    private final Map<String, LinkedHashMap<String, Window>> windowsByScope = new HashMap<>();

    public synchronized void requirePermit(String scope, String untrustedKey, RateLimit limit) {
        Instant now = clock.instant();
        String key = secretHasher.sha256(untrustedKey);
        LinkedHashMap<String, Window> windows = windowsByScope.computeIfAbsent(
                scope,
                ignored -> new LinkedHashMap<>());
        Window current = windows.get(key);
        if (current != null && !now.isBefore(current.expiresAt())) {
            windows.remove(key);
            current = null;
        }
        if (current == null && windows.size() >= MAXIMUM_WINDOWS_PER_SCOPE) {
            windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
            if (windows.size() >= MAXIMUM_WINDOWS_PER_SCOPE) {
                evictOldestWindow(windows);
            }
        }
        Window updated = current == null
                ? new Window(1, now.plus(limit.window()))
                : new Window(current.attempts() + 1, current.expiresAt());
        windows.put(key, updated);
        if (updated.attempts() > limit.attempts()) {
            throw new RateLimitExceededException(updated.expiresAt());
        }
    }

    private void evictOldestWindow(LinkedHashMap<String, Window> windows) {
        String oldestKey = windows.keySet().iterator().next();
        windows.remove(oldestKey);
    }

    private record Window(int attempts, Instant expiresAt) {
    }
}
