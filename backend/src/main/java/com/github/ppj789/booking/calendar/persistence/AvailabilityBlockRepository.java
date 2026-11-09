package com.github.ppj789.booking.calendar.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityBlockRepository extends JpaRepository<AvailabilityBlockEntity, UUID> {

    @EntityGraph(attributePaths = "reservation")
    @Query("""
            select block
            from AvailabilityBlockEntity block
            where block.reservation.staffUser.id = :staffId
              and block.reservation.startsAt < :to
              and block.reservation.endsAt > :from
            order by block.reservation.startsAt
            """)
    List<AvailabilityBlockEntity> findCalendarBlocks(
            @Param("staffId") UUID staffId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "reservation")
    @Query("""
            select block
            from AvailabilityBlockEntity block
            where block.id = :blockId
              and block.reservation.staffUser.id = :staffId
            """)
    Optional<AvailabilityBlockEntity> findOwnedByIdForUpdate(
            @Param("blockId") UUID blockId,
            @Param("staffId") UUID staffId);
}
