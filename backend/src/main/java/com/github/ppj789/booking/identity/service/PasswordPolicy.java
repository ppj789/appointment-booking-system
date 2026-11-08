package com.github.ppj789.booking.identity.service;

import com.github.ppj789.booking.identity.exception.InvalidPasswordException;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new InvalidPasswordException(
                    "Password must contain between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
        }
    }
}
