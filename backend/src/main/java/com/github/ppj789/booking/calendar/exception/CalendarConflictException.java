package com.github.ppj789.booking.calendar.exception;

public class CalendarConflictException extends RuntimeException {

    public CalendarConflictException() {
        super("The requested interval overlaps existing calendar occupancy");
    }
}
