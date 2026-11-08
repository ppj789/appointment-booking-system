package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.service.AdminStaffService.ManagedBranch;

public record ManagedBranchResponse(
        String publicCode,
        String name,
        String displayAddress,
        String timezone) {

    static ManagedBranchResponse from(ManagedBranch branch) {
        return new ManagedBranchResponse(
                branch.publicCode(),
                branch.name(),
                branch.displayAddress(),
                branch.timezone());
    }
}
