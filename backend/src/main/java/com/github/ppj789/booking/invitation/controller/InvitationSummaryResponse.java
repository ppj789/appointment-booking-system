package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationSummary;
import java.time.Instant;
import java.util.UUID;

public record InvitationSummaryResponse(
        UUID id,
        InvitationStatus status,
        String title,
        String description,
        InvitationGuestResponse guest,
        short defaultDurationMinutes,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant consumedAt,
        Instant revokedAt) {

    InvitationSummaryResponse(InvitationSummary invitation) {
        this(
                invitation.id(),
                invitation.status(),
                invitation.title(),
                invitation.description(),
                new InvitationGuestResponse(
                        invitation.guestDisplayName(),
                        invitation.guestEmail()),
                invitation.defaultDurationMinutes(),
                invitation.createdAt(),
                invitation.updatedAt(),
                invitation.activatedAt(),
                invitation.consumedAt(),
                invitation.revokedAt());
    }
}
