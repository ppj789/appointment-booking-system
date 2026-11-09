package com.github.ppj789.booking.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class UnhandledExceptionLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            logUnexpectedFailure(request, exception);
            throw exception;
        }
    }

    private void logUnexpectedFailure(
            HttpServletRequest request,
            Exception exception) {
        log.error(
                "Unhandled request failure method={} path={} exception={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getName(),
                exception);
    }
}
