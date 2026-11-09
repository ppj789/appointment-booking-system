package com.github.ppj789.booking.invitation.exception;

public class InvitationUnavailableException extends RuntimeException {

    public InvitationUnavailableException() {
        super("Invitation is unavailable");
    }
}
