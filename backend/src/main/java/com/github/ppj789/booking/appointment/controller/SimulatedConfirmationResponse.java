package com.github.ppj789.booking.appointment.controller;

public record SimulatedConfirmationResponse(
        String status,
        String message,
        String reference) {

    static SimulatedConfirmationResponse confirmed(String reference) {
        return new SimulatedConfirmationResponse(
                "CONFIRMED",
                "Your appointment is confirmed.",
                reference);
    }
}
