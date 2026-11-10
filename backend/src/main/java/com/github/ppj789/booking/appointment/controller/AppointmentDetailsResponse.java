package com.github.ppj789.booking.appointment.controller;

import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService.ConfirmationResult;
import java.time.Instant;
import java.util.UUID;

public record AppointmentDetailsResponse(
        UUID id,
        String reference,
        String title,
        String description,
        String hostDisplayName,
        String guestDisplayName,
        String guestEmail,
        Instant startAt,
        Instant endAt,
        Instant createdAt,
        AppointmentBranchResponse branch) {

    static AppointmentDetailsResponse from(ConfirmationResult result) {
        return new AppointmentDetailsResponse(
                result.appointmentId(),
                result.reference(),
                result.title(),
                result.description(),
                result.hostDisplayName(),
                result.guestDisplayName(),
                result.guestEmail(),
                result.startAt(),
                result.endAt(),
                result.createdAt(),
                AppointmentBranchResponse.from(result.branch()));
    }
}
