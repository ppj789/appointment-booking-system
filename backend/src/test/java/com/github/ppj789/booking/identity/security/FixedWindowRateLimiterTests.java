package com.github.ppj789.booking.identity.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.identity.config.IdentitySecurityProperties.RateLimit;
import com.github.ppj789.booking.identity.exception.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTests {

    @Test
    void rejectsAttemptsAboveTheLimitAndAllowsTheNextWindow() {
        Instant start = Instant.parse("2026-11-08T08:00:00Z");
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(start, start, start, start.plus(Duration.ofMinutes(5)));
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock, new SecretHasher());
        RateLimit limit = new RateLimit(2, Duration.ofMinutes(5));

        assertDoesNotThrow(() -> limiter.requirePermit("login", "admin@example.test", limit));
        assertDoesNotThrow(() -> limiter.requirePermit("login", "admin@example.test", limit));
        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> limiter.requirePermit("login", "admin@example.test", limit));
        assertEquals(start.plus(Duration.ofMinutes(5)), exception.getRetryAt());
        assertDoesNotThrow(() -> limiter.requirePermit("login", "admin@example.test", limit));
    }

    @Test
    void keepsIndependentCountersForDifferentScopes() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-08T08:00:00Z"), ZoneOffset.UTC);
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock, new SecretHasher());
        RateLimit limit = new RateLimit(1, Duration.ofMinutes(5));

        assertDoesNotThrow(() -> limiter.requirePermit("login", "shared-value", limit));
        assertDoesNotThrow(() -> limiter.requirePermit("account-setup", "shared-value", limit));
    }
}
