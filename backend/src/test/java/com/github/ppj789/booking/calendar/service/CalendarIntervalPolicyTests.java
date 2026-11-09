package com.github.ppj789.booking.calendar.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.ppj789.booking.calendar.exception.InvalidCalendarIntervalException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CalendarIntervalPolicyTests {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    @Test
    void acceptsAThirtyOneDayCalendarWindow() {
        assertDoesNotThrow(() -> CalendarIntervalPolicy.validateQueryWindow(
                NOW,
                NOW.plus(Duration.ofDays(31))));
    }

    @Test
    void rejectsAnOversizedOrReversedCalendarWindow() {
        InvalidCalendarIntervalException oversized = assertThrows(
                InvalidCalendarIntervalException.class,
                () -> CalendarIntervalPolicy.validateQueryWindow(
                        NOW,
                        NOW.plus(Duration.ofDays(31)).plusSeconds(1)));
        InvalidCalendarIntervalException reversed = assertThrows(
                InvalidCalendarIntervalException.class,
                () -> CalendarIntervalPolicy.validateQueryWindow(NOW, NOW));

        assertEquals("Calendar query window cannot exceed 31 days", oversized.getMessage());
        assertEquals("Calendar query start must be before its end", reversed.getMessage());
    }

    @Test
    void acceptsAFutureBlockOnTheFifteenMinuteGrid() {
        assertDoesNotThrow(() -> CalendarIntervalPolicy.validateFutureBlock(
                Instant.parse("2026-11-09T10:15:00Z"),
                Instant.parse("2026-11-09T11:45:00Z"),
                NOW));
    }

    @Test
    void rejectsPastReversedAndMisalignedBlocks() {
        InvalidCalendarIntervalException past = assertThrows(
                InvalidCalendarIntervalException.class,
                () -> CalendarIntervalPolicy.validateFutureBlock(
                        NOW,
                        NOW.plusSeconds(900),
                        NOW));
        InvalidCalendarIntervalException reversed = assertThrows(
                InvalidCalendarIntervalException.class,
                () -> CalendarIntervalPolicy.validateFutureBlock(
                        NOW.plusSeconds(1800),
                        NOW.plusSeconds(900),
                        NOW));
        InvalidCalendarIntervalException misaligned = assertThrows(
                InvalidCalendarIntervalException.class,
                () -> CalendarIntervalPolicy.validateFutureBlock(
                        NOW.plusSeconds(60),
                        NOW.plusSeconds(960),
                        NOW));

        assertEquals("Availability Block start must be in the future", past.getMessage());
        assertEquals("Availability Block start must be before its end", reversed.getMessage());
        assertEquals(
                "Availability Block boundaries must use the 15-minute UTC grid",
                misaligned.getMessage());
    }
}
