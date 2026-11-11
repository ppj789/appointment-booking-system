package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.invitation.service.StaffInvitationService;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationActivation;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationDraft;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationRevocation;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/invitations")
@Tag(name = "Staff invitations", description = "Private Staff Invitation lifecycle")
@SecurityRequirement(name = "sessionCookie")
@RequiredArgsConstructor
public class StaffInvitationController {

    private final StaffInvitationService invitationService;
    private final InvitationShareLinkFactory shareLinkFactory;

    @GetMapping
    @Operation(summary = "List owned Invitations", description = "Returns no capability values or share URLs.")
    @ApiResponse(responseCode = "200", description = "Owned Invitations returned")
    @ApiResponse(responseCode = "403", description = "Active Staff access is required")
    public ResponseEntity<List<InvitationSummaryResponse>> list(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor) {
        List<InvitationSummary> invitations = invitationService.list(actor);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(invitations.stream()
                        .map(InvitationSummaryResponse::new)
                        .toList());
    }

    @PostMapping
    @Operation(summary = "Create an Invitation draft")
    @ApiResponse(responseCode = "201", description = "Invitation draft created")
    @ApiResponse(responseCode = "403", description = "Active Staff access is required")
    @ApiResponse(responseCode = "422", description = "Request validation failed")
    public ResponseEntity<InvitationDraftResponse> create(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @Valid @RequestBody InvitationDraftRequest request) {
        InvitationDraft invitation = invitationService.create(
                actor,
                request.title(),
                request.description(),
                request.guest().displayName(),
                request.guest().email(),
                request.defaultDurationMinutes());
        return ResponseEntity.status(201)
                .cacheControl(CacheControl.noStore())
                .body(InvitationDraftResponse.from(invitation));
    }

    @PatchMapping("/{invitationId}")
    @Operation(summary = "Edit an owned Invitation draft")
    @ApiResponse(responseCode = "200", description = "Invitation draft updated")
    @ApiResponse(responseCode = "404", description = "Invitation is unavailable")
    @ApiResponse(responseCode = "409", description = "Invitation is not editable")
    @ApiResponse(responseCode = "422", description = "Request validation failed")
    public ResponseEntity<InvitationDraftResponse> update(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @PathVariable UUID invitationId,
            @Valid @RequestBody InvitationDraftRequest request) {
        InvitationDraft invitation = invitationService.update(
                actor,
                invitationId,
                request.title(),
                request.description(),
                request.guest().displayName(),
                request.guest().email(),
                request.defaultDurationMinutes());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(InvitationDraftResponse.from(invitation));
    }

    @PostMapping("/{invitationId}/activate")
    @Operation(
            summary = "Activate an Invitation",
            description = "Returns the fragment-based Guest share URL once.")
    @ApiResponse(responseCode = "200", description = "Invitation activated")
    @ApiResponse(responseCode = "404", description = "Invitation is unavailable")
    @ApiResponse(responseCode = "409", description = "Invitation is not activatable")
    public ResponseEntity<InvitationActivationResponse> activate(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @PathVariable UUID invitationId) {
        InvitationActivation activation = invitationService.activate(actor, invitationId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(InvitationActivationResponse.from(activation, shareLinkFactory));
    }

    @PostMapping("/{invitationId}/revoke")
    @Operation(summary = "Revoke an owned unused active Invitation")
    @ApiResponse(responseCode = "200", description = "Invitation revoked")
    @ApiResponse(responseCode = "404", description = "Invitation is unavailable")
    @ApiResponse(responseCode = "409", description = "Invitation is not revocable")
    public ResponseEntity<InvitationRevocationResponse> revoke(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @PathVariable UUID invitationId) {
        InvitationRevocation revocation = invitationService.revoke(actor, invitationId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(InvitationRevocationResponse.from(revocation));
    }
}
