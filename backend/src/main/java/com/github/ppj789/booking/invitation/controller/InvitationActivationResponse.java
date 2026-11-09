package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationActivation;
import java.time.Instant;
import java.util.UUID;

public record InvitationActivationResponse(
        UUID id,
        InvitationStatus status,
        Instant activatedAt,
        String shareUrl) {

    static InvitationActivationResponse from(
            InvitationActivation activation,
            InvitationShareLinkFactory linkFactory) {
        return new InvitationActivationResponse(
                activation.id(),
                activation.status(),
                activation.activatedAt(),
                linkFactory.create(activation.rawToken()));
    }
}
