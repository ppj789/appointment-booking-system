package com.github.ppj789.booking.invitation.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitationGuestRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Email @Size(max = 254) String email) {
}
