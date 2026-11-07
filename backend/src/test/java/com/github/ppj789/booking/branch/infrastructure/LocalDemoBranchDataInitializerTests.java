package com.github.ppj789.booking.branch.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.branch.persistence.BranchRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

class LocalDemoBranchDataInitializerTests {

    @Test
    void upsertsBothSyntheticBranches() throws Exception {
        BranchRepository branchRepository = mock(BranchRepository.class);
        when(branchRepository.findByPublicCode(anyString())).thenReturn(Optional.empty());
        LocalDemoBranchDataInitializer initializer = new LocalDemoBranchDataInitializer(branchRepository);

        initializer.run(mock(ApplicationArguments.class));

        verify(branchRepository, times(2)).save(any(BranchEntity.class));
    }
}
