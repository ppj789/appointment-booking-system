package com.github.ppj789.booking.calendar.service;

import com.github.ppj789.booking.calendar.exception.InvalidCalendarIntervalException;
import java.time.Duration;
import java.time.Instant;

public final class CalendarIntervalPolicy {

    public static final Duration MAXIMUM_QUERY_WINDOW = Duration.ofDays(31);
    public static final Duration START_GRID = Duration.ofMinutes(15);

    private CalendarIntervalPolicy() {
    }

    public static void validateQueryWindow(Instant from, Instant to) {
        requirePresent(from, to);
        if (!from.isBefore(to)) {
            throw new InvalidCalendarIntervalException("Calendar query start must be before its end");
        }
        if (Duration.between(from, to).compareTo(MAXIMUM_QUERY_WINDOW) > 0) {
            throw new InvalidCalendarIntervalException("Calendar query window cannot exceed 31 days");
        }
    }

    public static void validateFutureBlock(Instant startsAt, Instant endsAt, Instant now) {
        requirePresent(startsAt, endsAt);
        if (!startsAt.isAfter(now)) {
            throw new InvalidCalendarIntervalException("Availability Block start must be in the future");
        }
        if (!startsAt.isBefore(endsAt)) {
            throw new InvalidCalendarIntervalException(
                    "Availability Block start must be before its end");
        }
        if (!isAligned(startsAt) || !isAligned(endsAt)) {
            throw new InvalidCalendarIntervalException(
                    "Availability Block boundaries must use the 15-minute UTC grid");
        }
    }

    private static void requirePresent(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new InvalidCalendarIntervalException("Both interval boundaries are required");
        }
    }

    private static boolean isAligned(Instant instant) {
        return instant.getNano() == 0
                && Math.floorMod(instant.getEpochSecond(), START_GRID.toSeconds()) == 0;
    }
}
