package com.github.ppj789.booking.invitation.controller;

import org.springframework.stereotype.Component;

@Component
class InvitationShareLinkFactory {

    String create(String rawToken) {
        return "/book#token=" + rawToken;
    }
}
