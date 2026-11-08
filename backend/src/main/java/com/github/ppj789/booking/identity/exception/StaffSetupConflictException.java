package com.github.ppj789.booking.identity.exception;

public class StaffSetupConflictException extends RuntimeException {

    public StaffSetupConflictException() {
        super("Staff account is not waiting for setup");
    }
}
