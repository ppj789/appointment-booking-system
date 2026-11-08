package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.security.AbsoluteSessionLifetimeFilter;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.service.SessionAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Server-managed authentication sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionAuthenticationService authenticationService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final LogoutHandler logoutHandler;
    private final Clock clock;

    @PostMapping
    @Operation(summary = "Sign in", description = "Authenticates an active Admin or Staff account.")
    @ApiResponse(responseCode = "200", description = "Session established")
    @ApiResponse(responseCode = "401", description = "Email or password is incorrect")
    @ApiResponse(responseCode = "429", description = "Too many attempts")
    public ResponseEntity<AccountContextResponse> create(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = authenticationService.authenticate(
                loginRequest.email(), loginRequest.password());
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        request.getSession().setAttribute(
                AbsoluteSessionLifetimeFilter.AUTHENTICATED_AT_ATTRIBUTE,
                clock.instant());

        AuthenticatedAccountPrincipal principal =
                (AuthenticatedAccountPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(AccountContextResponse.from(principal));
    }

    @DeleteMapping("/current")
    @Operation(summary = "Sign out", description = "Invalidates the current server-managed session.")
    @SecurityRequirement(name = "sessionCookie")
    @ApiResponse(responseCode = "204", description = "Session ended")
    public ResponseEntity<Void> deleteCurrent(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        logoutHandler.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
}
