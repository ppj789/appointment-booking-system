package com.github.ppj789.booking.identity.service;

import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.security.AccountUserDetails;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.security.FixedWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final FixedWindowRateLimiter rateLimiter;
    private final IdentitySecurityProperties properties;

    public Authentication authenticate(String emailInput, String password) {
        String email;
        try {
            email = AccountInputNormalizer.email(emailInput);
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid credentials", exception);
        }
        rateLimiter.requirePermit("login", email, properties.loginRateLimit());

        Authentication verified = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        AccountUserDetails userDetails = (AccountUserDetails) verified.getPrincipal();
        AuthenticatedAccountPrincipal principal = userDetails.account();
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                verified.getAuthorities());
    }
}
