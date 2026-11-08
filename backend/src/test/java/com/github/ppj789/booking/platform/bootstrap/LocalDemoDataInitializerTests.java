package com.github.ppj789.booking.platform.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.branch.persistence.BranchRepository;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class LocalDemoDataInitializerTests {

    @Test
    void seedsBothSyntheticBranchesWhenTheyDoNotExist() throws Exception {
        BranchRepository branchRepository = mock(BranchRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        when(branchRepository.findByPublicCode(anyString())).thenReturn(Optional.empty());
        when(userAccountRepository.existsByEmail(anyString())).thenReturn(true);
        LocalDemoDataInitializer initializer = new LocalDemoDataInitializer(
                branchRepository,
                userAccountRepository,
                mock(PasswordEncoder.class),
                Clock.systemUTC());

        initializer.run(mock(ApplicationArguments.class));

        verify(branchRepository, times(2)).save(any(BranchEntity.class));
    }

    @Test
    void seedsClearlySyntheticAdminOnlyWhenMissing() throws Exception {
        BranchRepository branchRepository = mock(BranchRepository.class);
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(branchRepository.findByPublicCode(anyString())).thenReturn(Optional.empty());
        when(userAccountRepository.existsByEmail("admin@example.test")).thenReturn(false);
        when(passwordEncoder.encode("admin")).thenReturn("encoded-admin-password");
        LocalDemoDataInitializer initializer = new LocalDemoDataInitializer(
                branchRepository,
                userAccountRepository,
                passwordEncoder,
                Clock.fixed(Instant.parse("2026-11-08T08:00:00Z"), ZoneOffset.UTC));

        initializer.run(mock(ApplicationArguments.class));

        ArgumentCaptor<UserAccountEntity> accountCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(accountCaptor.capture());
        UserAccountEntity admin = accountCaptor.getValue();
        assertEquals(AccountType.ADMIN, admin.getAccountType());
        assertEquals("admin@example.test", admin.getEmail());
        assertEquals("encoded-admin-password", admin.getPasswordHash());
        assertNull(admin.getBranch());
    }
}
