package com.github.ppj789.booking.identity.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CredentialSetupRepository extends JpaRepository<CredentialSetupEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"userAccount", "userAccount.branch"})
    Optional<CredentialSetupEntity> findByTokenHash(String tokenHash);
}
