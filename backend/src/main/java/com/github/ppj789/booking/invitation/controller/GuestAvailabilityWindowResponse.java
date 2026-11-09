package com.github.ppj789.booking.invitation.controller;

import java.time.Instant;

public record GuestAvailabilityWindowResponse(
        Instant from,
        Instant toExclusive) {
}
