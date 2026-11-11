package com.github.ppj789.booking.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.appointment.persistence.AppointmentRepository;
import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.branch.persistence.BranchRepository;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties.RateLimit;
import com.github.ppj789.booking.identity.exception.DuplicateStaffEmailException;
import com.github.ppj789.booking.identity.exception.StaffDeactivationBlockedException;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.CredentialSetupEntity;
import com.github.ppj789.booking.identity.persistence.CredentialSetupRepository;
import com.github.ppj789.booking.identity.persistence.CredentialSetupStatus;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.security.FixedWindowRateLimiter;
import com.github.ppj789.booking.identity.service.AdminStaffService.StaffSetup;
import com.github.ppj789.booking.invitation.persistence.InvitationRepository;
import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;

class AdminStaffServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");
    private static final Instant SETUP_EXPIRY = NOW.plus(Duration.ofHours(24));
    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID STAFF_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");

    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final BranchRepository branchRepository = mock(BranchRepository.class);
    private final CredentialSetupRepository credentialSetupRepository = mock(CredentialSetupRepository.class);
    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final CredentialSetupIssuer credentialSetupIssuer = mock(CredentialSetupIssuer.class);
    private final FixedWindowRateLimiter rateLimiter = mock(FixedWindowRateLimiter.class);
    private final IdentitySecurityProperties properties = properties();
    private final BranchEntity branch = BranchEntity.demo(
            UUID.fromString("11111111-1111-4111-8111-111111111111"),
            "JHB-DEMO",
            "Johannesburg Demo Branch",
            "1 Demo Street, Johannesburg",
            "Africa/Johannesburg");

    private final AdminStaffService service = new AdminStaffService(
            userAccountRepository,
            branchRepository,
            credentialSetupRepository,
            invitationRepository,
            appointmentRepository,
            credentialSetupIssuer,
            rateLimiter,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void provisionsCanonicalPendingStaffAndReturnsTheOneTimeSetup() {
        when(userAccountRepository.existsByEmail("staff@example.test")).thenReturn(false);
        when(branchRepository.findByPublicCodeAndActiveTrue("JHB-DEMO")).thenReturn(Optional.of(branch));
        when(userAccountRepository.saveAndFlush(any(UserAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(credentialSetupIssuer.issue(any(UserAccountEntity.class)))
                .thenReturn(new CredentialSetupIssuer.IssuedSetup("raw-token", SETUP_EXPIRY));

        StaffSetup result = service.provision(
                adminPrincipal(),
                " jhb-demo ",
                " Demo Staff ",
                " STAFF@EXAMPLE.TEST ");

        ArgumentCaptor<UserAccountEntity> accountCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).saveAndFlush(accountCaptor.capture());
        UserAccountEntity saved = accountCaptor.getValue();
        assertEquals("Demo Staff", saved.getDisplayName());
        assertEquals("staff@example.test", saved.getEmail());
        assertEquals(AccountStatus.PENDING_SETUP, saved.getAccountStatus());
        assertSame(branch, saved.getBranch());
        assertEquals("raw-token", result.rawToken());
        assertEquals(SETUP_EXPIRY, result.expiresAt());
        verify(rateLimiter).requirePermit(
                "staff-provision",
                ADMIN_ID.toString(),
                properties.provisioningRateLimit());
        verify(credentialSetupIssuer).issue(saved);
    }

    @Test
    void rejectsDuplicateCanonicalEmailBeforeCreatingStaff() {
        when(userAccountRepository.existsByEmail("staff@example.test")).thenReturn(true);

        DuplicateStaffEmailException exception = assertThrows(
                DuplicateStaffEmailException.class,
                () -> service.provision(
                        adminPrincipal(),
                        "JHB-DEMO",
                        "Demo Staff",
                        "STAFF@EXAMPLE.TEST"));

        assertEquals("A Staff account already uses this email address", exception.getMessage());
        verifyNoInteractions(branchRepository, credentialSetupIssuer);
        verify(userAccountRepository, never()).saveAndFlush(any(UserAccountEntity.class));
    }

    @Test
    void rejectsStaffCallingAdminOperations() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> service.list(staffPrincipal(), null));

        assertEquals("Active Admin access is required", exception.getMessage());
        verifyNoInteractions(userAccountRepository);
    }

    @Test
    void replacementRevokesTheExistingSetupBeforeIssuingAnother() {
        UserAccountEntity staff = UserAccountEntity.pendingStaff(
                branch,
                "Demo Staff",
                "staff@example.test",
                NOW.minusSeconds(60));
        CredentialSetupEntity existingSetup = mock(CredentialSetupEntity.class);
        when(userAccountRepository.findByIdForUpdate(STAFF_ID)).thenReturn(Optional.of(staff));
        when(credentialSetupRepository.findByUserAccountIdAndStatus(
                        STAFF_ID,
                        CredentialSetupStatus.ACTIVE))
                .thenReturn(Optional.of(existingSetup));
        when(credentialSetupIssuer.issue(staff))
                .thenReturn(new CredentialSetupIssuer.IssuedSetup("replacement-token", SETUP_EXPIRY));

        StaffSetup result = service.replaceSetup(adminPrincipal(), STAFF_ID);

        InOrder replacementOrder = inOrder(
                existingSetup,
                credentialSetupRepository,
                credentialSetupIssuer);
        replacementOrder.verify(existingSetup).revoke(NOW);
        replacementOrder.verify(credentialSetupRepository).flush();
        replacementOrder.verify(credentialSetupIssuer).issue(staff);
        assertEquals("replacement-token", result.rawToken());
    }

    @Test
    void reportsBothSafeBlockersWithoutDeactivatingStaff() {
        UserAccountEntity staff = activeStaff();
        when(userAccountRepository.findByIdForUpdate(STAFF_ID)).thenReturn(Optional.of(staff));
        when(invitationRepository.existsByStaffUserIdAndStatus(STAFF_ID, InvitationStatus.ACTIVE))
                .thenReturn(true);
        when(appointmentRepository.existsByStaffUserIdAndReservationStartsAtAfter(STAFF_ID, NOW))
                .thenReturn(true);

        StaffDeactivationBlockedException exception = assertThrows(
                StaffDeactivationBlockedException.class,
                () -> service.deactivate(adminPrincipal(), STAFF_ID));

        assertTrue(exception.isActiveInvitation());
        assertTrue(exception.isFutureAppointment());
        assertEquals(AccountStatus.ACTIVE, staff.getAccountStatus());
        verifyNoInteractions(credentialSetupRepository);
    }

    @Test
    void deactivatingPendingStaffRevokesItsUnusedSetup() {
        UserAccountEntity staff = UserAccountEntity.pendingStaff(
                branch,
                "Demo Staff",
                "staff@example.test",
                NOW.minusSeconds(60));
        CredentialSetupEntity existingSetup = mock(CredentialSetupEntity.class);
        when(userAccountRepository.findByIdForUpdate(STAFF_ID)).thenReturn(Optional.of(staff));
        when(credentialSetupRepository.findByUserAccountIdAndStatus(
                        STAFF_ID,
                        CredentialSetupStatus.ACTIVE))
                .thenReturn(Optional.of(existingSetup));

        AdminStaffService.ManagedStaff result = service.deactivate(adminPrincipal(), STAFF_ID);

        verify(existingSetup).revoke(NOW);
        assertEquals(AccountStatus.DEACTIVATED, staff.getAccountStatus());
        assertEquals(AccountStatus.DEACTIVATED, result.status());
        assertFalse(result.branch().publicCode().isBlank());
    }

    private UserAccountEntity activeStaff() {
        UserAccountEntity staff = UserAccountEntity.pendingStaff(
                branch,
                "Demo Staff",
                "staff@example.test",
                NOW.minusSeconds(120));
        staff.activate("{bcrypt}encoded-password", NOW.minusSeconds(60));
        return staff;
    }

    private AuthenticatedAccountPrincipal adminPrincipal() {
        return new AuthenticatedAccountPrincipal(
                ADMIN_ID,
                "Local Admin",
                "admin@example.test",
                AccountType.ADMIN,
                AccountStatus.ACTIVE,
                null);
    }

    private AuthenticatedAccountPrincipal staffPrincipal() {
        return new AuthenticatedAccountPrincipal(
                STAFF_ID,
                "Demo Staff",
                "staff@example.test",
                AccountType.STAFF,
                AccountStatus.ACTIVE,
                null);
    }

    private IdentitySecurityProperties properties() {
        RateLimit defaultLimit = new RateLimit(10, Duration.ofMinutes(5));
        return new IdentitySecurityProperties(
                Duration.ofHours(24),
                Duration.ofHours(8),
                defaultLimit,
                defaultLimit,
                new RateLimit(20, Duration.ofMinutes(5)),
                new RateLimit(30, Duration.ofMinutes(5)),
                new RateLimit(120, Duration.ofMinutes(1)),
                new RateLimit(10, Duration.ofMinutes(1)));
    }
}
