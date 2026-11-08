package com.github.ppj789.booking.identity.exception;

import lombok.Getter;

@Getter
public class StaffDeactivationBlockedException extends RuntimeException {

    private final boolean activeInvitation;
    private final boolean futureAppointment;

    public StaffDeactivationBlockedException(
            boolean activeInvitation,
            boolean futureAppointment) {
        super("Staff deactivation is blocked by active customer work");
        this.activeInvitation = activeInvitation;
        this.futureAppointment = futureAppointment;
    }
}
