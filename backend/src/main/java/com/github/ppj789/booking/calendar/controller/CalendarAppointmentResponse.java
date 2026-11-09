package com.github.ppj789.booking.calendar.controller;

import com.github.ppj789.booking.calendar.service.StaffCalendarService.CalendarAppointment;
import java.time.Instant;
import java.util.UUID;

public record CalendarAppointmentResponse(
        UUID id,
        String reference,
        String title,
        String guestDisplayName,
        Instant startAt,
        Instant endAt) {

    public static CalendarAppointmentResponse from(CalendarAppointment appointment) {
        return new CalendarAppointmentResponse(
                appointment.id(),
                appointment.reference(),
                appointment.title(),
                appointment.guestDisplayName(),
                appointment.startAt(),
                appointment.endAt());
    }
}
