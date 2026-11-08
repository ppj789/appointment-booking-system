package com.github.ppj789.booking.identity.service;

import com.github.ppj789.booking.identity.exception.InvalidAccountInputException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AccountInputNormalizer {

    private static final int MAX_DISPLAY_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final Pattern EMAIL_SHAPE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private AccountInputNormalizer() {
    }

    public static String displayName(String input) {
        String normalized = input == null ? "" : input.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new InvalidAccountInputException("Display name must contain between 1 and 100 characters");
        }
        return normalized;
    }

    public static String email(String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3
                || normalized.length() > MAX_EMAIL_LENGTH
                || !EMAIL_SHAPE.matcher(normalized).matches()) {
            throw new InvalidAccountInputException("Email address is invalid");
        }
        return normalized;
    }
}
