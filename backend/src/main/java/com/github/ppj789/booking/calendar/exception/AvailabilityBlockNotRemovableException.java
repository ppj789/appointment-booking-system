package com.github.ppj789.booking.calendar.exception;

public class AvailabilityBlockNotRemovableException extends RuntimeException {

    public AvailabilityBlockNotRemovableException() {
        super("Only a future Availability Block can be removed");
    }
}
