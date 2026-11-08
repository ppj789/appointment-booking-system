package com.github.ppj789.booking.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties.RateLimit;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.CredentialSetupEntity;
import com.github.ppj789.booking.identity.persistence.CredentialSetupRepository;
import com.github.ppj789.booking.identity.persistence.CredentialSetupStatus;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.security.SecretHasher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CredentialSetupIssuerTests {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    @Test
    void persistsOnlyTheTokenHashWithTheConfiguredExpiry() {
        CredentialSetupRepository repository = mock(CredentialSetupRepository.class);
        SetupTokenGenerator tokenGenerator = mock(SetupTokenGenerator.class);
        SecretHasher secretHasher = mock(SecretHasher.class);
        UserAccountEntity staff = pendingStaff();
        when(tokenGenerator.generate()).thenReturn("raw-token");
        when(secretHasher.sha256("raw-token")).thenReturn("a".repeat(64));
        CredentialSetupIssuer issuer = new CredentialSetupIssuer(
                repository,
                tokenGenerator,
                secretHasher,
                properties(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        CredentialSetupIssuer.IssuedSetup result = issuer.issue(staff);

        ArgumentCaptor<CredentialSetupEntity> setupCaptor =
                ArgumentCaptor.forClass(CredentialSetupEntity.class);
        verify(repository).save(setupCaptor.capture());
        CredentialSetupEntity persisted = setupCaptor.getValue();
        assertSame(staff, persisted.getUserAccount());
        assertEquals("a".repeat(64), persisted.getTokenHash());
        assertEquals(CredentialSetupStatus.ACTIVE, persisted.getStatus());
        assertEquals(NOW, persisted.getCreatedAt());
        assertEquals(NOW.plus(Duration.ofHours(24)), persisted.getExpiresAt());
        assertEquals("raw-token", result.rawToken());
        assertEquals(persisted.getExpiresAt(), result.expiresAt());
    }

    @Test
    void rejectsStaffThatAlreadyCompletedSetup() {
        UserAccountEntity staff = pendingStaff();
        staff.activate("{bcrypt}encoded-password", NOW);
        CredentialSetupIssuer issuer = new CredentialSetupIssuer(
                mock(CredentialSetupRepository.class),
                mock(SetupTokenGenerator.class),
                mock(SecretHasher.class),
                properties(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> issuer.issue(staff));

        assertEquals("Only Staff waiting for setup can receive a setup capability", exception.getMessage());
        assertEquals(AccountStatus.ACTIVE, staff.getAccountStatus());
    }

    private UserAccountEntity pendingStaff() {
        BranchEntity branch = BranchEntity.demo(
                UUID.randomUUID(),
                "JHB-DEMO",
                "Johannesburg Demo Branch",
                "1 Demo Street, Johannesburg",
                "Africa/Johannesburg");
        return UserAccountEntity.pendingStaff(
                branch,
                "Demo Staff",
                "staff@example.test",
                NOW.minusSeconds(60));
    }

    private IdentitySecurityProperties properties() {
        RateLimit defaultLimit = new RateLimit(10, Duration.ofMinutes(5));
        return new IdentitySecurityProperties(
                Duration.ofHours(24),
                Duration.ofHours(8),
                defaultLimit,
                defaultLimit,
                new RateLimit(20, Duration.ofMinutes(5)));
    }
}
