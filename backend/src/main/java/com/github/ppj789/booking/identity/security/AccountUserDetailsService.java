package com.github.ppj789.booking.identity.security;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import com.github.ppj789.booking.identity.service.AccountInputNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AccountUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        String email;
        try {
            email = AccountInputNormalizer.email(username);
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException("Account not found", exception);
        }

        UserAccountEntity account = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        BranchEntity branch = account.getBranch();
        AuthenticatedAccountPrincipal principal = new AuthenticatedAccountPrincipal(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.getAccountType(),
                account.getAccountStatus(),
                toBranchPrincipal(branch));
        return new AccountUserDetails(principal, account.getPasswordHash());
    }

    private AuthenticatedAccountPrincipal.BranchPrincipal toBranchPrincipal(BranchEntity branch) {
        if (branch == null) {
            return null;
        }
        return new AuthenticatedAccountPrincipal.BranchPrincipal(
                branch.getId(),
                branch.getPublicCode(),
                branch.getName(),
                branch.getDisplayAddress(),
                branch.getTimezone());
    }
}
