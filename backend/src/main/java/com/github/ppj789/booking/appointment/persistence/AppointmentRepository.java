package com.github.ppj789.booking.appointment.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    boolean existsByStaffUserIdAndReservationStartsAtAfter(UUID staffId, Instant now);
}
