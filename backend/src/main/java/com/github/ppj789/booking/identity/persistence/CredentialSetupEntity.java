package com.github.ppj789.booking.identity.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credential_setup")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CredentialSetupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity userAccount;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CredentialSetupStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    private CredentialSetupEntity(
            UserAccountEntity userAccount,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt) {
        this.userAccount = userAccount;
        this.tokenHash = tokenHash;
        this.status = CredentialSetupStatus.ACTIVE;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static CredentialSetupEntity active(
            UserAccountEntity userAccount,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt) {
        return new CredentialSetupEntity(userAccount, tokenHash, createdAt, expiresAt);
    }

    public boolean isUsableAt(Instant now) {
        return status == CredentialSetupStatus.ACTIVE && now.isBefore(expiresAt);
    }

    public void consume(Instant consumedAt) {
        if (!isUsableAt(consumedAt)) {
            throw new IllegalStateException("Credential setup is not usable");
        }
        this.status = CredentialSetupStatus.CONSUMED;
        this.consumedAt = consumedAt;
    }

    public void revoke(Instant revokedAt) {
        if (status != CredentialSetupStatus.ACTIVE) {
            throw new IllegalStateException("Only an active credential setup can be revoked");
        }
        this.status = CredentialSetupStatus.REVOKED;
        this.revokedAt = revokedAt;
    }

}
