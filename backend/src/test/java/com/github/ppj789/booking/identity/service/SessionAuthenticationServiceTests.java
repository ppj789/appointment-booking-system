package com.github.ppj789.booking.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties.RateLimit;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.security.AccountUserDetails;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.security.FixedWindowRateLimiter;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class SessionAuthenticationServiceTests {

    @Test
    void canonicalisesEmailBeforeRateLimitingAndAuthentication() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        FixedWindowRateLimiter rateLimiter = mock(FixedWindowRateLimiter.class);
        RateLimit loginLimit = new RateLimit(10, Duration.ofMinutes(5));
        IdentitySecurityProperties properties = new IdentitySecurityProperties(
                Duration.ofHours(24),
                Duration.ofHours(8),
                loginLimit,
                new RateLimit(10, Duration.ofMinutes(5)));
        AuthenticatedAccountPrincipal principal = new AuthenticatedAccountPrincipal(
                UUID.randomUUID(),
                "Local Admin",
                "admin@example.test",
                AccountType.ADMIN,
                AccountStatus.ACTIVE,
                null);
        AccountUserDetails userDetails = new AccountUserDetails(principal, "encoded-password");
        Authentication verified = UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(verified);
        SessionAuthenticationService service = new SessionAuthenticationService(
                authenticationManager,
                rateLimiter,
                properties);

        Authentication result = service.authenticate(" ADMIN@EXAMPLE.TEST ", "password");

        verify(rateLimiter).requirePermit("login", "admin@example.test", loginLimit);
        ArgumentCaptor<Authentication> requestCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(requestCaptor.capture());
        assertEquals("admin@example.test", requestCaptor.getValue().getName());
        assertSame(principal, result.getPrincipal());
    }
}
