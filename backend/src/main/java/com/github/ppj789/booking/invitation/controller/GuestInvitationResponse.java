package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.service.GuestInvitationService.GuestInvitation;
import java.util.List;

public record GuestInvitationResponse(
        String title,
        String description,
        String hostDisplayName,
        String guestDisplayName,
        short defaultDurationMinutes,
        List<Integer> supportedDurationsMinutes,
        GuestBranchResponse branch) {

    static GuestInvitationResponse from(GuestInvitation invitation) {
        return new GuestInvitationResponse(
                invitation.title(),
                invitation.description(),
                invitation.hostDisplayName(),
                invitation.guestDisplayName(),
                invitation.defaultDurationMinutes(),
                invitation.supportedDurationsMinutes(),
                GuestBranchResponse.from(invitation.branch()));
    }
}
