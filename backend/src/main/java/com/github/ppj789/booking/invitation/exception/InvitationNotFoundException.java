package com.github.ppj789.booking.invitation.exception;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException() {
        super("Invitation is unavailable");
    }
}
