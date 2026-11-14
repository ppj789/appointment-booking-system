package com.github.ppj789.booking.appointment.service;

import com.github.ppj789.booking.appointment.exception.AppointmentSlotOutsideHorizonException;
import com.github.ppj789.booking.appointment.exception.InvalidAppointmentSlotException;
import com.github.ppj789.booking.appointment.exception.UnsupportedAppointmentDurationException;
import com.github.ppj789.booking.invitation.service.InvitationDurationPolicy;
import java.time.Duration;
import java.time.Instant;

final class AppointmentBookingPolicy {

    private static final Duration BOOKING_HORIZON = Duration.ofDays(14);
    private static final Duration START_GRID = Duration.ofMinutes(15);

    private AppointmentBookingPolicy() {
    }

    static AppointmentInterval validate(
            Instant now,
            Instant startsAt,
            int durationMinutes) {
        if (!InvitationDurationPolicy.supports(durationMinutes)) {
            throw new UnsupportedAppointmentDurationException();
        }
        Instant horizonEnd = now.plus(BOOKING_HORIZON);
        if (startsAt == null
                || !startsAt.isAfter(now)
                || !startsAt.isBefore(horizonEnd)) {
            throw new AppointmentSlotOutsideHorizonException();
        }
        if (startsAt.getNano() != 0
                || Math.floorMod(startsAt.getEpochSecond(), START_GRID.toSeconds()) != 0) {
            throw new InvalidAppointmentSlotException();
        }

        Instant endsAt = startsAt.plus(Duration.ofMinutes(durationMinutes));
        if (endsAt.isAfter(horizonEnd)) {
            throw new AppointmentSlotOutsideHorizonException();
        }
        return new AppointmentInterval(startsAt, endsAt);
    }

    record AppointmentInterval(Instant startsAt, Instant endsAt) {
    }
}
