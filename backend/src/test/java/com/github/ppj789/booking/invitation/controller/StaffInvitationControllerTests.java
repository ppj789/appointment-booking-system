package com.github.ppj789.booking.invitation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import com.github.ppj789.booking.invitation.service.StaffInvitationService;
import com.github.ppj789.booking.invitation.service.StaffInvitationService.InvitationActivation;
import com.github.ppj789.booking.platform.web.GlobalProblemHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StaffInvitationControllerTests {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");
    private static final UUID INVITATION_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    @Test
    void activationReturnsTheFragmentShareUrlOnceWithoutTokenFields() throws Exception {
        StaffInvitationService service = mock(StaffInvitationService.class);
        InvitationActivation activation = mock(InvitationActivation.class);
        when(activation.id()).thenReturn(INVITATION_ID);
        when(activation.status()).thenReturn(InvitationStatus.ACTIVE);
        when(activation.activatedAt()).thenReturn(NOW);
        when(activation.rawToken()).thenReturn("one-time-token");
        when(service.activate(any(AuthenticatedAccountPrincipal.class), eq(INVITATION_ID)))
                .thenReturn(activation);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/api/v1/me/invitations/{invitationId}/activate", INVITATION_ID)
                        .principal(() -> "staff"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.shareUrl").value("/book#token=one-time-token"))
                .andExpect(jsonPath("$.rawToken").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist());
    }

    @Test
    void invalidDraftFieldsUseTheRfc9457ValidationContract() throws Exception {
        StaffInvitationService service = mock(StaffInvitationService.class);
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(post("/api/v1/me/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "",
                                  "guest": {
                                    "displayName": "",
                                    "email": "not-an-email"
                                  },
                                  "defaultDurationMinutes": null
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(422));
    }

    private MockMvc mockMvc(StaffInvitationService service) {
        StaffInvitationController controller = new StaffInvitationController(
                service,
                new InvitationShareLinkFactory());
        GlobalProblemHandler problemHandler = new GlobalProblemHandler(
                Clock.fixed(NOW, ZoneOffset.UTC));
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(problemHandler)
                .setCustomArgumentResolvers(new StubPrincipalArgumentResolver())
                .build();
    }
}
