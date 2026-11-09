package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.service.GuestInvitationService.GuestSlot;
import java.time.Instant;

public record GuestAvailabilitySlotResponse(
        Instant startAt,
        Instant endAt) {

    static GuestAvailabilitySlotResponse from(GuestSlot slot) {
        return new GuestAvailabilitySlotResponse(slot.startAt(), slot.endAt());
    }
}
