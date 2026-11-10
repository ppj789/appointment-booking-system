package com.github.ppj789.booking.appointment.exception;

public class AppointmentSlotOutsideHorizonException extends RuntimeException {

    public AppointmentSlotOutsideHorizonException() {
        super("The selected Appointment time is outside the booking horizon");
    }
}
