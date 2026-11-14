package com.github.ppj789.booking.invitation.service;

import com.github.ppj789.booking.invitation.exception.InvalidGuestAvailabilityException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

final class GuestAvailabilityPolicy {

    private static final int MAXIMUM_DAYS = 14;
    private static final long SLOT_SECONDS = Duration.ofMinutes(15).toSeconds();

    private GuestAvailabilityPolicy() {
    }

    static AvailabilityWindow createWindow(
            Instant now,
            Instant requestedFrom,
            int days,
            int durationMinutes) {
        if (!InvitationDurationPolicy.supports(durationMinutes)) {
            throw new InvalidGuestAvailabilityException("Duration is unsupported");
        }
        if (days < 1 || days > MAXIMUM_DAYS) {
            throw new InvalidGuestAvailabilityException("Days must be between 1 and 14");
        }

        Instant lowerBound = requestedFrom == null || requestedFrom.isBefore(now)
                ? now
                : requestedFrom;
        Instant from = alignToNextSelectableBoundary(lowerBound, now);
        Instant horizonEnd = now.plus(MAXIMUM_DAYS, ChronoUnit.DAYS);
        if (!from.isBefore(horizonEnd)) {
            throw new InvalidGuestAvailabilityException(
                    "Availability start must be inside the booking horizon");
        }
        Instant requestedEnd = from.plus(days, ChronoUnit.DAYS);
        Instant toExclusive = requestedEnd.isBefore(horizonEnd) ? requestedEnd : horizonEnd;
        return new AvailabilityWindow(from, toExclusive, durationMinutes);
    }

    static List<AvailabilitySlot> availableSlots(
            AvailabilityWindow window,
            List<OccupiedInterval> occupiedIntervals) {
        Duration duration = Duration.ofMinutes(window.durationMinutes());
        List<AvailabilitySlot> slots = new ArrayList<>();
        for (Instant start = window.from();
                !start.plus(duration).isAfter(window.toExclusive());
                start = start.plusSeconds(SLOT_SECONDS)) {
            Instant end = start.plus(duration);
            if (isFree(start, end, occupiedIntervals)) {
                slots.add(new AvailabilitySlot(start, end));
            }
        }
        return List.copyOf(slots);
    }

    private static Instant alignToNextSelectableBoundary(Instant lowerBound, Instant now) {
        long epochSecond = lowerBound.getEpochSecond();
        long remainder = Math.floorMod(epochSecond, SLOT_SECONDS);
        boolean aligned = remainder == 0 && lowerBound.getNano() == 0;
        long alignedSecond = aligned ? epochSecond : epochSecond + SLOT_SECONDS - remainder;
        Instant result = Instant.ofEpochSecond(alignedSecond);
        return result.isAfter(now) ? result : result.plusSeconds(SLOT_SECONDS);
    }

    private static boolean isFree(
            Instant start,
            Instant end,
            List<OccupiedInterval> occupiedIntervals) {
        return occupiedIntervals.stream().noneMatch(
                occupied -> start.isBefore(occupied.endsAt())
                        && end.isAfter(occupied.startsAt()));
    }

    record AvailabilityWindow(
            Instant from,
            Instant toExclusive,
            int durationMinutes) {
    }

    record AvailabilitySlot(Instant startAt, Instant endAt) {
    }

    record OccupiedInterval(Instant startsAt, Instant endsAt) {
    }
}
