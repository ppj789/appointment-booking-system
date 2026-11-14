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

    @Test
    void evictsTheOldestActiveWindowInsteadOfRejectingANewCaller() {
        Instant start = Instant.parse("2026-11-08T08:00:00Z");
        Clock clock = Clock.fixed(start, ZoneOffset.UTC);
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock, new SecretHasher());
        RateLimit limit = new RateLimit(1, Duration.ofMinutes(5));

        for (int index = 0; index < FixedWindowRateLimiter.MAXIMUM_WINDOWS_PER_SCOPE; index++) {
            limiter.requirePermit("guest-source", "source-" + index, limit);
        }

        assertDoesNotThrow(() -> limiter.requirePermit("guest-source", "overflow", limit));
        assertDoesNotThrow(() -> limiter.requirePermit("guest-source", "source-0", limit));
    }

    @Test
    void keepsCapacityIndependentBetweenScopes() {
        Instant start = Instant.parse("2026-11-08T08:00:00Z");
        Clock clock = Clock.fixed(start, ZoneOffset.UTC);
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock, new SecretHasher());
        RateLimit limit = new RateLimit(1, Duration.ofMinutes(5));

        limiter.requirePermit("login", "existing-login", limit);
        for (int index = 0; index <= FixedWindowRateLimiter.MAXIMUM_WINDOWS_PER_SCOPE; index++) {
            limiter.requirePermit("account-setup", "setup-" + index, limit);
        }

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> limiter.requirePermit("login", "existing-login", limit));
        assertEquals(start.plus(Duration.ofMinutes(5)), exception.getRetryAt());
    }

    @Test
    void reclaimsExpiredWindowsBeforeEvictingActiveOnes() {
        Instant start = Instant.parse("2026-11-08T08:00:00Z");
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(start);
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock, new SecretHasher());
        RateLimit activeLimit = new RateLimit(1, Duration.ofMinutes(5));

        limiter.requirePermit("login", "oldest-active", activeLimit);
        limiter.requirePermit("login", "short-lived", new RateLimit(1, Duration.ofMinutes(1)));
        for (int index = 2; index < FixedWindowRateLimiter.MAXIMUM_WINDOWS_PER_SCOPE; index++) {
            limiter.requirePermit("login", "active-" + index, activeLimit);
        }

        when(clock.instant()).thenReturn(start.plus(Duration.ofMinutes(2)));
        assertDoesNotThrow(() -> limiter.requirePermit("login", "new-caller", activeLimit));
        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> limiter.requirePermit("login", "oldest-active", activeLimit));
        assertEquals(start.plus(Duration.ofMinutes(5)), exception.getRetryAt());
    }
}
