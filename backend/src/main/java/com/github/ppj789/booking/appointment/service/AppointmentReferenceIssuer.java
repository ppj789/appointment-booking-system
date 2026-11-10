package com.github.ppj789.booking.appointment.service;

import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AppointmentReferenceIssuer {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int RANDOM_CHARACTERS = 10;

    private final SecureRandom secureRandom;

    String issue() {
        StringBuilder reference = new StringBuilder("APT-");
        for (int index = 0; index < RANDOM_CHARACTERS; index++) {
            reference.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return reference.toString();
    }
}
