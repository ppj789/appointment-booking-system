package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.service.AdminStaffService.ManagedStaff;
import java.util.UUID;

public record ManagedStaffResponse(
        UUID id,
        String displayName,
        String email,
        AccountStatus status,
        ManagedBranchResponse branch) {

    static ManagedStaffResponse from(ManagedStaff staff) {
        return new ManagedStaffResponse(
                staff.id(),
                staff.displayName(),
                staff.email(),
                staff.status(),
                ManagedBranchResponse.from(staff.branch()));
    }
}
