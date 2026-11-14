package com.github.ppj789.booking.invitation.service;

import java.util.List;

public final class InvitationDurationPolicy {

    private static final List<Integer> SUPPORTED_DURATIONS = List.of(15, 30, 45, 60, 120, 240);

    private InvitationDurationPolicy() {
    }

    public static boolean supports(int durationMinutes) {
        return SUPPORTED_DURATIONS.contains(durationMinutes);
    }

    public static List<Integer> supportedDurations() {
        return SUPPORTED_DURATIONS;
    }
}
