package com.github.ppj789.booking.identity.service;

import com.github.ppj789.booking.appointment.persistence.AppointmentRepository;
import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.branch.persistence.BranchRepository;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.exception.BranchUnavailableException;
import com.github.ppj789.booking.identity.exception.DuplicateStaffEmailException;
import com.github.ppj789.booking.identity.exception.StaffDeactivationBlockedException;
import com.github.ppj789.booking.identity.exception.StaffSetupConflictException;
import com.github.ppj789.booking.identity.exception.StaffUnavailableException;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.CredentialSetupRepository;
import com.github.ppj789.booking.identity.persistence.CredentialSetupStatus;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import com.github.ppj789.booking.identity.security.AccountAccessPolicy;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.security.FixedWindowRateLimiter;
import com.github.ppj789.booking.invitation.persistence.InvitationRepository;
import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStaffService {

    private final UserAccountRepository userAccountRepository;
    private final BranchRepository branchRepository;
    private final CredentialSetupRepository credentialSetupRepository;
    private final InvitationRepository invitationRepository;
    private final AppointmentRepository appointmentRepository;
    private final CredentialSetupIssuer credentialSetupIssuer;
    private final FixedWindowRateLimiter rateLimiter;
    private final IdentitySecurityProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ManagedStaff> list(
            AuthenticatedAccountPrincipal actor,
            String branchCode) {
        AccountAccessPolicy.requireActiveAdmin(actor);
        List<UserAccountEntity> accounts;
        if (branchCode == null || branchCode.isBlank()) {
            accounts = userAccountRepository.findAllByAccountTypeOrderByDisplayNameAsc(AccountType.STAFF);
        } else {
            accounts = userAccountRepository
                    .findAllByAccountTypeAndBranchPublicCodeOrderByDisplayNameAsc(
                            AccountType.STAFF,
                            normalizeBranchCode(branchCode));
        }
        return accounts.stream().map(account -> ManagedStaff.from(account)).toList();
    }

    @Transactional
    public StaffSetup provision(
            AuthenticatedAccountPrincipal actor,
            String branchCode,
            String displayName,
            String email) {
        AccountAccessPolicy.requireActiveAdmin(actor);
        rateLimiter.requirePermit(
                "staff-provision",
                actor.accountId().toString(),
                properties.provisioningRateLimit());

        String normalizedEmail = AccountInputNormalizer.email(email);
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateStaffEmailException();
        }
        BranchEntity branch = branchRepository
                .findByPublicCodeAndActiveTrue(normalizeBranchCode(branchCode))
                .orElseThrow(BranchUnavailableException::new);
        UserAccountEntity staff = UserAccountEntity.pendingStaff(
                branch,
                AccountInputNormalizer.displayName(displayName),
                normalizedEmail,
                clock.instant());
        try {
            staff = userAccountRepository.saveAndFlush(staff);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateStaffEmailException(exception);
        }

        CredentialSetupIssuer.IssuedSetup setup = credentialSetupIssuer.issue(staff);
        return new StaffSetup(ManagedStaff.from(staff), setup.rawToken(), setup.expiresAt());
    }

    @Transactional
    public StaffSetup replaceSetup(
            AuthenticatedAccountPrincipal actor,
            UUID staffId) {
        AccountAccessPolicy.requireActiveAdmin(actor);
        rateLimiter.requirePermit(
                "staff-setup-replacement",
                actor.accountId().toString(),
                properties.provisioningRateLimit());

        UserAccountEntity staff = findStaffForUpdate(staffId);
        if (staff.getAccountStatus() != AccountStatus.PENDING_SETUP) {
            throw new StaffSetupConflictException();
        }
        credentialSetupRepository
                .findByUserAccountIdAndStatus(staffId, CredentialSetupStatus.ACTIVE)
                .ifPresent(setup -> {
                    setup.revoke(clock.instant());
                    credentialSetupRepository.flush();
                });

        CredentialSetupIssuer.IssuedSetup setup = credentialSetupIssuer.issue(staff);
        return new StaffSetup(ManagedStaff.from(staff), setup.rawToken(), setup.expiresAt());
    }

    @Transactional
    public ManagedStaff deactivate(
            AuthenticatedAccountPrincipal actor,
            UUID staffId) {
        AccountAccessPolicy.requireActiveAdmin(actor);
        UserAccountEntity staff = findStaffForUpdate(staffId);
        if (staff.getAccountStatus() == AccountStatus.DEACTIVATED) {
            return ManagedStaff.from(staff);
        }

        Instant now = clock.instant();
        boolean activeInvitation = invitationRepository
                .existsByStaffUserIdAndStatus(staffId, InvitationStatus.ACTIVE);
        boolean futureAppointment = appointmentRepository
                .existsByStaffUserIdAndReservationStartsAtAfter(staffId, now);
        if (activeInvitation || futureAppointment) {
            throw new StaffDeactivationBlockedException(activeInvitation, futureAppointment);
        }

        credentialSetupRepository
                .findByUserAccountIdAndStatus(staffId, CredentialSetupStatus.ACTIVE)
                .ifPresent(setup -> setup.revoke(now));
        staff.deactivate(now);
        return ManagedStaff.from(staff);
    }

    private UserAccountEntity findStaffForUpdate(UUID staffId) {
        return userAccountRepository.findByIdForUpdate(staffId)
                .filter(account -> account.getAccountType() == AccountType.STAFF)
                .orElseThrow(StaffUnavailableException::new);
    }

    private String normalizeBranchCode(String branchCode) {
        String normalized = branchCode == null ? "" : branchCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < 2 || normalized.length() > 30) {
            throw new BranchUnavailableException();
        }
        return normalized;
    }

    public record StaffSetup(
            ManagedStaff staff,
            String rawToken,
            Instant expiresAt) {
    }

    public record ManagedStaff(
            UUID id,
            String displayName,
            String email,
            AccountStatus status,
            ManagedBranch branch) {

        private static ManagedStaff from(UserAccountEntity account) {
            BranchEntity branch = account.getBranch();
            if (branch == null) {
                throw new StaffUnavailableException();
            }
            return new ManagedStaff(
                    account.getId(),
                    account.getDisplayName(),
                    account.getEmail(),
                    account.getAccountStatus(),
                    ManagedBranch.from(branch));
        }
    }

    public record ManagedBranch(
            String publicCode,
            String name,
            String displayAddress,
            String timezone) {

        private static ManagedBranch from(BranchEntity branch) {
            return new ManagedBranch(
                    branch.getPublicCode(),
                    branch.getName(),
                    branch.getDisplayAddress(),
                    branch.getTimezone());
        }
    }
}
