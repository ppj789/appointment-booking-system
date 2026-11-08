package com.github.ppj789.booking.identity.persistence;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
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
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    private UserAccountEntity(
            BranchEntity branch,
            AccountType accountType,
            AccountStatus accountStatus,
            String displayName,
            String email,
            String passwordHash,
            Instant createdAt,
            Instant activatedAt) {
        this.branch = branch;
        this.accountType = accountType;
        this.accountStatus = accountStatus;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.activatedAt = activatedAt;
    }

    public static UserAccountEntity activeAdmin(
            String displayName,
            String email,
            String passwordHash,
            Instant createdAt) {
        return new UserAccountEntity(
                null,
                AccountType.ADMIN,
                AccountStatus.ACTIVE,
                displayName,
                email,
                passwordHash,
                createdAt,
                createdAt);
    }

    public static UserAccountEntity pendingStaff(
            BranchEntity branch,
            String displayName,
            String email,
            Instant createdAt) {
        return new UserAccountEntity(
                branch,
                AccountType.STAFF,
                AccountStatus.PENDING_SETUP,
                displayName,
                email,
                null,
                createdAt,
                null);
    }

    public void activate(String encodedPassword, Instant activatedAt) {
        if (accountType != AccountType.STAFF || accountStatus != AccountStatus.PENDING_SETUP) {
            throw new IllegalStateException("Only Staff waiting for setup can be activated");
        }
        this.passwordHash = encodedPassword;
        this.accountStatus = AccountStatus.ACTIVE;
        this.activatedAt = activatedAt;
        this.updatedAt = activatedAt;
    }

}
