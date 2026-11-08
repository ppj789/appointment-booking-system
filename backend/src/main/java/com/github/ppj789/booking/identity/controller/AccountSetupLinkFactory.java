package com.github.ppj789.booking.identity.controller;

import org.springframework.stereotype.Component;

@Component
public class AccountSetupLinkFactory {

    public String create(String rawToken) {
        return "/account-setup#token=" + rawToken;
    }
}
