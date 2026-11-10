package com.github.ppj789.booking.invitation.service;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationRepository;
import com.github.ppj789.booking.identity.config.IdentitySecurityProperties;
import com.github.ppj789.booking.identity.security.FixedWindowRateLimiter;
import com.github.ppj789.booking.identity.security.SecretHasher;
import com.github.ppj789.booking.invitation.exception.InvitationUnavailableException;
import com.github.ppj789.booking.invitation.persistence.InvitationEntity;
import com.github.ppj789.booking.invitation.persistence.InvitationRepository;
import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import com.github.ppj789.booking.invitation.service.GuestAvailabilityPolicy.AvailabilitySlot;
import com.github.ppj789.booking.invitation.service.GuestAvailabilityPolicy.AvailabilityWindow;
import com.github.ppj789.booking.invitation.service.GuestAvailabilityPolicy.OccupiedInterval;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestInvitationService {

    private static final int MAXIMUM_TOKEN_LENGTH = 128;

    private final InvitationRepository invitationRepository;
    private final CalendarReservationRepository reservationRepository;
    private final SecretHasher secretHasher;
    private final FixedWindowRateLimiter rateLimiter;
    private final IdentitySecurityProperties securityProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GuestInvitation getInvitation(String rawToken, String clientKey) {
        rateLimiter.requirePermit(
                "guest-invitation",
                clientKey,
                securityProperties.guestInvitationRateLimit());
        return GuestInvitation.from(resolveActiveInvitation(rawToken));
    }

    @Transactional(readOnly = true)
    public GuestAvailability getAvailability(
            String rawToken,
            String clientKey,
            int durationMinutes,
            Instant requestedFrom,
            int days) {
        rateLimiter.requirePermit(
                "guest-availability",
                availabilityRateLimitKey(rawToken, clientKey),
                securityProperties.guestAvailabilityRateLimit());
        InvitationEntity invitation = resolveActiveInvitation(rawToken);
        Instant generatedAt = clock.instant();
        AvailabilityWindow window = GuestAvailabilityPolicy.createWindow(
                generatedAt,
                requestedFrom,
                days,
                durationMinutes);
        List<OccupiedInterval> occupied = reservationRepository.findOverlappingWindow(
                        invitation.getStaffUser().getId(),
                        window.from(),
                        window.toExclusive())
                .stream()
                .map(GuestInvitationService::toOccupiedInterval)
                .toList();
        List<AvailabilitySlot> slots = GuestAvailabilityPolicy.availableSlots(window, occupied);
        return new GuestAvailability(
                durationMinutes,
                generatedAt,
                window.from(),
                window.toExclusive(),
                slots.stream()
                        .map(slot -> new GuestSlot(slot.startAt(), slot.endAt()))
                        .toList());
    }

    private InvitationEntity resolveActiveInvitation(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > MAXIMUM_TOKEN_LENGTH) {
            throw new InvitationUnavailableException();
        }
        String tokenHash = secretHasher.sha256(rawToken);
        InvitationEntity invitation = invitationRepository
                .findByTokenHashAndStatus(tokenHash, InvitationStatus.ACTIVE)
                .orElseThrow(InvitationUnavailableException::new);
        if (!invitation.isBookable()) {
            throw new InvitationUnavailableException();
        }
        return invitation;
    }

    private String availabilityRateLimitKey(String rawToken, String clientKey) {
        String boundedToken = rawToken == null || rawToken.length() > MAXIMUM_TOKEN_LENGTH
                ? "invalid"
                : rawToken;
        return clientKey + ':' + boundedToken;
    }

    private static OccupiedInterval toOccupiedInterval(CalendarReservationEntity reservation) {
        return new OccupiedInterval(reservation.getStartsAt(), reservation.getEndsAt());
    }

    public record GuestInvitation(
            String title,
            String description,
            String hostDisplayName,
            String guestDisplayName,
            short defaultDurationMinutes,
            List<Integer> supportedDurationsMinutes,
            GuestBranch branch) {

        private static GuestInvitation from(InvitationEntity invitation) {
            BranchEntity branch = invitation.getBranch();
            return new GuestInvitation(
                    invitation.getTitle(),
                    invitation.getDescription(),
                    invitation.getStaffUser().getDisplayName(),
                    invitation.getGuestName(),
                    invitation.getDefaultDurationMinutes(),
                    GuestAvailabilityPolicy.SUPPORTED_DURATIONS,
                    new GuestBranch(
                            branch.getPublicCode(),
                            branch.getName(),
                            branch.getDisplayAddress(),
                            branch.getTimezone()));
        }
    }

    public record GuestBranch(
            String publicCode,
            String name,
            String displayAddress,
            String timezone) {
    }

    public record GuestAvailability(
            int durationMinutes,
            Instant generatedAt,
            Instant from,
            Instant toExclusive,
            List<GuestSlot> slots) {
    }

    public record GuestSlot(Instant startAt, Instant endAt) {
    }
}
