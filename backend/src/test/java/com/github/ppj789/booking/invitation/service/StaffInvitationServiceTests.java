package com.github.ppj789.booking.invitation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal.BranchPrincipal;
import com.github.ppj789.booking.invitation.exception.InvitationNotActivatableException;
import com.github.ppj789.booking.invitation.exception.InvitationNotFoundException;
import com.github.ppj789.booking.invitation.persistence.InvitationEntity;
import com.github.ppj789.booking.invitation.persistence.InvitationRepository;
import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class StaffInvitationServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");
    private static final UUID STAFF_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID BRANCH_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID INVITATION_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private static final String RAW_TOKEN = "raw-token-that-must-only-be-returned-once";
    private static final String TOKEN_HASH = "a".repeat(64);

    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final InvitationTokenIssuer tokenIssuer = mock(InvitationTokenIssuer.class);
    private final StaffInvitationService service = new StaffInvitationService(
            invitationRepository,
            userAccountRepository,
            tokenIssuer,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void listsOnlyInvitationsOwnedByTheActiveStaffAccount() {
        InvitationEntity invitation = draftInvitation();
        invitation.activate(TOKEN_HASH, NOW.minusSeconds(60));
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(invitationRepository.findAllByStaffUserIdOrderByUpdatedAtDesc(STAFF_ID))
                .thenReturn(List.of(invitation));

        List<StaffInvitationService.InvitationSummary> result = service.list(staffPrincipal());

        assertEquals(1, result.size());
        StaffInvitationService.InvitationSummary summary = result.getFirst();
        assertEquals(INVITATION_ID, summary.id());
        assertEquals(InvitationStatus.ACTIVE, summary.status());
        assertEquals("alex@example.test", summary.guestEmail());
        assertEquals(NOW.minusSeconds(60), summary.activatedAt());
        verify(invitationRepository).findAllByStaffUserIdOrderByUpdatedAtDesc(STAFF_ID);
    }

    @Test
    void createsABranchBoundDraftWithCanonicalGuestDetails() {
        UserAccountEntity staff = activeStaff();
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));
        when(invitationRepository.saveAndFlush(any(InvitationEntity.class)))
                .thenAnswer(invocation -> {
                    InvitationEntity invitation = invocation.getArgument(0);
                    ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
                    return invitation;
                });

        StaffInvitationService.InvitationDraft result = service.create(
                staffPrincipal(),
                "  Account discussion  ",
                "  Review account options.  ",
                "  Alex Guest  ",
                "  ALEX@EXAMPLE.TEST  ",
                60);

        ArgumentCaptor<InvitationEntity> invitationCaptor =
                ArgumentCaptor.forClass(InvitationEntity.class);
        verify(invitationRepository).saveAndFlush(invitationCaptor.capture());
        InvitationEntity invitation = invitationCaptor.getValue();
        assertSame(staff, invitation.getStaffUser());
        assertSame(staff.getBranch(), invitation.getBranch());
        assertEquals(InvitationStatus.DRAFT, result.status());
        assertEquals("Account discussion", result.title());
        assertEquals("alex@example.test", result.guestEmail());
    }

    @Test
    void activatesAnOwnedDraftWhilePersistingOnlyTheTokenHash() {
        InvitationEntity invitation = draftInvitation();
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(invitationRepository.findOwnedByIdForUpdate(INVITATION_ID, STAFF_ID))
                .thenReturn(Optional.of(invitation));
        when(tokenIssuer.issue())
                .thenReturn(new InvitationTokenIssuer.IssuedToken(RAW_TOKEN, TOKEN_HASH));

        StaffInvitationService.InvitationActivation result =
                service.activate(staffPrincipal(), INVITATION_ID);

        assertEquals(InvitationStatus.ACTIVE, invitation.getStatus());
        assertEquals(TOKEN_HASH, invitation.getTokenHash());
        assertEquals(RAW_TOKEN, result.rawToken());
        assertEquals(NOW, result.activatedAt());
        assertFalse(result.toString().contains(RAW_TOKEN));
        verify(invitationRepository).flush();
    }

    @Test
    void editsAnOwnedDraftAndCanonicalizesTheReplacementDetails() {
        InvitationEntity invitation = draftInvitation();
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(invitationRepository.findOwnedByIdForUpdate(INVITATION_ID, STAFF_ID))
                .thenReturn(Optional.of(invitation));

        StaffInvitationService.InvitationDraft result = service.update(
                staffPrincipal(),
                INVITATION_ID,
                "  Lending discussion  ",
                "  Review lending options.  ",
                "  Casey Guest  ",
                "  CASEY@EXAMPLE.TEST  ",
                45);

        assertEquals("Lending discussion", result.title());
        assertEquals("Review lending options.", result.description());
        assertEquals("Casey Guest", result.guestDisplayName());
        assertEquals("casey@example.test", result.guestEmail());
        assertEquals(45, result.defaultDurationMinutes());
        assertEquals(NOW, result.updatedAt());
        verify(invitationRepository).flush();
    }

    @Test
    void revokesAnOwnedUnusedActiveInvitation() {
        InvitationEntity invitation = draftInvitation();
        invitation.activate(TOKEN_HASH, NOW.minusSeconds(60));
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(invitationRepository.findOwnedByIdForUpdate(INVITATION_ID, STAFF_ID))
                .thenReturn(Optional.of(invitation));

        StaffInvitationService.InvitationRevocation result =
                service.revoke(staffPrincipal(), INVITATION_ID);

        assertEquals(InvitationStatus.REVOKED, result.status());
        assertEquals(NOW, result.revokedAt());
        assertEquals(NOW, invitation.getRevokedAt());
        verify(invitationRepository).flush();
    }

    @Test
    void refusesToIssueAnotherTokenForAnActiveInvitation() {
        InvitationEntity invitation = draftInvitation();
        invitation.activate(TOKEN_HASH, NOW.minusSeconds(60));
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(invitationRepository.findOwnedByIdForUpdate(INVITATION_ID, STAFF_ID))
                .thenReturn(Optional.of(invitation));

        InvitationNotActivatableException exception = assertThrows(
                InvitationNotActivatableException.class,
                () -> service.activate(staffPrincipal(), INVITATION_ID));

        assertEquals("Only a draft Invitation can be activated", exception.getMessage());
        verify(tokenIssuer, never()).issue();
    }

    @Test
    void hidesMissingAndCrossOwnerInvitationIdsBehindTheSameFailure() {
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(invitationRepository.findOwnedByIdForUpdate(INVITATION_ID, STAFF_ID))
                .thenReturn(Optional.empty());

        InvitationNotFoundException exception = assertThrows(
                InvitationNotFoundException.class,
                () -> service.revoke(staffPrincipal(), INVITATION_ID));

        assertEquals("Invitation is unavailable", exception.getMessage());
    }

    @Test
    void rejectsAStaleSessionAfterTheStaffAccountIsDeactivated() {
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.empty());

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> service.create(
                        staffPrincipal(),
                        "Account discussion",
                        "Review account options.",
                        "Alex Guest",
                        "alex@example.test",
                        30));

        assertEquals("Active Staff access is required", exception.getMessage());
        verify(invitationRepository, never()).saveAndFlush(any(InvitationEntity.class));
    }

    private InvitationEntity draftInvitation() {
        UserAccountEntity staff = activeStaff();
        InvitationEntity invitation = InvitationEntity.draft(
                staff.getBranch(),
                staff,
                "Account discussion",
                "Review account options.",
                "Alex Guest",
                "alex@example.test",
                (short) 60,
                NOW.minusSeconds(120));
        ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
        return invitation;
    }

    private UserAccountEntity activeStaff() {
        BranchEntity branch = BranchEntity.demo(
                BRANCH_ID,
                "JHB-DEMO",
                "Johannesburg Demo Branch",
                "1 Demo Street, Johannesburg",
                "Africa/Johannesburg");
        UserAccountEntity staff = UserAccountEntity.pendingStaff(
                branch,
                "Demo Staff",
                "staff@example.test",
                NOW.minusSeconds(3600));
        staff.activate("encoded-password", NOW.minusSeconds(1800));
        ReflectionTestUtils.setField(staff, "id", STAFF_ID);
        return staff;
    }

    private AuthenticatedAccountPrincipal staffPrincipal() {
        return new AuthenticatedAccountPrincipal(
                STAFF_ID,
                "Demo Staff",
                "staff@example.test",
                AccountType.STAFF,
                AccountStatus.ACTIVE,
                new BranchPrincipal(
                        BRANCH_ID,
                        "JHB-DEMO",
                        "Johannesburg Demo Branch",
                        "1 Demo Street, Johannesburg",
                        "Africa/Johannesburg"));
    }
}
