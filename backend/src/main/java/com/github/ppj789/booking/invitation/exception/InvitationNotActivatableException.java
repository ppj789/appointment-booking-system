package com.github.ppj789.booking.invitation.exception;

public class InvitationNotActivatableException extends RuntimeException {

    public InvitationNotActivatableException() {
        super("Only a draft Invitation can be activated");
    }
}
