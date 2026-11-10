package com.github.ppj789.booking.identity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.ppj789.booking.identity.service.AccountSetupService;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AccountSetupControllerTests {

    @Test
    void setupActivationReturnsNoSessionContext() {
        AccountSetupService service = mock(AccountSetupService.class);
        AccountSetupController controller = new AccountSetupController(service);

        ResponseEntity<Void> response = controller.complete(
                "one-time-setup-token",
                new CompleteAccountSetupRequest("valid-password"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(CacheControl.noStore().getHeaderValue(), response.getHeaders().getCacheControl());
        verify(service).complete("one-time-setup-token", "valid-password");
    }
}
