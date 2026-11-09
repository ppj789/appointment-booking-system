package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.service.GuestInvitationService.GuestBranch;

public record GuestBranchResponse(
        String publicCode,
        String name,
        String displayAddress,
        String timezone) {

    static GuestBranchResponse from(GuestBranch branch) {
        return new GuestBranchResponse(
                branch.publicCode(),
                branch.name(),
                branch.displayAddress(),
                branch.timezone());
    }
}
