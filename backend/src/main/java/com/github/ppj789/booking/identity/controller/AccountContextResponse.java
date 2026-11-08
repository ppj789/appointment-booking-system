package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;

public record AccountContextResponse(AccountResponse account, BranchResponse branch) {

    static AccountContextResponse from(AuthenticatedAccountPrincipal principal) {
        return new AccountContextResponse(
                AccountResponse.from(principal),
                BranchResponse.from(principal.branch()));
    }
}
