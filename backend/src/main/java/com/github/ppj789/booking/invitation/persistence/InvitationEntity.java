package com.github.ppj789.booking.invitation.persistence;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
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
@Table(name = "invitation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private UserAccountEntity staffUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "guest_email", nullable = false, length = 254)
    private String guestEmail;

    @Column(name = "default_duration_minutes", nullable = false)
    private short defaultDurationMinutes;

    @Column(name = "token_hash", length = 64, unique = true)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    private InvitationEntity(
            BranchEntity branch,
            UserAccountEntity staffUser,
            String title,
            String description,
            String guestName,
            String guestEmail,
            short defaultDurationMinutes,
            Instant createdAt) {
        this.branch = branch;
        this.staffUser = staffUser;
        this.status = InvitationStatus.DRAFT;
        this.title = title;
        this.description = description;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static InvitationEntity draft(
            BranchEntity branch,
            UserAccountEntity staffUser,
            String title,
            String description,
            String guestName,
            String guestEmail,
            short defaultDurationMinutes,
            Instant createdAt) {
        return new InvitationEntity(
                branch,
                staffUser,
                title,
                description,
                guestName,
                guestEmail,
                defaultDurationMinutes,
                createdAt);
    }

    public void edit(
            String title,
            String description,
            String guestName,
            String guestEmail,
            short defaultDurationMinutes,
            Instant updatedAt) {
        if (status != InvitationStatus.DRAFT) {
            throw new IllegalStateException("Only a draft Invitation can be edited");
        }
        this.title = title;
        this.description = description;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.updatedAt = updatedAt;
    }

    public void activate(String tokenHash, Instant activatedAt) {
        if (status != InvitationStatus.DRAFT) {
            throw new IllegalStateException("Only a draft Invitation can be activated");
        }
        if (tokenHash == null || tokenHash.length() != 64) {
            throw new IllegalArgumentException("Invitation token hash must contain 64 characters");
        }
        this.status = InvitationStatus.ACTIVE;
        this.tokenHash = tokenHash;
        this.activatedAt = activatedAt;
        this.updatedAt = activatedAt;
    }

    public void revoke(Instant revokedAt) {
        if (status != InvitationStatus.ACTIVE) {
            throw new IllegalStateException("Only an active Invitation can be revoked");
        }
        this.status = InvitationStatus.REVOKED;
        this.revokedAt = revokedAt;
        this.updatedAt = revokedAt;
    }

    public void consume(Instant consumedAt) {
        if (status != InvitationStatus.ACTIVE) {
            throw new IllegalStateException("Only an active Invitation can be consumed");
        }
        this.status = InvitationStatus.CONSUMED;
        this.consumedAt = consumedAt;
        this.updatedAt = consumedAt;
    }

    public boolean isBookable() {
        return status == InvitationStatus.ACTIVE
                && branch.isActive()
                && staffUser.getAccountType() == AccountType.STAFF
                && staffUser.getAccountStatus() == AccountStatus.ACTIVE
                && staffUser.getBranch() != null
                && staffUser.getBranch().getId().equals(branch.getId());
    }
}
