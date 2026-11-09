package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationDraft;
import java.time.Instant;
import java.util.UUID;

public record InvitationDraftResponse(
        UUID id,
        InvitationStatus status,
        String title,
        String description,
        InvitationGuestResponse guest,
        short defaultDurationMinutes,
        Instant createdAt,
        Instant updatedAt) {

    static InvitationDraftResponse from(InvitationDraft draft) {
        return new InvitationDraftResponse(
                draft.id(),
                draft.status(),
                draft.title(),
                draft.description(),
                new InvitationGuestResponse(draft.guestDisplayName(), draft.guestEmail()),
                draft.defaultDurationMinutes(),
                draft.createdAt(),
                draft.updatedAt());
    }
}
