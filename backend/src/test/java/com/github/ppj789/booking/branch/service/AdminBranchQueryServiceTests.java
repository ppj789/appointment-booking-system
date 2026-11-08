package com.github.ppj789.booking.branch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.branch.persistence.BranchRepository;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AdminBranchQueryServiceTests {

    @Test
    void returnsOnlyTheRepositorysActiveBranchChoices() {
        BranchRepository repository = mock(BranchRepository.class);
        BranchEntity branch = BranchEntity.demo(
                UUID.randomUUID(),
                "JHB-DEMO",
                "Johannesburg Demo Branch",
                "1 Demo Street, Johannesburg",
                "Africa/Johannesburg");
        when(repository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(branch));
        AdminBranchQueryService service = new AdminBranchQueryService(repository);

        List<AdminBranchQueryService.BranchView> result = service.listActive(adminPrincipal());

        assertEquals(1, result.size());
        assertEquals("JHB-DEMO", result.getFirst().publicCode());
        assertEquals("Africa/Johannesburg", result.getFirst().timezone());
    }

    @Test
    void rejectsNonAdminBranchListing() {
        BranchRepository repository = mock(BranchRepository.class);
        AdminBranchQueryService service = new AdminBranchQueryService(repository);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> service.listActive(staffPrincipal()));

        assertEquals("Active Admin access is required", exception.getMessage());
        verifyNoInteractions(repository);
    }

    private AuthenticatedAccountPrincipal adminPrincipal() {
        return principal(AccountType.ADMIN);
    }

    private AuthenticatedAccountPrincipal staffPrincipal() {
        return principal(AccountType.STAFF);
    }

    private AuthenticatedAccountPrincipal principal(AccountType accountType) {
        return new AuthenticatedAccountPrincipal(
                UUID.randomUUID(),
                "Test Account",
                "account@example.test",
                accountType,
                AccountStatus.ACTIVE,
                null);
    }
}
