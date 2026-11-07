package com.github.ppj789.booking.branch.infrastructure;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.branch.persistence.BranchRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@RequiredArgsConstructor
class LocalDemoBranchDataInitializer implements ApplicationRunner {

    static final String JOHANNESBURG_DEMO_CODE = "JHB-DEMO";
    static final String CAPE_TOWN_DEMO_CODE = "CPT-DEMO";

    private static final String TIMEZONE = "Africa/Johannesburg";

    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upsertBranch(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                JOHANNESBURG_DEMO_CODE,
                "Johannesburg Demo Branch",
                "1 Demo Street, Johannesburg");
        upsertBranch(
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                CAPE_TOWN_DEMO_CODE,
                "Cape Town Demo Branch",
                "2 Example Avenue, Cape Town");
    }

    private void upsertBranch(UUID id, String publicCode, String name, String displayAddress) {
        branchRepository.findByPublicCode(publicCode).ifPresentOrElse(
                branch -> branch.refreshReferenceData(name, displayAddress, TIMEZONE),
                () -> branchRepository.save(
                        BranchEntity.demo(id, publicCode, name, displayAddress, TIMEZONE)));
    }
}
