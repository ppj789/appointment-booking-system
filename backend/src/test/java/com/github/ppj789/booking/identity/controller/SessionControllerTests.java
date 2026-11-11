package com.github.ppj789.booking.identity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.service.SessionAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

class SessionControllerTests {

    @Test
    void currentSessionReturnsOnlyTheFrontendAccountContextWithoutCaching() {
        SessionController controller = controller(mock(LogoutHandler.class));
        AuthenticatedAccountPrincipal principal = new AuthenticatedAccountPrincipal(
                UUID.randomUUID(),
                "Local Admin",
                "admin@example.test",
                AccountType.ADMIN,
                AccountStatus.ACTIVE,
                null);

        ResponseEntity<AccountContextResponse> response = controller.current(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(CacheControl.noStore().getHeaderValue(), response.getHeaders().getCacheControl());
        AccountContextResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Local Admin", body.account().displayName());
        assertEquals(AccountType.ADMIN, body.account().accountType());
        assertNull(body.branch());
    }

    @Test
    void logoutRemainsSuccessfulWithoutAnAuthenticatedPrincipal() {
        LogoutHandler logoutHandler = mock(LogoutHandler.class);
        SessionController controller = controller(logoutHandler);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        ResponseEntity<Void> result = controller.deleteCurrent(null, request, response);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(logoutHandler).logout(request, response, null);
    }

    private SessionController controller(LogoutHandler logoutHandler) {
        return new SessionController(
                mock(SessionAuthenticationService.class),
                mock(SessionAuthenticationStrategy.class),
                mock(SecurityContextRepository.class),
                logoutHandler,
                Clock.fixed(Instant.parse("2026-11-10T20:00:00Z"), ZoneOffset.UTC));
    }
}
