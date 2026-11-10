package com.github.ppj789.booking.appointment.exception;

public class UnsupportedAppointmentDurationException extends RuntimeException {

    public UnsupportedAppointmentDurationException() {
        super("The selected Appointment duration is unsupported");
    }
}
