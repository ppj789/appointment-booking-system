package com.github.ppj789.booking.platform.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.ppj789.booking.identity.exception.RateLimitExceededException;
import com.github.ppj789.booking.identity.exception.StaffDeactivationBlockedException;
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
        MockHttpServletRequest request = request("POST", "/api/v1/sessions");

        ResponseEntity<ProblemDetail> response = handler.invalidCredentials(request);
        ProblemDetail problem = response.getBody();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(URI.create("urn:problem:appointment-booking:invalid-credentials"), problem.getType());
        assertEquals("Sign-in failed", problem.getTitle());
        assertEquals(401, problem.getStatus());
        assertEquals("The email or password is incorrect.", problem.getDetail());
        assertEquals(URI.create("/api/v1/sessions"), problem.getInstance());
        assertEquals("INVALID_CREDENTIALS", problem.getProperties().get("code"));
        assertEquals(
                "7c1ad3d0-b0b3-42ed-903b-e131bc8cb6b1",
                problem.getProperties().get("correlationId"));
        assertFalse(problem.getDetail().contains("admin@example.test"));
    }

    @Test
    void returnsRetryAfterWithRateLimitProblem() {
        MockHttpServletRequest request = request("POST", "/api/v1/sessions");

        ResponseEntity<ProblemDetail> response = handler.rateLimited(
                new RateLimitExceededException(NOW.plusSeconds(30)),
                request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("30", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("RATE_LIMITED", response.getBody().getProperties().get("code"));
    }

    @Test
    void returnsStableAdministrationProblemsWithoutGuestDetails() {
        MockHttpServletRequest request = request(
                "POST",
                "/api/v1/admin/staff/example/deactivate");

        ResponseEntity<ProblemDetail> denied = handler.accessDenied(request);
        ResponseEntity<ProblemDetail> unavailable = handler.administrationTargetUnavailable(request);
        ResponseEntity<ProblemDetail> duplicate = handler.duplicateStaffEmail(request);
        ResponseEntity<ProblemDetail> setupConflict = handler.staffSetupConflict(request);
        ResponseEntity<ProblemDetail> setupUnavailable = handler.setupUnavailable(request);
        ResponseEntity<ProblemDetail> validationFailed = handler.validationFailed(request);
        ResponseEntity<ProblemDetail> blocked = handler.staffDeactivationBlocked(
                new StaffDeactivationBlockedException(true, false),
                request);

        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, unavailable.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, setupConflict.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, setupUnavailable.getStatusCode());
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, validationFailed.getStatusCode());
        assertEquals("STAFF_DEACTIVATION_BLOCKED", blocked.getBody().getProperties().get("code"));
        assertEquals(true, blocked.getBody().getProperties().get("activeInvitation"));
        assertEquals(false, blocked.getBody().getProperties().get("futureAppointment"));
    }

    @Test
    void returnsStableCalendarProblemsWithoutOccupancyDetails() {
        MockHttpServletRequest request = request(
                "POST",
                "/api/v1/me/availability-blocks");

        ResponseEntity<ProblemDetail> conflict = handler.calendarConflict(request);
        ResponseEntity<ProblemDetail> unavailable = handler.availabilityBlockNotFound(request);
        ResponseEntity<ProblemDetail> notRemovable =
                handler.availabilityBlockNotRemovable(request);

        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertEquals("CALENDAR_CONFLICT", conflict.getBody().getProperties().get("code"));
        assertFalse(conflict.getBody().getDetail().contains("Guest"));
        assertEquals(HttpStatus.NOT_FOUND, unavailable.getStatusCode());
        assertEquals("BLOCK_NOT_FOUND", unavailable.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.CONFLICT, notRemovable.getStatusCode());
        assertEquals(
                "BLOCK_NOT_REMOVABLE",
                notRemovable.getBody().getProperties().get("code"));
    }

    @Test
    void returnsStableAppointmentProblemsWithoutBookingDetails() {
        MockHttpServletRequest request = request(
                "POST",
                "/api/v1/guest/appointments");

        ResponseEntity<ProblemDetail> unavailable = handler.slotUnavailable(request);
        ResponseEntity<ProblemDetail> reused = handler.idempotencyKeyReused(request);
        ResponseEntity<ProblemDetail> horizon = handler.appointmentOutsideHorizon(request);
        ResponseEntity<ProblemDetail> duration = handler.unsupportedAppointmentDuration(request);
        ResponseEntity<ProblemDetail> invalidSlot = handler.invalidAppointmentSlot(request);

        assertEquals(HttpStatus.CONFLICT, unavailable.getStatusCode());
        assertEquals("SLOT_UNAVAILABLE", unavailable.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.CONFLICT, reused.getStatusCode());
        assertEquals("IDEMPOTENCY_KEY_REUSED", reused.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, horizon.getStatusCode());
        assertEquals("SLOT_OUTSIDE_HORIZON", horizon.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, duration.getStatusCode());
        assertEquals("UNSUPPORTED_DURATION", duration.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, invalidSlot.getStatusCode());
        assertEquals("INVALID_SLOT", invalidSlot.getBody().getProperties().get("code"));
        assertFalse(unavailable.getBody().getDetail().contains("Alex Guest"));
    }

    @Test
    void mapsFrameworkRequestFailuresToStableProblemCodes() {
        MockHttpServletRequest request = request("POST", "/api/v1/guest/appointments");

        ResponseEntity<ProblemDetail> malformed = handler.malformedRequest(request);
        ResponseEntity<ProblemDetail> invalid = handler.invalidRequest(request);
        ResponseEntity<ProblemDetail> missing = handler.resourceNotFound(request);
        ResponseEntity<ProblemDetail> method = handler.methodNotAllowed(request);
        ResponseEntity<ProblemDetail> media = handler.mediaTypeNotSupported(request);

        assertEquals(HttpStatus.BAD_REQUEST, malformed.getStatusCode());
        assertEquals("MALFORMED_REQUEST", malformed.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        assertEquals("INVALID_REQUEST", invalid.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", missing.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, method.getStatusCode());
        assertEquals("METHOD_NOT_ALLOWED", method.getBody().getProperties().get("code"));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, media.getStatusCode());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", media.getBody().getProperties().get("code"));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setAttribute(
                RequestObservabilityFilter.CORRELATION_ATTRIBUTE,
                "7c1ad3d0-b0b3-42ed-903b-e131bc8cb6b1");
        return request;
    }

}
