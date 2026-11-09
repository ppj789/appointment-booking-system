package com.github.ppj789.booking.invitation.service;

import com.github.ppj789.booking.identity.security.SecretHasher;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class InvitationTokenIssuer {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;
    private final SecretHasher secretHasher;

    IssuedToken issue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(rawToken, secretHasher.sha256(rawToken));
    }

    static final class IssuedToken {

        private final String rawToken;
        private final String tokenHash;

        IssuedToken(String rawToken, String tokenHash) {
            this.rawToken = rawToken;
            this.tokenHash = tokenHash;
        }

        String rawToken() {
            return rawToken;
        }

        String tokenHash() {
            return tokenHash;
        }

        @Override
        public String toString() {
            return "IssuedToken[redacted]";
        }
    }
}
