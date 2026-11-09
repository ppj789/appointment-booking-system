package com.github.ppj789.booking.calendar.controller;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateAvailabilityBlockRequest(
        @NotNull Instant startAt,
        @NotNull Instant endAt) {
}
