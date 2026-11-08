package com.github.ppj789.booking.platform.bootstrap;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.branch.persistence.BranchRepository;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@Order(0)
@RequiredArgsConstructor
class LocalDemoDataInitializer implements ApplicationRunner {

    static final String JOHANNESBURG_DEMO_BRANCH_CODE = "JHB-DEMO";
    static final String CAPE_TOWN_DEMO_BRANCH_CODE = "CPT-DEMO";

    private static final String TIMEZONE = "Africa/Johannesburg";
    private static final String DEMO_ADMIN_DISPLAY_NAME = "Local Admin";
    private static final String DEMO_ADMIN_EMAIL = "admin@example.test";
    private static final String DEMO_ADMIN_PASSWORD = "admin";

    private final BranchRepository branchRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedBranches();
        seedAdmin();
    }

    private void seedAdmin() {
        if (userAccountRepository.existsByEmail(DEMO_ADMIN_EMAIL)) {
            return;
        }
        userAccountRepository.save(UserAccountEntity.activeAdmin(
                DEMO_ADMIN_DISPLAY_NAME,
                DEMO_ADMIN_EMAIL,
                passwordEncoder.encode(DEMO_ADMIN_PASSWORD),
                clock.instant()));
    }

    private void seedBranches() {
        upsertBranch(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                JOHANNESBURG_DEMO_BRANCH_CODE,
                "Johannesburg Demo Branch",
                "1 Demo Street, Johannesburg");
        upsertBranch(
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                CAPE_TOWN_DEMO_BRANCH_CODE,
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
