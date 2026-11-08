package com.github.ppj789.booking.identity.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    @EntityGraph(attributePaths = "branch")
    Optional<UserAccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
