package com.github.ppj789.booking.appointment.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    boolean existsByStaffUserIdAndReservationStartsAtAfter(UUID staffId, Instant now);

    @EntityGraph(attributePaths = {"reservation", "invitation", "branch", "staffUser"})
    java.util.Optional<AppointmentEntity> findByIdempotencyKey(UUID idempotencyKey);

    @EntityGraph(attributePaths = {"reservation", "invitation"})
    @Query("""
            select appointment
            from AppointmentEntity appointment
            where appointment.staffUser.id = :staffId
              and appointment.reservation.startsAt < :to
              and appointment.reservation.endsAt > :from
            order by appointment.reservation.startsAt
            """)
    List<AppointmentEntity> findCalendarAppointments(
            @Param("staffId") UUID staffId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
