package com.github.ppj789.booking.identity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountResponseTests {

    @Test
    void exposesOnlyFieldsRequiredByTheFrontend() {
        AuthenticatedAccountPrincipal principal = new AuthenticatedAccountPrincipal(
                UUID.randomUUID(),
                "Local Admin",
                "admin@example.test",
                AccountType.ADMIN,
                AccountStatus.ACTIVE,
                null);

        AccountResponse response = AccountResponse.from(principal);
        List<String> responseFields = Arrays.stream(AccountResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertEquals(List.of("displayName", "accountType"), responseFields);
        assertEquals("Local Admin", response.displayName());
        assertEquals(AccountType.ADMIN, response.accountType());
    }
}
