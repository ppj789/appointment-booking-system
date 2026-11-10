package com.github.ppj789.booking.appointment.controller;

import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService.ConfirmationResult;

public record AppointmentConfirmationResponse(
        AppointmentDetailsResponse appointment,
        SimulatedConfirmationResponse confirmation) {

    static AppointmentConfirmationResponse from(ConfirmationResult result) {
        return new AppointmentConfirmationResponse(
                AppointmentDetailsResponse.from(result),
                SimulatedConfirmationResponse.confirmed(result.reference()));
    }
}
