package com.github.ppj789.booking.platform.web;

import com.github.ppj789.booking.appointment.exception.AppointmentSlotOutsideHorizonException;
import com.github.ppj789.booking.appointment.exception.IdempotencyKeyReusedException;
import com.github.ppj789.booking.appointment.exception.InvalidAppointmentSlotException;
import com.github.ppj789.booking.appointment.exception.SlotUnavailableException;
import com.github.ppj789.booking.appointment.exception.UnsupportedAppointmentDurationException;
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
import com.github.ppj789.booking.invitation.exception.InvalidInvitationInputException;
import com.github.ppj789.booking.invitation.exception.InvalidGuestAvailabilityException;
import com.github.ppj789.booking.invitation.exception.InvitationNotActivatableException;
import com.github.ppj789.booking.invitation.exception.InvitationNotEditableException;
import com.github.ppj789.booking.invitation.exception.InvitationNotFoundException;
import com.github.ppj789.booking.invitation.exception.InvitationNotRevocableException;
import com.github.ppj789.booking.invitation.exception.InvitationUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalProblemHandler {

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

    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<ProblemDetail> invitationNotFound(HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "Invitation is unavailable",
                "The selected Invitation is unavailable.",
                "INVITATION_NOT_FOUND",
                request);
    }

    @ExceptionHandler(InvitationNotEditableException.class)
    public ResponseEntity<ProblemDetail> invitationNotEditable(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Invitation cannot be edited",
                "Only a draft Invitation can be edited.",
                "INVITATION_NOT_EDITABLE",
                request);
    }

    @ExceptionHandler(InvitationNotActivatableException.class)
    public ResponseEntity<ProblemDetail> invitationNotActivatable(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Invitation cannot be activated",
                "Only a draft Invitation can be activated.",
                "INVITATION_NOT_ACTIVATABLE",
                request);
    }

    @ExceptionHandler(InvitationNotRevocableException.class)
    public ResponseEntity<ProblemDetail> invitationNotRevocable(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Invitation cannot be revoked",
                "Only an active unused Invitation can be revoked.",
                "INVITATION_NOT_REVOCABLE",
                request);
    }

    @ExceptionHandler(InvitationUnavailableException.class)
    public ResponseEntity<ProblemDetail> invitationUnavailable(HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "Invitation is unavailable",
                "The Invitation cannot be used.",
                "INVITATION_UNAVAILABLE",
                request);
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<ProblemDetail> slotUnavailable(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Appointment time is unavailable",
                "Choose another appointment time and try again.",
                "SLOT_UNAVAILABLE",
                request);
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ResponseEntity<ProblemDetail> idempotencyKeyReused(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "Idempotency Key was already used",
                "Use the original request or provide a new Idempotency Key.",
                "IDEMPOTENCY_KEY_REUSED",
                request);
    }

    @ExceptionHandler(AppointmentSlotOutsideHorizonException.class)
    public ResponseEntity<ProblemDetail> appointmentOutsideHorizon(HttpServletRequest request) {
        return response(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Appointment time is outside the booking horizon",
                "Choose a future time that fits within the next 14 days.",
                "SLOT_OUTSIDE_HORIZON",
                request);
    }

    @ExceptionHandler(UnsupportedAppointmentDurationException.class)
    public ResponseEntity<ProblemDetail> unsupportedAppointmentDuration(
            HttpServletRequest request) {
        return response(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Appointment duration is unsupported",
                "Choose one of the supported Appointment durations.",
                "UNSUPPORTED_DURATION",
                request);
    }

    @ExceptionHandler(InvalidAppointmentSlotException.class)
    public ResponseEntity<ProblemDetail> invalidAppointmentSlot(HttpServletRequest request) {
        return response(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Appointment time is invalid",
                "Choose an Appointment time on the 15-minute UTC grid.",
                "INVALID_SLOT",
                request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> dataStoreUnavailable(
            DataAccessException exception,
            HttpServletRequest request) {
        logTechnicalFailure(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SERVICE_UNAVAILABLE",
                request,
                exception);
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service is temporarily unavailable",
                "Try the request again later.",
                "SERVICE_UNAVAILABLE",
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> malformedRequest(HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Request content is malformed",
                "Provide a valid JSON request body.",
                "MALFORMED_REQUEST",
                request);
    }

    @ExceptionHandler({
        MissingRequestHeaderException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> invalidRequest(HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Request is invalid",
                "Provide the required headers, parameters, and identifier formats.",
                "INVALID_REQUEST",
                request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> resourceNotFound(HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "The requested resource is unavailable.",
                "RESOURCE_NOT_FOUND",
                request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> methodNotAllowed(HttpServletRequest request) {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "The requested method is not supported for this resource.",
                "METHOD_NOT_ALLOWED",
                request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> mediaTypeNotSupported(HttpServletRequest request) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Media type is unsupported",
                "Use a supported Content-Type for this request.",
                "UNSUPPORTED_MEDIA_TYPE",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpectedFailure(
            Exception exception,
            HttpServletRequest request) {
        logTechnicalFailure(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                request,
                exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected service failure",
                "The request could not be completed.",
                "INTERNAL_SERVER_ERROR",
                request);
    }

    @ExceptionHandler({
        InvalidAccountInputException.class,
        InvalidCalendarIntervalException.class,
        InvalidGuestAvailabilityException.class,
        InvalidInvitationInputException.class,
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
        logKnownFailure(status, code, request);
        return problem(status, title, detail, code, request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            String code,
            HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiProblemDetails.create(status, title, detail, code, request));
    }

    private void logKnownFailure(
            HttpStatus status,
            String code,
            HttpServletRequest request) {
        if (status == HttpStatus.UNAUTHORIZED
                || status == HttpStatus.FORBIDDEN
                || status == HttpStatus.TOO_MANY_REQUESTS) {
            log.atWarn()
                    .addKeyValue("event", "http.request.rejected")
                    .addKeyValue("outcome", "rejected")
                    .addKeyValue("status", status.value())
                    .addKeyValue("errorCode", code)
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .log("Request rejected");
            return;
        }
        log.atInfo()
                .addKeyValue("event", "http.request.rejected")
                .addKeyValue("outcome", "rejected")
                .addKeyValue("status", status.value())
                .addKeyValue("errorCode", code)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .log("Request rejected");
    }

    private void logTechnicalFailure(
            HttpStatus status,
            String code,
            HttpServletRequest request,
            Exception exception) {
        log.atError()
                .setCause(exception)
                .addKeyValue("event", "http.request.failed")
                .addKeyValue("outcome", "failed")
                .addKeyValue("status", status.value())
                .addKeyValue("errorCode", code)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .log("Request failed");
    }
}
