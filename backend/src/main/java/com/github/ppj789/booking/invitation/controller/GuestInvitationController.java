package com.github.ppj789.booking.invitation.controller;

import com.github.ppj789.booking.invitation.service.GuestInvitationService;
import com.github.ppj789.booking.invitation.service.GuestInvitationService.GuestAvailability;
import com.github.ppj789.booking.invitation.service.GuestInvitationService.GuestInvitation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guest")
@Tag(name = "Guest booking", description = "Invitation-scoped public booking access")
@SecurityRequirement(name = "invitationToken")
@Validated
@RequiredArgsConstructor
public class GuestInvitationController {

    static final String INVITATION_TOKEN_HEADER = "X-Invitation-Token";

    private final GuestInvitationService invitationService;

    @GetMapping("/invitation")
    @Operation(summary = "Read Guest-safe Invitation context")
    @ApiResponse(responseCode = "200", description = "Active Invitation context returned")
    @ApiResponse(responseCode = "404", description = "Invitation is unavailable")
    @ApiResponse(responseCode = "429", description = "Guest access rate limit exceeded")
    public ResponseEntity<GuestInvitationResponse> getInvitation(
            @RequestHeader(INVITATION_TOKEN_HEADER) String rawToken,
            HttpServletRequest request) {
        GuestInvitation invitation = invitationService.getInvitation(
                rawToken,
                clientKey(request));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(GuestInvitationResponse.from(invitation));
    }

    @GetMapping("/availability")
    @Operation(summary = "Read selectable Invitation-scoped availability")
    @ApiResponse(responseCode = "200", description = "Selectable UTC slots returned")
    @ApiResponse(responseCode = "404", description = "Invitation is unavailable")
    @ApiResponse(responseCode = "422", description = "Availability request is invalid")
    @ApiResponse(responseCode = "429", description = "Guest access rate limit exceeded")
    public ResponseEntity<GuestAvailabilityResponse> getAvailability(
            @RequestHeader(INVITATION_TOKEN_HEADER) String rawToken,
            @RequestParam
            @Parameter(description = "Supported Appointment duration in minutes")
            int durationMinutes,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Optional UTC lower bound; past values clamp to backend time")
            Instant from,
            @RequestParam(defaultValue = "14")
            @Min(1) @Max(14)
            @Parameter(description = "Requested window length from 1 to 14 days")
            int days,
            HttpServletRequest request) {
        GuestAvailability availability = invitationService.getAvailability(
                rawToken,
                clientKey(request),
                durationMinutes,
                from,
                days);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(GuestAvailabilityResponse.from(availability));
    }

    private String clientKey(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }
}
