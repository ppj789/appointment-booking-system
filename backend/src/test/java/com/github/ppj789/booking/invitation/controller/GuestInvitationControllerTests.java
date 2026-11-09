package com.github.ppj789.booking.invitation.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.ppj789.booking.invitation.exception.InvitationUnavailableException;
import com.github.ppj789.booking.invitation.service.GuestInvitationService;
import com.github.ppj789.booking.invitation.service.GuestInvitationService.GuestBranch;
import com.github.ppj789.booking.invitation.service.GuestInvitationService.GuestInvitation;
import com.github.ppj789.booking.platform.web.GlobalProblemHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GuestInvitationControllerTests {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    @Test
    void invitationResponseAllowsOnlyGuestSafeContextAndDisablesCaching() throws Exception {
        GuestInvitationService service = mock(GuestInvitationService.class);
        when(service.getInvitation(anyString(), anyString())).thenReturn(new GuestInvitation(
                "Account discussion",
                "Review account options.",
                "Demo Staff",
                "Alex Guest",
                (short) 60,
                List.of(15, 30, 45, 60, 120, 240),
                new GuestBranch(
                        "JHB-DEMO",
                        "Johannesburg Demo Branch",
                        "1 Demo Street, Johannesburg",
                        "Africa/Johannesburg")));

        mockMvc(service).perform(get("/api/v1/guest/invitation")
                        .header(GuestInvitationController.INVITATION_TOKEN_HEADER, "guest-token")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.hostDisplayName").value("Demo Staff"))
                .andExpect(jsonPath("$.branch.publicCode").value("JHB-DEMO"))
                .andExpect(jsonPath("$.branch.timezone").value("Africa/Johannesburg"))
                .andExpect(jsonPath("$.guestEmail").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void unavailableCapabilitiesUseOneGenericRfc9457Response() throws Exception {
        GuestInvitationService service = mock(GuestInvitationService.class);
        when(service.getInvitation(anyString(), anyString()))
                .thenThrow(new InvitationUnavailableException());

        mockMvc(service).perform(get("/api/v1/guest/invitation")
                        .header(GuestInvitationController.INVITATION_TOKEN_HEADER, "unknown-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVITATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("The Invitation cannot be used."));
    }

    private MockMvc mockMvc(GuestInvitationService service) {
        GuestInvitationController controller = new GuestInvitationController(service);
        GlobalProblemHandler problemHandler = new GlobalProblemHandler(
                Clock.fixed(NOW, ZoneOffset.UTC));
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(problemHandler)
                .build();
    }
}
