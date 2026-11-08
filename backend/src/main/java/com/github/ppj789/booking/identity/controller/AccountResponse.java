package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;

public record AccountResponse(
        String displayName,
        AccountType accountType) {

    static AccountResponse from(AuthenticatedAccountPrincipal principal) {
        return new AccountResponse(
                principal.displayName(),
                principal.accountType());
    }
}
