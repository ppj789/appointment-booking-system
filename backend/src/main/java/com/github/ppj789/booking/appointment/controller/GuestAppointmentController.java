package com.github.ppj789.booking.appointment.controller;

import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService;
import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService.ConfirmationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guest/appointments")
@Tag(name = "Guest booking", description = "Invitation-scoped public booking access")
@SecurityRequirement(name = "invitationToken")
@Validated
@RequiredArgsConstructor
public class GuestAppointmentController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    static final String INVITATION_TOKEN_HEADER = "X-Invitation-Token";
    static final String IDEMPOTENCY_REPLAYED_HEADER = "Idempotency-Replayed";

    private final AppointmentConfirmationService confirmationService;

    @PostMapping
    @Operation(summary = "Confirm one Invitation-scoped Appointment atomically")
    @ApiResponse(responseCode = "201", description = "Appointment committed and confirmed")
    @ApiResponse(responseCode = "200", description = "Original confirmation replayed")
    @ApiResponse(responseCode = "404", description = "Invitation is unavailable")
    @ApiResponse(responseCode = "409", description = "Slot or Idempotency Key conflict")
    @ApiResponse(responseCode = "422", description = "Appointment selection is invalid")
    public ResponseEntity<AppointmentConfirmationResponse> confirm(
            @RequestHeader(INVITATION_TOKEN_HEADER) String rawToken,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER)
            @Parameter(description = "Client-generated UUID for safe command replay")
            UUID idempotencyKey,
            @Valid @RequestBody CreateAppointmentRequest request) {
        ConfirmationResult result = confirmationService.confirm(
                rawToken,
                idempotencyKey,
                request.startAt(),
                request.durationMinutes());

        ResponseEntity.BodyBuilder response = ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore());
        if (result.replayed()) {
            response.header(IDEMPOTENCY_REPLAYED_HEADER, Boolean.TRUE.toString());
        }
        return response.body(AppointmentConfirmationResponse.from(result));
    }
}
