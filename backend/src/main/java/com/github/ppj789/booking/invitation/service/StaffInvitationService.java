package com.github.ppj789.booking.invitation.service;

import static com.github.ppj789.booking.identity.security.AccountAccessPolicy.requireActiveStaff;

import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.invitation.exception.InvitationNotActivatableException;
import com.github.ppj789.booking.invitation.exception.InvitationNotEditableException;
import com.github.ppj789.booking.invitation.exception.InvitationNotFoundException;
import com.github.ppj789.booking.invitation.exception.InvitationNotRevocableException;
import com.github.ppj789.booking.invitation.persistence.InvitationEntity;
import com.github.ppj789.booking.invitation.persistence.InvitationRepository;
import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import com.github.ppj789.booking.invitation.service.InvitationInputPolicy.InvitationDetails;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffInvitationService {

    private final InvitationRepository invitationRepository;
    private final UserAccountRepository userAccountRepository;
    private final InvitationTokenIssuer tokenIssuer;
    private final Clock clock;

    @Transactional
    public InvitationDraft create(
            AuthenticatedAccountPrincipal actor,
            String title,
            String description,
            String guestDisplayName,
            String guestEmail,
            int defaultDurationMinutes) {
        requireActiveStaff(actor);
        UserAccountEntity staff = loadActiveStaff(actor);
        InvitationDetails details = InvitationInputPolicy.normalize(
                title,
                description,
                guestDisplayName,
                guestEmail,
                defaultDurationMinutes);
        InvitationEntity invitation = InvitationEntity.draft(
                staff.getBranch(),
                staff,
                details.title(),
                details.description(),
                details.guestDisplayName(),
                details.guestEmail(),
                details.defaultDurationMinutes(),
                clock.instant());
        return InvitationDraft.from(invitationRepository.saveAndFlush(invitation));
    }

    @Transactional
    public InvitationDraft update(
            AuthenticatedAccountPrincipal actor,
            UUID invitationId,
            String title,
            String description,
            String guestDisplayName,
            String guestEmail,
            int defaultDurationMinutes) {
        requireActiveStaff(actor);
        loadActiveStaff(actor);
        InvitationDetails details = InvitationInputPolicy.normalize(
                title,
                description,
                guestDisplayName,
                guestEmail,
                defaultDurationMinutes);
        InvitationEntity invitation = findOwnedForUpdate(invitationId, actor.accountId());
        if (invitation.getStatus() != InvitationStatus.DRAFT) {
            throw new InvitationNotEditableException();
        }
        invitation.edit(
                details.title(),
                details.description(),
                details.guestDisplayName(),
                details.guestEmail(),
                details.defaultDurationMinutes(),
                clock.instant());
        invitationRepository.flush();
        return InvitationDraft.from(invitation);
    }

    @Transactional
    public InvitationActivation activate(
            AuthenticatedAccountPrincipal actor,
            UUID invitationId) {
        requireActiveStaff(actor);
        loadActiveStaff(actor);
        InvitationEntity invitation = findOwnedForUpdate(invitationId, actor.accountId());
        if (invitation.getStatus() != InvitationStatus.DRAFT) {
            throw new InvitationNotActivatableException();
        }
        InvitationInputPolicy.validateForActivation(invitation);
        InvitationTokenIssuer.IssuedToken token = tokenIssuer.issue();
        Instant activatedAt = clock.instant();
        invitation.activate(token.tokenHash(), activatedAt);
        invitationRepository.flush();
        return new InvitationActivation(
                invitation.getId(),
                invitation.getStatus(),
                activatedAt,
                token.rawToken());
    }

    @Transactional
    public InvitationRevocation revoke(
            AuthenticatedAccountPrincipal actor,
            UUID invitationId) {
        requireActiveStaff(actor);
        loadActiveStaff(actor);
        InvitationEntity invitation = findOwnedForUpdate(invitationId, actor.accountId());
        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            throw new InvitationNotRevocableException();
        }
        Instant revokedAt = clock.instant();
        invitation.revoke(revokedAt);
        invitationRepository.flush();
        return new InvitationRevocation(invitation.getId(), invitation.getStatus(), revokedAt);
    }

    private InvitationEntity findOwnedForUpdate(UUID invitationId, UUID staffId) {
        return invitationRepository.findOwnedByIdForUpdate(invitationId, staffId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private UserAccountEntity loadActiveStaff(AuthenticatedAccountPrincipal actor) {
        return userAccountRepository.findById(actor.accountId())
                .filter(account -> account.getAccountType() == AccountType.STAFF)
                .filter(account -> account.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(account -> account.getBranch() != null)
                .filter(account -> account.getBranch().getId().equals(actor.branch().id()))
                .orElseThrow(() -> new AccessDeniedException("Active Staff access is required"));
    }

    public record InvitationDraft(
            UUID id,
            InvitationStatus status,
            String title,
            String description,
            String guestDisplayName,
            String guestEmail,
            short defaultDurationMinutes,
            Instant createdAt,
            Instant updatedAt) {

        private static InvitationDraft from(InvitationEntity invitation) {
            return new InvitationDraft(
                    invitation.getId(),
                    invitation.getStatus(),
                    invitation.getTitle(),
                    invitation.getDescription(),
                    invitation.getGuestName(),
                    invitation.getGuestEmail(),
                    invitation.getDefaultDurationMinutes(),
                    invitation.getCreatedAt(),
                    invitation.getUpdatedAt());
        }
    }

    public static final class InvitationActivation {

        private final UUID id;
        private final InvitationStatus status;
        private final Instant activatedAt;
        private final String rawToken;

        InvitationActivation(
                UUID id,
                InvitationStatus status,
                Instant activatedAt,
                String rawToken) {
            this.id = id;
            this.status = status;
            this.activatedAt = activatedAt;
            this.rawToken = rawToken;
        }

        public UUID id() {
            return id;
        }

        public InvitationStatus status() {
            return status;
        }

        public Instant activatedAt() {
            return activatedAt;
        }

        public String rawToken() {
            return rawToken;
        }

        @Override
        public String toString() {
            return "InvitationActivation[id=" + id + ", status=" + status + ", token=redacted]";
        }
    }

    public record InvitationRevocation(
            UUID id,
            InvitationStatus status,
            Instant revokedAt) {
    }
}
