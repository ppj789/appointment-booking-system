package com.github.ppj789.booking.appointment.exception;

public class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException() {
        super("The selected Appointment time is unavailable");
    }
}
