package com.github.ppj789.booking.calendar.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.ppj789.booking.calendar.service.StaffCalendarService;
import com.github.ppj789.booking.calendar.service.StaffCalendarService.CalendarBlock;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.platform.web.GlobalProblemHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StaffCalendarControllerTests {

    @Test
    void creationReturnsTheCommittedBlockAndCreatedStatus() {
        StaffCalendarService service = mock(StaffCalendarService.class);
        AuthenticatedAccountPrincipal actor = mock(AuthenticatedAccountPrincipal.class);
        Instant start = Instant.parse("2026-11-09T10:15:00Z");
        Instant end = Instant.parse("2026-11-09T11:00:00Z");
        UUID blockId = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
        when(service.createBlock(actor, start, end))
                .thenReturn(new CalendarBlock(blockId, start, end));
        StaffCalendarController controller = new StaffCalendarController(service);

        ResponseEntity<AvailabilityBlockResponse> response = controller.createBlock(
                actor,
                new CreateAvailabilityBlockRequest(start, end));

        assertEquals(201, response.getStatusCode().value());
        assertEquals(blockId, response.getBody().id());
        assertEquals(start, response.getBody().startAt());
        assertEquals(end, response.getBody().endAt());
    }

    @Test
    void invalidJsonFieldsUseTheRfc9457ValidationContract() throws Exception {
        StaffCalendarService service = mock(StaffCalendarService.class);
        StaffCalendarController controller = new StaffCalendarController(service);
        GlobalProblemHandler problemHandler = new GlobalProblemHandler(
                Clock.fixed(
                        Instant.parse("2026-11-09T10:00:00Z"),
                        ZoneOffset.UTC));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(problemHandler)
                .build();

        mockMvc.perform(post("/api/v1/me/availability-blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": null,
                                  "endAt": null
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.status").value(422));
    }
}
