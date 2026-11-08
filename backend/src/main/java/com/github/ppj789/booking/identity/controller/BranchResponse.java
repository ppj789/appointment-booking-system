package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal.BranchPrincipal;

public record BranchResponse(
        String publicCode,
        String name,
        String displayAddress,
        String timezone) {

    static BranchResponse from(BranchPrincipal branch) {
        if (branch == null) {
            return null;
        }
        return new BranchResponse(
                branch.publicCode(),
                branch.name(),
                branch.displayAddress(),
                branch.timezone());
    }
}
