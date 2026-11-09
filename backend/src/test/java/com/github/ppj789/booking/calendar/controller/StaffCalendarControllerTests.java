package com.github.ppj789.booking.calendar.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.calendar.service.StaffCalendarService;
import com.github.ppj789.booking.calendar.service.StaffCalendarService.CalendarBlock;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

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
}
