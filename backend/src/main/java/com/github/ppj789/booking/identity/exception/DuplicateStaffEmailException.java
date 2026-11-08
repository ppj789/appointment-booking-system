package com.github.ppj789.booking.identity.exception;

public class DuplicateStaffEmailException extends RuntimeException {

    public DuplicateStaffEmailException() {
        super("A Staff account already uses this email address");
    }

    public DuplicateStaffEmailException(Throwable cause) {
        super("A Staff account already uses this email address", cause);
    }
}
