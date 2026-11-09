package com.github.ppj789.booking.invitation.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    boolean existsByStaffUserIdAndStatus(UUID staffId, InvitationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from InvitationEntity invitation
            where invitation.id = :invitationId
              and invitation.staffUser.id = :staffId
            """)
    Optional<InvitationEntity> findOwnedByIdForUpdate(
            @Param("invitationId") UUID invitationId,
            @Param("staffId") UUID staffId);
}
