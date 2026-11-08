package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.service.AdminStaffService.StaffSetup;
import java.time.Instant;

public record StaffSetupResponse(
        ManagedStaffResponse staff,
        String setupUrl,
        Instant expiresAt) {

    static StaffSetupResponse from(
            StaffSetup setup,
            AccountSetupLinkFactory linkFactory) {
        return new StaffSetupResponse(
                ManagedStaffResponse.from(setup.staff()),
                linkFactory.create(setup.rawToken()),
                setup.expiresAt());
    }
}
