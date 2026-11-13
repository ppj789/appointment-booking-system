package com.github.ppj789.booking.calendar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.appointment.persistence.AppointmentEntity;
import com.github.ppj789.booking.appointment.persistence.AppointmentRepository;
import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.calendar.exception.AvailabilityBlockNotFoundException;
import com.github.ppj789.booking.calendar.exception.AvailabilityBlockNotRemovableException;
import com.github.ppj789.booking.calendar.exception.CalendarConflictException;
import com.github.ppj789.booking.calendar.persistence.AvailabilityBlockEntity;
import com.github.ppj789.booking.calendar.persistence.AvailabilityBlockRepository;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationKind;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationRepository;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal.BranchPrincipal;
import com.github.ppj789.booking.invitation.persistence.InvitationEntity;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

class StaffCalendarServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");
    private static final Instant STARTS_AT = Instant.parse("2026-11-09T10:15:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-11-09T11:00:00Z");
    private static final UUID STAFF_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID BRANCH_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID BLOCK_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final AvailabilityBlockRepository blockRepository = mock(AvailabilityBlockRepository.class);
    private final CalendarReservationRepository reservationRepository =
            mock(CalendarReservationRepository.class);
    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);

    private final StaffCalendarService service = new StaffCalendarService(
            appointmentRepository,
            blockRepository,
            reservationRepository,
            userAccountRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void readsOnlyTheAuthenticatedStaffCalendarProjection() {
        Instant from = NOW.minusSeconds(3600);
        Instant to = NOW.plusSeconds(3600);
        AppointmentEntity appointment = appointment();
        AvailabilityBlockEntity block = block(BLOCK_ID, STARTS_AT, ENDS_AT);
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(appointmentRepository.findCalendarAppointments(STAFF_ID, from, to))
                .thenReturn(List.of(appointment));
        when(blockRepository.findCalendarBlocks(STAFF_ID, from, to))
                .thenReturn(List.of(block));

        StaffCalendarService.CalendarView result = service.getCalendar(staffPrincipal(), from, to);

        assertEquals(from, result.from());
        assertEquals(to, result.toExclusive());
        assertEquals("APT-6H8KM4Q2TX", result.appointments().getFirst().reference());
        assertEquals("Account discussion", result.appointments().getFirst().title());
        assertEquals("Alex Guest", result.appointments().getFirst().guestDisplayName());
        assertEquals(BLOCK_ID, result.blocks().getFirst().id());
        verify(appointmentRepository).findCalendarAppointments(STAFF_ID, from, to);
        verify(blockRepository).findCalendarBlocks(STAFF_ID, from, to);
    }

    @Test
    void createsABranchBoundBlockForTheAuthenticatedStaffMember() {
        UserAccountEntity staff = activeStaff();
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));
        when(reservationRepository.saveAndFlush(any(CalendarReservationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(blockRepository.saveAndFlush(any(AvailabilityBlockEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StaffCalendarService.CalendarBlock result =
                service.createBlock(staffPrincipal(), STARTS_AT, ENDS_AT);

        ArgumentCaptor<CalendarReservationEntity> reservationCaptor =
                ArgumentCaptor.forClass(CalendarReservationEntity.class);
        verify(reservationRepository).saveAndFlush(reservationCaptor.capture());
        CalendarReservationEntity reservation = reservationCaptor.getValue();
        assertSame(staff, reservation.getStaffUser());
        assertEquals(BRANCH_ID, reservation.getBranch().getId());
        assertEquals(CalendarReservationKind.BLOCK, reservation.getKind());
        assertEquals(STARTS_AT, result.startAt());
        assertEquals(ENDS_AT, result.endAt());
    }

    @Test
    void translatesAnAuthoritativeReservationConflict() {
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(reservationRepository.saveAndFlush(any(CalendarReservationEntity.class)))
                .thenThrow(overlapViolation());

        CalendarConflictException exception = assertThrows(
                CalendarConflictException.class,
                () -> service.createBlock(staffPrincipal(), STARTS_AT, ENDS_AT));

        assertEquals(
                "The requested interval overlaps existing calendar occupancy",
                exception.getMessage());
        verify(blockRepository, never()).saveAndFlush(any(AvailabilityBlockEntity.class));
    }

    @Test
    void doesNotHideAnUnexpectedDatabaseIntegrityFailure() {
        DataIntegrityViolationException unexpected =
                new DataIntegrityViolationException("unexpected integrity failure");
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(reservationRepository.saveAndFlush(any(CalendarReservationEntity.class)))
                .thenThrow(unexpected);

        DataIntegrityViolationException result = assertThrows(
                DataIntegrityViolationException.class,
                () -> service.createBlock(staffPrincipal(), STARTS_AT, ENDS_AT));

        assertSame(unexpected, result);
        verify(blockRepository, never()).saveAndFlush(any(AvailabilityBlockEntity.class));
    }

    @Test
    void hidesMissingAndCrossOwnerBlockIdsBehindTheSameFailure() {
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(blockRepository.findOwnedByIdForUpdate(BLOCK_ID, STAFF_ID))
                .thenReturn(Optional.empty());

        AvailabilityBlockNotFoundException exception = assertThrows(
                AvailabilityBlockNotFoundException.class,
                () -> service.removeBlock(staffPrincipal(), BLOCK_ID));

        assertEquals("Availability Block is unavailable", exception.getMessage());
        verify(reservationRepository, never()).delete(any(CalendarReservationEntity.class));
    }

    @Test
    void removesTheSubtypeBeforeItsFutureReservation() {
        AvailabilityBlockEntity block = block(BLOCK_ID, STARTS_AT, ENDS_AT);
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(blockRepository.findOwnedByIdForUpdate(BLOCK_ID, STAFF_ID))
                .thenReturn(Optional.of(block));

        service.removeBlock(staffPrincipal(), BLOCK_ID);

        InOrder deletionOrder = inOrder(blockRepository, reservationRepository);
        deletionOrder.verify(blockRepository).delete(block);
        deletionOrder.verify(blockRepository).flush();
        deletionOrder.verify(reservationRepository).delete(block.getReservation());
    }

    @Test
    void refusesToRemoveABlockThatHasStarted() {
        AvailabilityBlockEntity block = block(BLOCK_ID, NOW, ENDS_AT);
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.of(activeStaff()));
        when(blockRepository.findOwnedByIdForUpdate(BLOCK_ID, STAFF_ID))
                .thenReturn(Optional.of(block));

        AvailabilityBlockNotRemovableException exception = assertThrows(
                AvailabilityBlockNotRemovableException.class,
                () -> service.removeBlock(staffPrincipal(), BLOCK_ID));

        assertEquals("Only a future Availability Block can be removed", exception.getMessage());
        verify(blockRepository, never()).delete(any(AvailabilityBlockEntity.class));
    }

    @Test
    void rejectsAdminCalendarAccessBeforeCallingPersistence() {
        AuthenticatedAccountPrincipal admin = new AuthenticatedAccountPrincipal(
                UUID.randomUUID(),
                "Local Admin",
                "admin@example.test",
                AccountType.ADMIN,
                AccountStatus.ACTIVE,
                null);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> service.getCalendar(admin, NOW, NOW.plusSeconds(3600)));

        assertEquals("Active Staff access is required", exception.getMessage());
        verifyNoInteractions(appointmentRepository, blockRepository);
    }

    @Test
    void rejectsAnActiveSessionAfterTheStaffAccountIsDeactivated() {
        when(userAccountRepository.findById(STAFF_ID)).thenReturn(Optional.empty());

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> service.getCalendar(
                        staffPrincipal(),
                        NOW,
                        NOW.plusSeconds(3600)));

        assertEquals("Active Staff access is required", exception.getMessage());
        verifyNoInteractions(appointmentRepository, blockRepository);
    }

    private AppointmentEntity appointment() {
        CalendarReservationEntity reservation = mock(CalendarReservationEntity.class);
        InvitationEntity invitation = mock(InvitationEntity.class);
        AppointmentEntity appointment = mock(AppointmentEntity.class);
        when(reservation.getStartsAt()).thenReturn(STARTS_AT);
        when(reservation.getEndsAt()).thenReturn(ENDS_AT);
        when(invitation.getTitle()).thenReturn("Account discussion");
        when(invitation.getGuestName()).thenReturn("Alex Guest");
        when(appointment.getId()).thenReturn(UUID.randomUUID());
        when(appointment.getPublicReference()).thenReturn("APT-6H8KM4Q2TX");
        when(appointment.getReservation()).thenReturn(reservation);
        when(appointment.getInvitation()).thenReturn(invitation);
        return appointment;
    }

    private AvailabilityBlockEntity block(UUID id, Instant startsAt, Instant endsAt) {
        CalendarReservationEntity reservation = mock(CalendarReservationEntity.class);
        AvailabilityBlockEntity block = mock(AvailabilityBlockEntity.class);
        when(reservation.getStartsAt()).thenReturn(startsAt);
        when(reservation.getEndsAt()).thenReturn(endsAt);
        when(block.getId()).thenReturn(id);
        when(block.getReservation()).thenReturn(reservation);
        return block;
    }

    private UserAccountEntity activeStaff() {
        BranchEntity branch = BranchEntity.demo(
                BRANCH_ID,
                "JHB-DEMO",
                "Johannesburg Demo Branch",
                "1 Demo Street, Johannesburg",
                "Africa/Johannesburg");
        UserAccountEntity staff = UserAccountEntity.pendingStaff(
                branch,
                "Demo Staff",
                "staff@example.test",
                NOW.minusSeconds(3600));
        staff.activate("encoded-password", NOW.minusSeconds(1800));
        return staff;
    }

    private DataIntegrityViolationException overlapViolation() {
        ConstraintViolationException hibernateViolation = new ConstraintViolationException(
                "calendar overlap",
                new SQLException(
                        "violates exclusion constraint "
                                + "\"ex_calendar_reservation_staff_time\"",
                        "23P01"),
                null);
        return new DataIntegrityViolationException("calendar overlap", hibernateViolation);
    }

    private AuthenticatedAccountPrincipal staffPrincipal() {
        return new AuthenticatedAccountPrincipal(
                STAFF_ID,
                "Demo Staff",
                "staff@example.test",
                AccountType.STAFF,
                AccountStatus.ACTIVE,
                new BranchPrincipal(
                        BRANCH_ID,
                        "JHB-DEMO",
                        "Johannesburg Demo Branch",
                        "1 Demo Street, Johannesburg",
                        "Africa/Johannesburg"));
    }
}
