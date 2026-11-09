package com.github.ppj789.booking.calendar.controller;

import com.github.ppj789.booking.calendar.service.StaffCalendarService.CalendarView;
import java.time.Instant;
import java.util.List;

public record StaffCalendarResponse(
        Instant from,
        Instant toExclusive,
        List<CalendarAppointmentResponse> appointments,
        List<AvailabilityBlockResponse> blocks) {

    public static StaffCalendarResponse from(CalendarView calendar) {
        return new StaffCalendarResponse(
                calendar.from(),
                calendar.toExclusive(),
                calendar.appointments().stream()
                        .map(CalendarAppointmentResponse::from)
                        .toList(),
                calendar.blocks().stream()
                        .map(AvailabilityBlockResponse::from)
                        .toList());
    }
}
