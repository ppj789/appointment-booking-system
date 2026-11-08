package com.github.ppj789.booking.platform.web;

import com.github.ppj789.booking.identity.exception.InvalidPasswordException;
import com.github.ppj789.booking.identity.exception.RateLimitExceededException;
import com.github.ppj789.booking.identity.exception.SetupUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(SetupUnavailableException.class)
    public ResponseEntity<ProblemDetail> setupUnavailable(HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "Account setup is unavailable",
                "Ask your Administrator for a new setup link.",
                "SETUP_UNAVAILABLE",
                request);
    }

    @ExceptionHandler({InvalidPasswordException.class, MethodArgumentNotValidException.class})
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
