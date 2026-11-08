package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.service.AdminStaffService;
import com.github.ppj789.booking.identity.service.AdminStaffService.StaffSetup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/staff")
@Tag(name = "Admin Staff", description = "Branch-bound Staff account administration")
@SecurityRequirement(name = "sessionCookie")
@Validated
@RequiredArgsConstructor
public class AdminStaffController {

    private final AdminStaffService staffService;
    private final AccountSetupLinkFactory setupLinkFactory;

    @GetMapping
    @Operation(summary = "List Staff", description = "Lists Staff across Branches or within one Branch.")
    @ApiResponse(responseCode = "200", description = "Staff returned")
    @ApiResponse(responseCode = "403", description = "Active Admin access is required")
    public List<ManagedStaffResponse> list(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @RequestParam(required = false)
            @Size(max = 30)
            @Parameter(description = "Optional Branch public code")
            String branchCode) {
        return staffService.list(actor, branchCode).stream()
                .map(staff -> ManagedStaffResponse.from(staff))
                .toList();
    }

    @PostMapping
    @Operation(
            summary = "Provision Staff",
            description = "Creates pending Staff and returns the one-time setup URL once.")
    @ApiResponse(responseCode = "201", description = "Staff provisioned")
    @ApiResponse(responseCode = "404", description = "Branch is unavailable")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @ApiResponse(responseCode = "422", description = "Request validation failed")
    public ResponseEntity<StaffSetupResponse> provision(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @Valid @RequestBody CreateStaffRequest request) {
        StaffSetup setup = staffService.provision(
                actor,
                request.branchCode(),
                request.displayName(),
                request.email());
        return ResponseEntity.status(201)
                .cacheControl(CacheControl.noStore())
                .body(StaffSetupResponse.from(setup, setupLinkFactory));
    }

    @PostMapping("/{staffId}/setup-replacement")
    @Operation(
            summary = "Replace Staff setup",
            description = "Revokes any unused setup capability and returns a replacement once.")
    @ApiResponse(responseCode = "200", description = "Replacement setup returned")
    @ApiResponse(responseCode = "404", description = "Staff is unavailable")
    @ApiResponse(responseCode = "409", description = "Staff is not waiting for setup")
    public ResponseEntity<StaffSetupResponse> replaceSetup(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @PathVariable UUID staffId) {
        StaffSetup setup = staffService.replaceSetup(actor, staffId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(StaffSetupResponse.from(setup, setupLinkFactory));
    }

    @PostMapping("/{staffId}/deactivate")
    @Operation(
            summary = "Deactivate Staff",
            description = "Soft-deactivates Staff when no active customer work would be orphaned.")
    @ApiResponse(responseCode = "200", description = "Staff is deactivated")
    @ApiResponse(responseCode = "404", description = "Staff is unavailable")
    @ApiResponse(responseCode = "409", description = "Active Invitation or future Appointment blocks deactivation")
    public ManagedStaffResponse deactivate(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @PathVariable UUID staffId) {
        return ManagedStaffResponse.from(staffService.deactivate(actor, staffId));
    }
}
