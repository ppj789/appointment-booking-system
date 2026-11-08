package com.github.ppj789.booking.branch.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {

    Optional<BranchEntity> findByPublicCode(String publicCode);

    Optional<BranchEntity> findByPublicCodeAndActiveTrue(String publicCode);

    List<BranchEntity> findAllByActiveTrueOrderByNameAsc();

    boolean existsByPublicCodeIn(Collection<String> publicCodes);
}
