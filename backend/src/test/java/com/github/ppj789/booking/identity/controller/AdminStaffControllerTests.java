package com.github.ppj789.booking.identity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.service.AdminStaffService;
import com.github.ppj789.booking.identity.service.AdminStaffService.ManagedBranch;
import com.github.ppj789.booking.identity.service.AdminStaffService.ManagedStaff;
import com.github.ppj789.booking.identity.service.AdminStaffService.StaffSetup;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class AdminStaffControllerTests {

    @Test
    void provisioningReturnsANoStoreFragmentSetupUrl() {
        AdminStaffService service = mock(AdminStaffService.class);
        AccountSetupLinkFactory linkFactory = new AccountSetupLinkFactory();
        AuthenticatedAccountPrincipal actor = mock(AuthenticatedAccountPrincipal.class);
        CreateStaffRequest request = new CreateStaffRequest(
                "JHB-DEMO",
                "Demo Staff",
                "staff@example.test");
        Instant expiresAt = Instant.parse("2026-11-09T10:00:00Z");
        ManagedStaff staff = new ManagedStaff(
                UUID.randomUUID(),
                "Demo Staff",
                "staff@example.test",
                AccountStatus.PENDING_SETUP,
                new ManagedBranch(
                        "JHB-DEMO",
                        "Johannesburg Demo Branch",
                        "1 Demo Street, Johannesburg",
                        "Africa/Johannesburg"));
        when(service.provision(actor, "JHB-DEMO", "Demo Staff", "staff@example.test"))
                .thenReturn(new StaffSetup(staff, "one-time-secret", expiresAt));
        AdminStaffController controller = new AdminStaffController(service, linkFactory);

        ResponseEntity<StaffSetupResponse> response = controller.provision(actor, request);
        StaffSetupResponse body = response.getBody();

        assertEquals(201, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertEquals("/account-setup#token=one-time-secret", body.setupUrl());
        assertEquals(expiresAt, body.expiresAt());
        assertEquals("staff@example.test", body.staff().email());
        assertFalse(body.setupUrl().contains("?token="));
    }
}
