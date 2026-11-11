package com.github.ppj789.booking.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class RequestObservabilityFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ATTRIBUTE =
            RequestObservabilityFilter.class.getName() + ".correlationId";
    public static final String CORRELATION_MDC_KEY = "correlationId";

    private final CorrelationIdGenerator correlationIdGenerator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = correlationIdGenerator.resolve(
                request.getHeader(CORRELATION_HEADER));
        request.setAttribute(CORRELATION_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        long startedAt = System.nanoTime();
        boolean failed = false;

        MDC.put(CORRELATION_MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failed = true;
            log.atError()
                    .setCause(exception)
                    .addKeyValue("event", "http.request.failed")
                    .addKeyValue("outcome", "failed")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .log("Unhandled request failure");
            throw exception;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.atInfo()
                    .addKeyValue("event", "http.request.completed")
                    .addKeyValue("outcome", outcome(response.getStatus(), failed))
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("durationMs", durationMs)
                    .log("HTTP request completed");
            MDC.remove(CORRELATION_MDC_KEY);
        }
    }

    public static String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CORRELATION_ATTRIBUTE);
        return value instanceof String correlationId ? correlationId : "unavailable";
    }

    private static String outcome(int status, boolean failed) {
        if (failed || status >= 500) {
            return "failed";
        }
        return status >= 400 ? "rejected" : "success";
    }
}
