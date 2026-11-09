package com.github.ppj789.booking.calendar.controller;

import com.github.ppj789.booking.calendar.service.StaffCalendarService;
import com.github.ppj789.booking.calendar.service.StaffCalendarService.CalendarBlock;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Staff calendar", description = "Private Staff calendar and availability management")
@SecurityRequirement(name = "sessionCookie")
@RequiredArgsConstructor
public class StaffCalendarController {

    private final StaffCalendarService calendarService;

    @GetMapping("/calendar")
    @Operation(summary = "Read the Staff calendar")
    @ApiResponse(responseCode = "200", description = "Owned calendar entries returned")
    @ApiResponse(responseCode = "403", description = "Active Staff access is required")
    @ApiResponse(responseCode = "422", description = "Calendar window is invalid")
    public StaffCalendarResponse getCalendar(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Inclusive UTC query start")
            Instant from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Exclusive UTC query end; maximum 31-day window")
            Instant to) {
        return StaffCalendarResponse.from(calendarService.getCalendar(actor, from, to));
    }

    @PostMapping("/availability-blocks")
    @Operation(summary = "Create a future Availability Block")
    @ApiResponse(responseCode = "201", description = "Availability Block created")
    @ApiResponse(responseCode = "403", description = "Active Staff access is required")
    @ApiResponse(responseCode = "409", description = "The interval overlaps existing occupancy")
    @ApiResponse(responseCode = "422", description = "The interval is invalid")
    public ResponseEntity<AvailabilityBlockResponse> createBlock(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @Valid @RequestBody CreateAvailabilityBlockRequest request) {
        CalendarBlock block = calendarService.createBlock(
                actor,
                request.startAt(),
                request.endAt());
        return ResponseEntity.status(201).body(AvailabilityBlockResponse.from(block));
    }

    @DeleteMapping("/availability-blocks/{blockId}")
    @Operation(summary = "Remove an owned future Availability Block")
    @ApiResponse(responseCode = "204", description = "Availability Block removed")
    @ApiResponse(responseCode = "403", description = "Active Staff access is required")
    @ApiResponse(responseCode = "404", description = "Availability Block is unavailable")
    @ApiResponse(responseCode = "409", description = "Availability Block has already started")
    public ResponseEntity<Void> removeBlock(
            @AuthenticationPrincipal AuthenticatedAccountPrincipal actor,
            @PathVariable UUID blockId) {
        calendarService.removeBlock(actor, blockId);
        return ResponseEntity.noContent().build();
    }
}
