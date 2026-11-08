package com.github.ppj789.booking.identity.exception;

public class SetupUnavailableException extends RuntimeException {

    public SetupUnavailableException() {
        super("Account setup is unavailable");
    }
}
