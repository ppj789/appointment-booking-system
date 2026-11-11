package com.github.ppj789.booking.invitation.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    boolean existsByStaffUserIdAndStatus(UUID staffId, InvitationStatus status);

    List<InvitationEntity> findAllByStaffUserIdOrderByUpdatedAtDesc(UUID staffId);

    @EntityGraph(attributePaths = {"branch", "staffUser"})
    Optional<InvitationEntity> findByTokenHashAndStatus(
            String tokenHash,
            InvitationStatus status);

    @EntityGraph(attributePaths = {"branch", "staffUser"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from InvitationEntity invitation where invitation.tokenHash = :tokenHash")
    Optional<InvitationEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

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
