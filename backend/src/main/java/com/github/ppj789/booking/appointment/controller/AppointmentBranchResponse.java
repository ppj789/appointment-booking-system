package com.github.ppj789.booking.appointment.controller;

import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService.ConfirmationBranch;

public record AppointmentBranchResponse(
        String publicCode,
        String name,
        String displayAddress,
        String timezone) {

    static AppointmentBranchResponse from(ConfirmationBranch branch) {
        return new AppointmentBranchResponse(
                branch.publicCode(),
                branch.name(),
                branch.displayAddress(),
                branch.timezone());
    }
}
