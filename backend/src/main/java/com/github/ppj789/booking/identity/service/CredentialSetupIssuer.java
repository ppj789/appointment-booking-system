package com.github.ppj789.booking.identity.service;

import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.CredentialSetupEntity;
import com.github.ppj789.booking.identity.persistence.CredentialSetupRepository;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.security.SecretHasher;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CredentialSetupIssuer {

    private final CredentialSetupRepository credentialSetupRepository;
    private final SetupTokenGenerator tokenGenerator;
    private final SecretHasher secretHasher;
    private final IdentitySecurityProperties properties;
    private final Clock clock;

    @Transactional
    public IssuedSetup issue(UserAccountEntity staffAccount) {
        if (staffAccount.getAccountType() != AccountType.STAFF
                || staffAccount.getAccountStatus() != AccountStatus.PENDING_SETUP) {
            throw new IllegalStateException("Only Staff waiting for setup can receive a setup capability");
        }

        Instant createdAt = clock.instant();
        Instant expiresAt = createdAt.plus(properties.credentialSetupExpiry());
        String rawToken = tokenGenerator.generate();
        credentialSetupRepository.save(CredentialSetupEntity.active(
                staffAccount,
                secretHasher.sha256(rawToken),
                createdAt,
                expiresAt));
        return new IssuedSetup(rawToken, expiresAt);
    }

    public record IssuedSetup(String rawToken, Instant expiresAt) {
    }
}
