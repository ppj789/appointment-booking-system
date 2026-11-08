package com.github.ppj789.booking.identity.service;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.exception.SetupUnavailableException;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.CredentialSetupEntity;
import com.github.ppj789.booking.identity.persistence.CredentialSetupRepository;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.security.FixedWindowRateLimiter;
import com.github.ppj789.booking.identity.security.SecretHasher;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountSetupService {

    private final CredentialSetupRepository credentialSetupRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretHasher secretHasher;
    private final FixedWindowRateLimiter rateLimiter;
    private final IdentitySecurityProperties properties;
    private final Clock clock;

    @Transactional
    public AuthenticatedAccountPrincipal complete(String rawToken, String password) {
        String tokenHash;
        try {
            rateLimiter.requirePermit("account-setup", rawToken, properties.setupRateLimit());
            tokenHash = secretHasher.sha256(rawToken);
        } catch (IllegalArgumentException exception) {
            throw new SetupUnavailableException();
        }

        Instant now = clock.instant();
        CredentialSetupEntity setup = credentialSetupRepository.findByTokenHash(tokenHash)
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(SetupUnavailableException::new);
        UserAccountEntity account = setup.getUserAccount();
        if (account.getAccountType() != AccountType.STAFF
                || account.getAccountStatus() != AccountStatus.PENDING_SETUP) {
            throw new SetupUnavailableException();
        }

        PasswordPolicy.validate(password);
        account.activate(passwordEncoder.encode(password), now);
        setup.consume(now);
        return toPrincipal(account);
    }

    private AuthenticatedAccountPrincipal toPrincipal(UserAccountEntity account) {
        BranchEntity branch = account.getBranch();
        return new AuthenticatedAccountPrincipal(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.getAccountType(),
                account.getAccountStatus(),
                new AuthenticatedAccountPrincipal.BranchPrincipal(
                        branch.getId(),
                        branch.getPublicCode(),
                        branch.getName(),
                        branch.getDisplayAddress(),
                        branch.getTimezone()));
    }
}
