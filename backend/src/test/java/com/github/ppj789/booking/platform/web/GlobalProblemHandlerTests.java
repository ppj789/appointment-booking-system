package com.github.ppj789.booking.platform.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.ppj789.booking.identity.exception.RateLimitExceededException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalProblemHandlerTests {

    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");

    private final GlobalProblemHandler handler =
            new GlobalProblemHandler(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void returnsStableRfc9457AuthenticationProblemWithoutSensitiveDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/sessions");

        ResponseEntity<ProblemDetail> response = handler.invalidCredentials(request);
        ProblemDetail problem = response.getBody();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(URI.create("urn:problem:appointment-booking:invalid-credentials"), problem.getType());
        assertEquals("Sign-in failed", problem.getTitle());
        assertEquals(401, problem.getStatus());
        assertEquals("The email or password is incorrect.", problem.getDetail());
        assertEquals(URI.create("/api/v1/sessions"), problem.getInstance());
        assertEquals("INVALID_CREDENTIALS", problem.getProperties().get("code"));
        assertFalse(problem.getDetail().contains("admin@example.test"));
    }

    @Test
    void returnsRetryAfterWithRateLimitProblem() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/sessions");

        ResponseEntity<ProblemDetail> response = handler.rateLimited(
                new RateLimitExceededException(NOW.plusSeconds(30)),
                request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("30", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("RATE_LIMITED", response.getBody().getProperties().get("code"));
    }
}
