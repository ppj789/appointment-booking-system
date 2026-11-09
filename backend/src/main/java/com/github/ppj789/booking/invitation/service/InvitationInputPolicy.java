package com.github.ppj789.booking.invitation.service;

import com.github.ppj789.booking.invitation.exception.InvalidInvitationInputException;
import com.github.ppj789.booking.invitation.persistence.InvitationEntity;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class InvitationInputPolicy {

    public static final Set<Integer> SUPPORTED_DURATIONS = Set.of(15, 30, 45, 60, 120, 240);

    private static final Pattern EMAIL_SHAPE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private InvitationInputPolicy() {
    }

    public static InvitationDetails normalize(
            String title,
            String description,
            String guestDisplayName,
            String guestEmail,
            int defaultDurationMinutes) {
        String normalizedTitle = requiredText(title, 120, "Title");
        String normalizedDescription = requiredText(description, 2000, "Description");
        String normalizedGuestName = requiredText(guestDisplayName, 100, "Guest display name");
        String normalizedGuestEmail = normalizeEmail(guestEmail);
        if (!SUPPORTED_DURATIONS.contains(defaultDurationMinutes)) {
            throw new InvalidInvitationInputException("Default duration is unsupported");
        }
        return new InvitationDetails(
                normalizedTitle,
                normalizedDescription,
                normalizedGuestName,
                normalizedGuestEmail,
                (short) defaultDurationMinutes);
    }

    public static void validateForActivation(InvitationEntity invitation) {
        normalize(
                invitation.getTitle(),
                invitation.getDescription(),
                invitation.getGuestName(),
                invitation.getGuestEmail(),
                invitation.getDefaultDurationMinutes());
    }

    private static String requiredText(String input, int maximumLength, String fieldName) {
        String normalized = input == null ? "" : input.trim();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new InvalidInvitationInputException(
                    fieldName + " must contain between 1 and " + maximumLength + " characters");
        }
        return normalized;
    }

    private static String normalizeEmail(String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3
                || normalized.length() > 254
                || !EMAIL_SHAPE.matcher(normalized).matches()) {
            throw new InvalidInvitationInputException("Guest email address is invalid");
        }
        return normalized;
    }

    public record InvitationDetails(
            String title,
            String description,
            String guestDisplayName,
            String guestEmail,
            short defaultDurationMinutes) {
    }
}
