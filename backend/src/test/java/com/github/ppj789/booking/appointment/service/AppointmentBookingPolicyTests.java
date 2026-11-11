package com.github.ppj789.booking.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.ppj789.booking.appointment.exception.AppointmentSlotOutsideHorizonException;
import com.github.ppj789.booking.appointment.exception.InvalidAppointmentSlotException;
import com.github.ppj789.booking.appointment.exception.UnsupportedAppointmentDurationException;
import com.github.ppj789.booking.appointment.service.AppointmentBookingPolicy.AppointmentInterval;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AppointmentBookingPolicyTests {

    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");

    @Test
    void returnsTheCompleteSupportedFutureInterval() {
        Instant startsAt = Instant.parse("2026-11-10T10:15:00Z");

        AppointmentInterval interval = AppointmentBookingPolicy.validate(NOW, startsAt, 60);

        assertEquals(startsAt, interval.startsAt());
        assertEquals(Instant.parse("2026-11-10T11:15:00Z"), interval.endsAt());
    }

    @Test
    void rejectsUnsupportedDurations() {
        UnsupportedAppointmentDurationException exception = assertThrows(
                UnsupportedAppointmentDurationException.class,
                () -> AppointmentBookingPolicy.validate(
                        NOW,
                        Instant.parse("2026-11-10T10:15:00Z"),
                        90));

        assertEquals("The selected Appointment duration is unsupported", exception.getMessage());
    }

    @Test
    void rejectsPastStartsAndIntervalsThatDoNotFitInsideTheHorizon() {
        AppointmentSlotOutsideHorizonException nonFuture = assertThrows(
                AppointmentSlotOutsideHorizonException.class,
                () -> AppointmentBookingPolicy.validate(NOW, NOW, 15));
        AppointmentSlotOutsideHorizonException horizonOverflow = assertThrows(
                AppointmentSlotOutsideHorizonException.class,
                () -> AppointmentBookingPolicy.validate(
                        NOW,
                        Instant.parse("2026-11-24T09:45:00Z"),
                        30));

        assertEquals(
                "The selected Appointment time is outside the booking horizon",
                nonFuture.getMessage());
        assertEquals(
                "The selected Appointment time is outside the booking horizon",
                horizonOverflow.getMessage());
    }

    @Test
    void rejectsStartsOutsideTheFifteenMinuteUtcGrid() {
        InvalidAppointmentSlotException exception = assertThrows(
                InvalidAppointmentSlotException.class,
                () -> AppointmentBookingPolicy.validate(
                        NOW,
                        Instant.parse("2026-11-10T10:16:00Z"),
                        15));

        assertEquals(
                "The selected Appointment time must use the 15-minute UTC grid",
                exception.getMessage());
    }
}
