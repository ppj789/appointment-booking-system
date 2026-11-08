package com.github.ppj789.booking.identity.controller;

import com.github.ppj789.booking.identity.service.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteAccountSetupRequest(
        @NotBlank
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        String password) {
}
