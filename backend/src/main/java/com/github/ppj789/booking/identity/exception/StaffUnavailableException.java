package com.github.ppj789.booking.identity.exception;

public class StaffUnavailableException extends RuntimeException {

    public StaffUnavailableException() {
        super("Staff account is unavailable");
    }
}
