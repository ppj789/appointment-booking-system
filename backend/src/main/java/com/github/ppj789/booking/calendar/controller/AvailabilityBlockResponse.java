package com.github.ppj789.booking.calendar.controller;

import com.github.ppj789.booking.calendar.service.StaffCalendarService.CalendarBlock;
import java.time.Instant;
import java.util.UUID;

public record AvailabilityBlockResponse(UUID id, Instant startAt, Instant endAt) {

    public static AvailabilityBlockResponse from(CalendarBlock block) {
        return new AvailabilityBlockResponse(block.id(), block.startAt(), block.endAt());
    }
}
