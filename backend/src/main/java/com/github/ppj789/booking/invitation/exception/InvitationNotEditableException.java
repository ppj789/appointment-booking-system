package com.github.ppj789.booking.invitation.exception;

public class InvitationNotEditableException extends RuntimeException {

    public InvitationNotEditableException() {
        super("Only a draft Invitation can be edited");
    }
}
