package com.github.ppj789.booking.appointment.exception;

public class InvalidAppointmentSlotException extends RuntimeException {

    public InvalidAppointmentSlotException() {
        super("The selected Appointment time must use the 15-minute UTC grid");
    }
}
