package com.github.ppj789.booking.invitation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.ppj789.booking.invitation.exception.InvalidInvitationInputException;
import com.github.ppj789.booking.invitation.service.InvitationInputPolicy.InvitationDetails;
import org.junit.jupiter.api.Test;

class InvitationInputPolicyTests {

    @Test
    void normalizesStaffAuthoredInvitationDetails() {
        InvitationDetails details = InvitationInputPolicy.normalize(
                "  Account discussion  ",
                "  Review the available options.  ",
                "  Alex Guest  ",
                "  Alex.Guest@Example.TEST  ",
                60);

        assertEquals("Account discussion", details.title());
        assertEquals("Review the available options.", details.description());
        assertEquals("Alex Guest", details.guestDisplayName());
        assertEquals("alex.guest@example.test", details.guestEmail());
        assertEquals(60, details.defaultDurationMinutes());
    }

    @Test
    void rejectsUnsupportedDurations() {
        InvalidInvitationInputException exception = assertThrows(
                InvalidInvitationInputException.class,
                () -> InvitationInputPolicy.normalize(
                        "Account discussion",
                        "Review the available options.",
                        "Alex Guest",
                        "alex@example.test",
                        90));

        assertEquals("Default duration is unsupported", exception.getMessage());
    }

    @Test
    void rejectsInvalidGuestEmailAddresses() {
        InvalidInvitationInputException exception = assertThrows(
                InvalidInvitationInputException.class,
                () -> InvitationInputPolicy.normalize(
                        "Account discussion",
                        "Review the available options.",
                        "Alex Guest",
                        "not-an-email",
                        30));

        assertEquals("Guest email address is invalid", exception.getMessage());
    }
}
