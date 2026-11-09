package com.github.ppj789.booking.invitation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.github.ppj789.booking.identity.security.SecretHasher;
import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class InvitationTokenIssuerTests {

    @Test
    void issuesAHighEntropyUrlSafeTokenAndOnlyItsHashForPersistence() {
        SecureRandom secureRandom = mock(SecureRandom.class);
        doAnswer(invocation -> {
            byte[] target = invocation.getArgument(0);
            Arrays.fill(target, (byte) 7);
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));
        SecretHasher hasher = new SecretHasher();
        InvitationTokenIssuer issuer = new InvitationTokenIssuer(secureRandom, hasher);

        InvitationTokenIssuer.IssuedToken token = issuer.issue();

        assertEquals(43, token.rawToken().length());
        assertTrue(token.rawToken().matches("[A-Za-z0-9_-]+"));
        assertEquals(hasher.sha256(token.rawToken()), token.tokenHash());
        assertEquals("IssuedToken[redacted]", token.toString());
        assertFalse(token.toString().contains(token.rawToken()));
    }
}
