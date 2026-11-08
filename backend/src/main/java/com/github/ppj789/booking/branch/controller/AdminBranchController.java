package com.github.ppj789.booking.branch.controller;

import com.github.ppj789.booking.branch.service.AdminBranchQueryService;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/branches")
@Tag(name = "Admin Branches", description = "Read-only Branch choices for Staff administration")
@SecurityRequirement(name = "sessionCookie")
@RequiredArgsConstructor
public class AdminBranchController {

    private final AdminBranchQueryService branchQueryService;

    @GetMapping
    @Operation(summary = "List active Branches")
    @ApiResponse(responseCode = "200", description = "Configured Branches returned")
    @ApiResponse(responseCode = "403", description = "Active Admin access is required")
    public List<AdminBranchResponse> list(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor) {
        return branchQueryService.listActive(actor).stream()
                .map(branch -> AdminBranchResponse.from(branch))
                .toList();
    }
}
