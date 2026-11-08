package com.github.ppj789.booking.branch.service;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.branch.persistence.BranchRepository;
import com.github.ppj789.booking.identity.security.AccountAccessPolicy;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBranchQueryService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchView> listActive(AuthenticatedAccountPrincipal actor) {
        AccountAccessPolicy.requireActiveAdmin(actor);
        return branchRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(branch -> BranchView.from(branch))
                .toList();
    }

    public record BranchView(
            String publicCode,
            String name,
            String displayAddress,
            String timezone) {

        private static BranchView from(BranchEntity branch) {
            return new BranchView(
                    branch.getPublicCode(),
                    branch.getName(),
                    branch.getDisplayAddress(),
                    branch.getTimezone());
        }
    }
}
