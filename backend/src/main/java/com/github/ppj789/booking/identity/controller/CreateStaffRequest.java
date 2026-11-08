package com.github.ppj789.booking.identity.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank @Size(max = 30) String branchCode,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Email @Size(max = 254) String email) {
}
