package com.github.ppj789.booking.identity.security;

import com.github.ppj789.booking.identity.persistence.AccountStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AccountUserDetails(AuthenticatedAccountPrincipal account, String passwordHash) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return account.email();
    }

    @Override
    public boolean isEnabled() {
        return account.status() == AccountStatus.ACTIVE;
    }
}
