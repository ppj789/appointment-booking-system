package com.github.ppj789.booking.identity.security;

import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class AbsoluteSessionLifetimeFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_AT_ATTRIBUTE = "booking.authenticated-at";

    private final IdentitySecurityProperties properties;
    private final Clock clock;
    private final LogoutHandler logoutHandler;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AUTHENTICATED_AT_ATTRIBUTE) instanceof Instant authenticatedAt) {
            Instant expiresAt = authenticatedAt.plus(properties.absoluteSessionTimeout());
            if (!clock.instant().isBefore(expiresAt)) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                logoutHandler.logout(request, response, authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
