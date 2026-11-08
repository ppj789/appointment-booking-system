package com.github.ppj789.booking.identity.exception;

public class BranchUnavailableException extends RuntimeException {

    public BranchUnavailableException() {
        super("Branch is unavailable");
    }
}
