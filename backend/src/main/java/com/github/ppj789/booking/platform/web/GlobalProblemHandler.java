package com.github.ppj789.booking.platform.web;

import com.github.ppj789.booking.calendar.exception.AvailabilityBlockNotFoundException;
import com.github.ppj789.booking.calendar.exception.AvailabilityBlockNotRemovableException;
import com.github.ppj789.booking.calendar.exception.CalendarConflictException;
import com.github.ppj789.booking.calendar.exception.InvalidCalendarIntervalException;
import com.github.ppj789.booking.identity.exception.BranchUnavailableException;
import com.github.ppj789.booking.identity.exception.DuplicateStaffEmailException;
import com.github.ppj789.booking.identity.exception.InvalidAccountInputException;
import com.github.ppj789.booking.identity.exception.InvalidPasswordException;
import com.github.ppj789.booking.identity.exception.RateLimitExceededException;
import com.github.ppj789.booking.identity.exception.SetupUnavailableException;
import com.github.ppj789.booking.identity.exception.StaffDeactivationBlockedException;
import com.github.ppj789.booking.identity.exception.StaffSetupConflictException;
import com.github.ppj789.booking.identity.exception.StaffUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalProblemHandler {

    private static final String PROBLEM_TYPE_PREFIX = "urn:problem:appointment-booking:";

    private final Clock clock;

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> invalidCredentials(HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "Sign-in failed",
                "The email or password is incorrect.",
                "INVALID_CREDENTIALS",
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> accessDenied(HttpServletRequest request) {
        return response(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "The authenticated account cannot perform this operation.",
                "ACCESS_DENIED",
                request);
    }

    @ExceptionHandler({BranchUnavailableException.class, StaffUnavailableException.class})
    public ResponseEntity<ProblemDetail> administrationTargetUnavailable(HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "Administration target is unavailable",
                "The selected Branch or Staff account is unavailable.",
                "ADMINISTRATION_TARGET_UNAVAILABLE",
                request);
    }

    @ExceptionHandler(DuplicateStaffEmailException.class)
    public ResponseEntity<ProblemDetail> duplicateStaffEmail(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Staff email is unavailable",
                "A Staff account already uses this email address.",
                "STAFF_EMAIL_UNAVAILABLE",
                request);
    }

    @ExceptionHandler(StaffSetupConflictException.class)
    public ResponseEntity<ProblemDetail> staffSetupConflict(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Staff setup cannot be replaced",
                "Only Staff waiting for initial setup can receive a replacement link.",
                "STAFF_SETUP_CONFLICT",
                request);
    }

    @ExceptionHandler(StaffDeactivationBlockedException.class)
    public ResponseEntity<ProblemDetail> staffDeactivationBlocked(
            StaffDeactivationBlockedException exception,
            HttpServletRequest request) {
        ResponseEntity<ProblemDetail> response = response(
                HttpStatus.CONFLICT,
                "Staff deactivation is blocked",
                "Resolve the active customer work before deactivating this Staff account.",
                "STAFF_DEACTIVATION_BLOCKED",
                request);
        ProblemDetail problem = response.getBody();
        problem.setProperty("activeInvitation", exception.isActiveInvitation());
        problem.setProperty("futureAppointment", exception.isFutureAppointment());
        return response;
    }

    @ExceptionHandler(SetupUnavailableException.class)
    public ResponseEntity<ProblemDetail> setupUnavailable(HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "Account setup is unavailable",
                "Ask your Administrator for a new setup link.",
                "SETUP_UNAVAILABLE",
                request);
    }

    @ExceptionHandler(AvailabilityBlockNotFoundException.class)
    public ResponseEntity<ProblemDetail> availabilityBlockNotFound(HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "Availability Block is unavailable",
                "The selected Availability Block is unavailable.",
                "BLOCK_NOT_FOUND",
                request);
    }

    @ExceptionHandler(CalendarConflictException.class)
    public ResponseEntity<ProblemDetail> calendarConflict(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Calendar interval is unavailable",
                "The requested interval overlaps existing calendar occupancy.",
                "CALENDAR_CONFLICT",
                request);
    }

    @ExceptionHandler(AvailabilityBlockNotRemovableException.class)
    public ResponseEntity<ProblemDetail> availabilityBlockNotRemovable(
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Availability Block cannot be removed",
                "Only a future Availability Block can be removed.",
                "BLOCK_NOT_REMOVABLE",
                request);
    }

    @ExceptionHandler({
        InvalidAccountInputException.class,
        InvalidCalendarIntervalException.class,
        InvalidPasswordException.class,
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class
    })
    public ResponseEntity<ProblemDetail> validationFailed(HttpServletRequest request) {
        return response(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Request validation failed",
                "Correct the request and try again.",
                "VALIDATION_FAILED",
                request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> rateLimited(
            RateLimitExceededException exception,
            HttpServletRequest request) {
        long retryAfter = Math.max(1, Duration.between(clock.instant(), exception.getRetryAt()).toSeconds());
        ResponseEntity<ProblemDetail> response = response(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests",
                "Wait before trying again.",
                "RATE_LIMITED",
                request);
        return ResponseEntity.status(response.getStatusCode())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter))
                .body(response.getBody());
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String title,
            String detail,
            String code,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(PROBLEM_TYPE_PREFIX + code.toLowerCase().replace('_', '-')));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
