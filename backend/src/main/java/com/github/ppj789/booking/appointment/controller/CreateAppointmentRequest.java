package com.github.ppj789.booking.appointment.controller;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateAppointmentRequest(
        @NotNull Instant startAt,
        @NotNull Integer durationMinutes) {
}
