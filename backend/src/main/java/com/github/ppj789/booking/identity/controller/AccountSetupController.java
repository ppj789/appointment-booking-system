package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.service.AccountSetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account-setup")
@Tag(name = "Account setup", description = "One-time initial Staff credential setup")
@RequiredArgsConstructor
public class AccountSetupController {

    private final AccountSetupService accountSetupService;

    @PostMapping
    @Operation(
            summary = "Complete Staff account setup",
            description = "Sets the first password, consumes the setup capability, and activates the Staff account.")
    @SecurityRequirement(name = "setupToken")
    @ApiResponse(responseCode = "200", description = "Staff account activated")
    @ApiResponse(responseCode = "404", description = "Setup link is invalid, expired, consumed, or revoked")
    @ApiResponse(responseCode = "422", description = "Password does not meet the policy")
    @ApiResponse(responseCode = "429", description = "Too many attempts")
    public ResponseEntity<AccountContextResponse> complete(
            @Parameter(description = "Raw capability read from the frontend URL fragment")
            @RequestHeader(name = "X-Setup-Token", required = false) String setupToken,
            @Valid @RequestBody CompleteAccountSetupRequest setupRequest) {
        AuthenticatedAccountPrincipal principal =
                accountSetupService.complete(setupToken, setupRequest.password());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(AccountContextResponse.from(principal));
    }
}
