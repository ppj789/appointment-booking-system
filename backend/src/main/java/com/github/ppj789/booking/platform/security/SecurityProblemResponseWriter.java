package com.github.ppj789.booking.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ppj789.booking.platform.web.ApiProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public void writeAuthenticationRequired(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                "Sign in before performing this operation.",
                "AUTHENTICATION_REQUIRED");
    }

    public void writeAccessDenied(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "Access denied",
                "The authenticated account cannot perform this operation.",
                "ACCESS_DENIED");
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail,
            String code) throws IOException {
        log.atWarn()
                .addKeyValue("event", "http.request.rejected")
                .addKeyValue("outcome", "rejected")
                .addKeyValue("errorCode", code)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .log("Security request rejected");
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiProblemDetails.responseBody(
                ApiProblemDetails.create(status, title, detail, code, request)));
    }
}
