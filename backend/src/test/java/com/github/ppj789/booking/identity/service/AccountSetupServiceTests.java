package com.github.ppj789.booking.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties.RateLimit;
import com.github.ppj789.booking.identity.exception.SetupUnavailableException;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.CredentialSetupEntity;
import com.github.ppj789.booking.identity.persistence.CredentialSetupRepository;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.security.FixedWindowRateLimiter;
import com.github.ppj789.booking.identity.security.SecretHasher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountSetupServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");

    @Test
    void activatesPendingStaffAndConsumesUsableSetup() {
        CredentialSetupRepository repository = mock(CredentialSetupRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SecretHasher secretHasher = mock(SecretHasher.class);
        FixedWindowRateLimiter rateLimiter = mock(FixedWindowRateLimiter.class);
        CredentialSetupEntity setup = mock(CredentialSetupEntity.class);
        UserAccountEntity account = mock(UserAccountEntity.class);
        IdentitySecurityProperties properties = properties();
        when(secretHasher.sha256("raw-token")).thenReturn("token-hash");
        when(repository.findByTokenHash("token-hash")).thenReturn(Optional.of(setup));
        when(setup.isUsableAt(NOW)).thenReturn(true);
        when(setup.getUserAccount()).thenReturn(account);
        when(account.getAccountType()).thenReturn(AccountType.STAFF);
        when(account.getAccountStatus()).thenReturn(AccountStatus.PENDING_SETUP);
        when(passwordEncoder.encode("valid-password")).thenReturn("encoded-password");
        AccountSetupService service = new AccountSetupService(
                repository,
                passwordEncoder,
                secretHasher,
                rateLimiter,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));

        service.complete("raw-token", "valid-password");

        verify(rateLimiter).requirePermit("account-setup", "raw-token", properties.setupRateLimit());
        verify(account).activate("encoded-password", NOW);
        verify(setup).consume(NOW);
    }

    @Test
    void returnsTheSameUnavailableFailureForUnknownOrExpiredSetup() {
        CredentialSetupRepository repository = mock(CredentialSetupRepository.class);
        SecretHasher secretHasher = mock(SecretHasher.class);
        CredentialSetupEntity expiredSetup = mock(CredentialSetupEntity.class);
        when(secretHasher.sha256("unknown-token")).thenReturn("unknown-hash");
        when(secretHasher.sha256("expired-token")).thenReturn("expired-hash");
        when(repository.findByTokenHash("unknown-hash")).thenReturn(Optional.empty());
        when(repository.findByTokenHash("expired-hash")).thenReturn(Optional.of(expiredSetup));
        when(expiredSetup.isUsableAt(NOW)).thenReturn(false);
        AccountSetupService service = new AccountSetupService(
                repository,
                mock(PasswordEncoder.class),
                secretHasher,
                mock(FixedWindowRateLimiter.class),
                properties(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        SetupUnavailableException unknownSetup = assertThrows(
                SetupUnavailableException.class,
                () -> service.complete("unknown-token", "valid-password"));
        SetupUnavailableException expiredSetupFailure = assertThrows(
                SetupUnavailableException.class,
                () -> service.complete("expired-token", "valid-password"));

        assertEquals("Account setup is unavailable", unknownSetup.getMessage());
        assertEquals(unknownSetup.getMessage(), expiredSetupFailure.getMessage());
    }

    private IdentitySecurityProperties properties() {
        return new IdentitySecurityProperties(
                Duration.ofHours(24),
                Duration.ofHours(8),
                new RateLimit(10, Duration.ofMinutes(5)),
                new RateLimit(10, Duration.ofMinutes(5)),
                new RateLimit(10, Duration.ofMinutes(5)),
                new RateLimit(30, Duration.ofMinutes(5)),
                new RateLimit(120, Duration.ofMinutes(1)),
                new RateLimit(10, Duration.ofMinutes(1)));
    }
}
