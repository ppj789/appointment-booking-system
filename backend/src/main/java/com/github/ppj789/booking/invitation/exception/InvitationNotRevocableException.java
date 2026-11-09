package com.github.ppj789.booking.invitation.exception;

public class InvitationNotRevocableException extends RuntimeException {

    public InvitationNotRevocableException() {
        super("Only an active unused Invitation can be revoked");
    }
}
