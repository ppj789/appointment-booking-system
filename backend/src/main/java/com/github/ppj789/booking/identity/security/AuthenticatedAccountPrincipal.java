package com.github.ppj789.booking.identity.security;

import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public record AuthenticatedAccountPrincipal(
        UUID accountId,
        String displayName,
        String email,
        AccountType accountType,
        AccountStatus status,
        BranchPrincipal branch) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public record BranchPrincipal(
            UUID id,
            String publicCode,
            String name,
            String displayAddress,
            String timezone) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
