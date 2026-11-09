package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.service.GuestInvitationService.GuestAvailability;
import java.time.Instant;
import java.util.List;

public record GuestAvailabilityResponse(
        int durationMinutes,
        Instant generatedAt,
        GuestAvailabilityWindowResponse window,
        List<GuestAvailabilitySlotResponse> slots) {

    static GuestAvailabilityResponse from(GuestAvailability availability) {
        return new GuestAvailabilityResponse(
                availability.durationMinutes(),
                availability.generatedAt(),
                new GuestAvailabilityWindowResponse(
                        availability.from(),
                        availability.toExclusive()),
                availability.slots().stream()
                        .map(GuestAvailabilitySlotResponse::from)
                        .toList());
    }
}
