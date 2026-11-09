package com.github.ppj789.booking.invitation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.ppj789.booking.invitation.exception.InvalidGuestAvailabilityException;
import com.github.ppj789.booking.invitation.service.GuestAvailabilityPolicy.AvailabilitySlot;
import com.github.ppj789.booking.invitation.service.GuestAvailabilityPolicy.AvailabilityWindow;
import com.github.ppj789.booking.invitation.service.GuestAvailabilityPolicy.OccupiedInterval;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuestAvailabilityPolicyTests {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    @Test
    void alignsTheFirstSelectableSlotStrictlyAfterBackendTime() {
        AvailabilityWindow window = GuestAvailabilityPolicy.createWindow(NOW, null, 1, 30);

        assertEquals(Instant.parse("2026-11-09T10:15:00Z"), window.from());
        assertEquals(Instant.parse("2026-11-10T10:15:00Z"), window.toExclusive());
    }

    @Test
    void removesEverySlotWhoseFullDurationOverlapsOccupancy() {
        AvailabilityWindow window = new AvailabilityWindow(
                Instant.parse("2026-11-09T10:00:00Z"),
                Instant.parse("2026-11-09T11:30:00Z"),
                30);
        List<OccupiedInterval> occupied = List.of(new OccupiedInterval(
                Instant.parse("2026-11-09T10:30:00Z"),
                Instant.parse("2026-11-09T11:00:00Z")));

        List<AvailabilitySlot> slots = GuestAvailabilityPolicy.availableSlots(window, occupied);

        assertEquals(List.of(
                new AvailabilitySlot(
                        Instant.parse("2026-11-09T10:00:00Z"),
                        Instant.parse("2026-11-09T10:30:00Z")),
                new AvailabilitySlot(
                        Instant.parse("2026-11-09T11:00:00Z"),
                        Instant.parse("2026-11-09T11:30:00Z"))), slots);
    }

    @Test
    void capsTheRequestedWindowAtTheRollingFourteenDayHorizon() {
        AvailabilityWindow window = GuestAvailabilityPolicy.createWindow(
                NOW,
                NOW.plusSeconds(13 * 24 * 60 * 60),
                14,
                60);

        assertEquals(NOW.plusSeconds(14 * 24 * 60 * 60), window.toExclusive());
    }

    @Test
    void rejectsUnsupportedDurationAndOutOfRangeDays() {
        InvalidGuestAvailabilityException durationFailure = assertThrows(
                InvalidGuestAvailabilityException.class,
                () -> GuestAvailabilityPolicy.createWindow(NOW, null, 1, 90));
        InvalidGuestAvailabilityException daysFailure = assertThrows(
                InvalidGuestAvailabilityException.class,
                () -> GuestAvailabilityPolicy.createWindow(NOW, null, 15, 30));

        assertEquals("Duration is unsupported", durationFailure.getMessage());
        assertEquals("Days must be between 1 and 14", daysFailure.getMessage());
    }
}
