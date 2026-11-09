package com.github.ppj789.booking.invitation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationRepository;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties.RateLimit;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.security.FixedWindowRateLimiter;
import com.github.ppj789.booking.identity.security.SecretHasher;
import com.github.ppj789.booking.invitation.exception.InvitationUnavailableException;
import com.github.ppj789.booking.invitation.persistence.InvitationEntity;
import com.github.ppj789.booking.invitation.persistence.InvitationRepository;
import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GuestInvitationServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");
    private static final UUID STAFF_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID BRANCH_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final String RAW_TOKEN = "valid-guest-capability";
    private static final String CLIENT_KEY = "127.0.0.1";

    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final CalendarReservationRepository reservationRepository =
            mock(CalendarReservationRepository.class);
    private final FixedWindowRateLimiter rateLimiter = mock(FixedWindowRateLimiter.class);
    private final SecretHasher secretHasher = new SecretHasher();
    private final IdentitySecurityProperties properties = properties();
    private final GuestInvitationService service = new GuestInvitationService(
            invitationRepository,
            reservationRepository,
            secretHasher,
            rateLimiter,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void returnsOnlyTheApprovedGuestInvitationContext() {
        InvitationEntity invitation = activeInvitation();
        when(invitationRepository.findByTokenHashAndStatus(
                secretHasher.sha256(RAW_TOKEN),
                InvitationStatus.ACTIVE)).thenReturn(Optional.of(invitation));

        GuestInvitationService.GuestInvitation result =
                service.getInvitation(RAW_TOKEN, CLIENT_KEY);

        assertEquals("Account discussion", result.title());
        assertEquals("Demo Staff", result.hostDisplayName());
        assertEquals("Alex Guest", result.guestDisplayName());
        assertEquals("JHB-DEMO", result.branch().publicCode());
        assertEquals("Africa/Johannesburg", result.branch().timezone());
        assertEquals(List.of(15, 30, 45, 60, 120, 240), result.supportedDurationsMinutes());
        verify(rateLimiter).requirePermit(
                "guest-invitation",
                CLIENT_KEY,
                properties.guestInvitationRateLimit());
    }

    @Test
    void returnsTheSameUnavailableFailureForUnknownOrInactiveCapabilities() {
        when(invitationRepository.findByTokenHashAndStatus(any(), any()))
                .thenReturn(Optional.empty());

        InvitationUnavailableException unknown = assertThrows(
                InvitationUnavailableException.class,
                () -> service.getInvitation("unknown-token", CLIENT_KEY));
        InvitationUnavailableException blank = assertThrows(
                InvitationUnavailableException.class,
                () -> service.getInvitation(" ", CLIENT_KEY));

        assertEquals("Invitation is unavailable", unknown.getMessage());
        assertEquals(unknown.getMessage(), blank.getMessage());
    }

    @Test
    void calculatesSlotsWithoutReturningReservationDetails() {
        InvitationEntity invitation = activeInvitation();
        Instant windowFrom = Instant.parse("2026-11-09T10:15:00Z");
        Instant windowEnd = Instant.parse("2026-11-10T10:15:00Z");
        CalendarReservationEntity occupied = mock(CalendarReservationEntity.class);
        when(occupied.getStartsAt()).thenReturn(Instant.parse("2026-11-09T10:45:00Z"));
        when(occupied.getEndsAt()).thenReturn(Instant.parse("2026-11-09T11:00:00Z"));
        when(invitationRepository.findByTokenHashAndStatus(
                secretHasher.sha256(RAW_TOKEN),
                InvitationStatus.ACTIVE)).thenReturn(Optional.of(invitation));
        when(reservationRepository.findOverlappingWindow(STAFF_ID, windowFrom, windowEnd))
                .thenReturn(List.of(occupied));

        GuestInvitationService.GuestAvailability result = service.getAvailability(
                RAW_TOKEN,
                CLIENT_KEY,
                30,
                null,
                1);

        assertEquals(30, result.durationMinutes());
        assertEquals(windowFrom, result.from());
        assertEquals(windowEnd, result.toExclusive());
        assertEquals(93, result.slots().size());
        assertEquals(
                Instant.parse("2026-11-09T10:15:00Z"),
                result.slots().getFirst().startAt());
        assertEquals(
                Instant.parse("2026-11-09T11:00:00Z"),
                result.slots().get(1).startAt());
        verify(rateLimiter).requirePermit(
                "guest-availability",
                CLIENT_KEY + ':' + RAW_TOKEN,
                properties.guestAvailabilityRateLimit());
    }

    private InvitationEntity activeInvitation() {
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
        ReflectionTestUtils.setField(staff, "id", STAFF_ID);
        staff.activate("encoded-password", NOW.minusSeconds(1800));
        InvitationEntity invitation = InvitationEntity.draft(
                branch,
                staff,
                "Account discussion",
                "Review the available account options.",
                "Alex Guest",
                "alex@example.test",
                (short) 60,
                NOW.minusSeconds(600));
        invitation.activate(secretHasher.sha256(RAW_TOKEN), NOW.minusSeconds(300));
        return invitation;
    }

    private IdentitySecurityProperties properties() {
        RateLimit defaultLimit = new RateLimit(10, Duration.ofMinutes(5));
        return new IdentitySecurityProperties(
                Duration.ofHours(24),
                Duration.ofHours(8),
                defaultLimit,
                defaultLimit,
                defaultLimit,
                new RateLimit(30, Duration.ofMinutes(5)),
                new RateLimit(120, Duration.ofMinutes(1)));
    }
}
