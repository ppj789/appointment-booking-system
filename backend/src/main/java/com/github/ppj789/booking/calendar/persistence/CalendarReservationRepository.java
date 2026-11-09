package com.github.ppj789.booking.calendar.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarReservationRepository
        extends JpaRepository<CalendarReservationEntity, UUID> {
}
