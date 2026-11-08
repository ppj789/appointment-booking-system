package com.github.ppj789.booking.invitation.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    boolean existsByStaffUserIdAndStatus(UUID staffId, InvitationStatus status);
}
