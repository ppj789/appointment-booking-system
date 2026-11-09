package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationRevocation;
import java.time.Instant;
import java.util.UUID;

public record InvitationRevocationResponse(
        UUID id,
        InvitationStatus status,
        Instant revokedAt) {

    static InvitationRevocationResponse from(InvitationRevocation revocation) {
        return new InvitationRevocationResponse(
                revocation.id(),
                revocation.status(),
                revocation.revokedAt());
    }
}
