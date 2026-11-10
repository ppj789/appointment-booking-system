package com.github.ppj789.booking.appointment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService;
import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService.ConfirmationBranch;
import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService.ConfirmationResult;
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

class GuestAppointmentControllerTests {

    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("55555555-5555-4555-8555-555555555555");

    @Test
    void returnsCreatedOnlyForANewCommittedAppointment() throws Exception {
        AppointmentConfirmationService service = mock(AppointmentConfirmationService.class);
        when(service.confirm(anyString(), any(UUID.class), any(Instant.class), anyInt()))
                .thenReturn(result(false));

        mockMvc(service).perform(post("/api/v1/guest/appointments")
                        .header(GuestAppointmentController.INVITATION_TOKEN_HEADER, "guest-token")
                        .header(GuestAppointmentController.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-11-10T10:15:00Z",
                                  "durationMinutes": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().doesNotExist(
                        GuestAppointmentController.IDEMPOTENCY_REPLAYED_HEADER))
                .andExpect(jsonPath("$.appointment.reference").value("APT-6H8KM4Q2TX"))
                .andExpect(jsonPath("$.appointment.branch.publicCode").value("JHB-DEMO"))
                .andExpect(jsonPath("$.appointment.branch.timezone")
                        .value("Africa/Johannesburg"))
                .andExpect(jsonPath("$.confirmation.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmation.reference").value("APT-6H8KM4Q2TX"))
                .andExpect(jsonPath("$.tokenHash").doesNotExist())
                .andExpect(jsonPath("$.idempotencyKey").doesNotExist());
    }

    @Test
    void returnsOkAndReplayHeaderForTheOriginalCommittedResult() throws Exception {
        AppointmentConfirmationService service = mock(AppointmentConfirmationService.class);
        when(service.confirm(anyString(), any(UUID.class), any(Instant.class), anyInt()))
                .thenReturn(result(true));

        mockMvc(service).perform(post("/api/v1/guest/appointments")
                        .header(GuestAppointmentController.INVITATION_TOKEN_HEADER, "guest-token")
                        .header(GuestAppointmentController.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-11-10T10:15:00Z",
                                  "durationMinutes": 60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        GuestAppointmentController.IDEMPOTENCY_REPLAYED_HEADER,
                        "true"))
                .andExpect(jsonPath("$.confirmation.status").value("CONFIRMED"));
    }

    private MockMvc mockMvc(AppointmentConfirmationService service) {
        GuestAppointmentController controller = new GuestAppointmentController(service);
        GlobalProblemHandler problemHandler = new GlobalProblemHandler(
                Clock.fixed(NOW, ZoneOffset.UTC));
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(problemHandler)
                .build();
    }

    private ConfirmationResult result(boolean replayed) {
        return new ConfirmationResult(
                UUID.fromString("44444444-4444-4444-8444-444444444444"),
                "APT-6H8KM4Q2TX",
                "Account discussion",
                "Review account options.",
                "Demo Staff",
                "Alex Guest",
                "alex@example.test",
                Instant.parse("2026-11-10T10:15:00Z"),
                Instant.parse("2026-11-10T11:15:00Z"),
                NOW,
                new ConfirmationBranch(
                        "JHB-DEMO",
                        "Johannesburg Demo Branch",
                        "1 Demo Street, Johannesburg",
                        "Africa/Johannesburg"),
                replayed);
    }
}
