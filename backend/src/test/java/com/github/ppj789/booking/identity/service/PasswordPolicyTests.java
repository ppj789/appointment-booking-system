package com.github.ppj789.booking.identity.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.ppj789.booking.identity.exception.InvalidPasswordException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTests {

    @Test
    void acceptsPasswordsAtSupportedBoundaries() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MIN_LENGTH)));
        assertDoesNotThrow(() -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MAX_LENGTH)));
    }

    @Test
    void rejectsMissingShortAndLongPasswords() {
        InvalidPasswordException missingPassword =
                assertThrows(InvalidPasswordException.class, () -> PasswordPolicy.validate(null));
        InvalidPasswordException shortPassword = assertThrows(
                InvalidPasswordException.class,
                () -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MIN_LENGTH - 1)));
        InvalidPasswordException longPassword = assertThrows(
                InvalidPasswordException.class,
                () -> PasswordPolicy.validate("a".repeat(PasswordPolicy.MAX_LENGTH + 1)));

        String expectedMessage = "Password must contain between "
                + PasswordPolicy.MIN_LENGTH
                + " and "
                + PasswordPolicy.MAX_LENGTH
                + " characters";
        assertEquals(expectedMessage, missingPassword.getMessage());
        assertEquals(expectedMessage, shortPassword.getMessage());
        assertEquals(expectedMessage, longPassword.getMessage());
    }
}
