package com.github.ppj789.booking.identity.security;

import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import org.springframework.security.access.AccessDeniedException;

public final class AccountAccessPolicy {

    private AccountAccessPolicy() {
    }

    public static void requireActiveAdmin(AuthenticatedAccountPrincipal principal) {
        if (principal == null
                || principal.accountType() != AccountType.ADMIN
                || principal.status() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException("Active Admin access is required");
        }
    }

    public static void requireActiveStaff(AuthenticatedAccountPrincipal principal) {
        if (principal == null
                || principal.accountType() != AccountType.STAFF
                || principal.status() != AccountStatus.ACTIVE
                || principal.branch() == null) {
            throw new AccessDeniedException("Active Staff access is required");
        }
    }
}
