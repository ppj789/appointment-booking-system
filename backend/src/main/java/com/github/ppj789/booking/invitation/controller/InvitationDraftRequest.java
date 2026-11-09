package com.github.ppj789.booking.invitation.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvitationDraftRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @Valid InvitationGuestRequest guest,
        @NotNull
        @Schema(allowableValues = {"15", "30", "45", "60", "120", "240"})
        Integer defaultDurationMinutes) {
}
