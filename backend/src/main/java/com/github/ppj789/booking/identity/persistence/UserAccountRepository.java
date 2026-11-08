package com.github.ppj789.booking.identity.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    @EntityGraph(attributePaths = "branch")
    Optional<UserAccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "branch")
    List<UserAccountEntity> findAllByAccountTypeOrderByDisplayNameAsc(AccountType accountType);

    @EntityGraph(attributePaths = "branch")
    List<UserAccountEntity> findAllByAccountTypeAndBranchPublicCodeOrderByDisplayNameAsc(
            AccountType accountType,
            String branchCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "branch")
    @Query("select account from UserAccountEntity account where account.id = :accountId")
    Optional<UserAccountEntity> findByIdForUpdate(@Param("accountId") UUID accountId);
}
