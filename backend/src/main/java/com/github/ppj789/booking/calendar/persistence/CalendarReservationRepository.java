package com.github.ppj789.booking.calendar.persistence;

import java.util.UUID;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarReservationRepository
        extends JpaRepository<CalendarReservationEntity, UUID> {

    @Query("""
            select reservation
            from CalendarReservationEntity reservation
            where reservation.staffUser.id = :staffId
              and reservation.startsAt < :toExclusive
              and reservation.endsAt > :from
            order by reservation.startsAt
            """)
    List<CalendarReservationEntity> findOverlappingWindow(
            @Param("staffId") UUID staffId,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);
}
