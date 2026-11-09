package com.github.ppj789.booking.calendar.exception;

public class AvailabilityBlockNotFoundException extends RuntimeException {

    public AvailabilityBlockNotFoundException() {
        super("Availability Block is unavailable");
    }
}
