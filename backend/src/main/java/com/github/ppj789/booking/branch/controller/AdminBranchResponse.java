package com.github.ppj789.booking.branch.controller;

import com.github.ppj789.booking.branch.service.AdminBranchQueryService.BranchView;

public record AdminBranchResponse(
        String publicCode,
        String name,
        String displayAddress,
        String timezone) {

    static AdminBranchResponse from(BranchView branch) {
        return new AdminBranchResponse(
                branch.publicCode(),
                branch.name(),
                branch.displayAddress(),
                branch.timezone());
    }
}
