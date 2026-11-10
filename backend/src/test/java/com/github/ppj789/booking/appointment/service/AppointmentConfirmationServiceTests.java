package com.github.ppj789.booking.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.ppj789.booking.appointment.exception.IdempotencyKeyReusedException;
import com.github.ppj789.booking.appointment.exception.SlotUnavailableException;
import com.github.ppj789.booking.appointment.persistence.AppointmentEntity;
import com.github.ppj789.booking.appointment.persistence.AppointmentRepository;
import com.github.ppj789.booking.appointment.service.AppointmentConfirmationService.ConfirmationResult;
import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationKind;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationRepository;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.security.SecretHasher;
import com.github.ppj789.booking.invitation.exception.InvitationUnavailableException;
import com.github.ppj789.booking.invitation.persistence.InvitationEntity;
import com.github.ppj789.booking.invitation.persistence.InvitationRepository;
import com.github.ppj789.booking.invitation.persistence.InvitationStatus;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class AppointmentConfirmationServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");
    private static final Instant STARTS_AT = Instant.parse("2026-11-10T10:15:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-11-10T11:15:00Z");
    private static final UUID BRANCH_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID STAFF_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID INVITATION_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID APPOINTMENT_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final String RAW_TOKEN = "valid-guest-capability";
    private static final String REFERENCE = "APT-6H8KM4Q2TX";

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final CalendarReservationRepository reservationRepository =
            mock(CalendarReservationRepository.class);
    private final InvitationRepository invitationRepository = mock(InvitationRepository.class);
    private final AppointmentReferenceIssuer referenceIssuer = mock(AppointmentReferenceIssuer.class);
    private final SecretHasher secretHasher = new SecretHasher();
    private final AppointmentConfirmationService service = new AppointmentConfirmationService(
            appointmentRepository,
            reservationRepository,
            invitationRepository,
            referenceIssuer,
            secretHasher,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void commitsReservationAppointmentAndInvitationConsumptionAsOneOrderedCommand() {
        InvitationEntity invitation = activeInvitation();
        stubNewCommand(invitation);
        when(referenceIssuer.issue()).thenReturn(REFERENCE);
        when(reservationRepository.saveAndFlush(any(CalendarReservationEntity.class)))
                .thenAnswer(invocation -> {
                    CalendarReservationEntity reservation = invocation.getArgument(0);
                    ReflectionTestUtils.setField(reservation, "id", APPOINTMENT_ID);
                    return reservation;
                });
        when(appointmentRepository.saveAndFlush(any(AppointmentEntity.class)))
                .thenAnswer(invocation -> {
                    AppointmentEntity appointment = invocation.getArgument(0);
                    ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
                    return appointment;
                });

        ConfirmationResult result = service.confirm(
                RAW_TOKEN,
                IDEMPOTENCY_KEY,
                STARTS_AT,
                60);

        assertEquals(APPOINTMENT_ID, result.appointmentId());
        assertEquals(REFERENCE, result.reference());
        assertEquals(STARTS_AT, result.startAt());
        assertEquals(ENDS_AT, result.endAt());
        assertEquals("JHB-DEMO", result.branch().publicCode());
        assertEquals(InvitationStatus.CONSUMED, invitation.getStatus());
        assertEquals(NOW, invitation.getConsumedAt());

        ArgumentCaptor<CalendarReservationEntity> reservationCaptor =
                ArgumentCaptor.forClass(CalendarReservationEntity.class);
        ArgumentCaptor<AppointmentEntity> appointmentCaptor =
                ArgumentCaptor.forClass(AppointmentEntity.class);
        InOrder persistenceOrder = inOrder(
                reservationRepository,
                appointmentRepository,
                invitationRepository);
        persistenceOrder.verify(reservationRepository).saveAndFlush(reservationCaptor.capture());
        persistenceOrder.verify(appointmentRepository).saveAndFlush(appointmentCaptor.capture());
        persistenceOrder.verify(invitationRepository).flush();
        assertEquals(CalendarReservationKind.APPOINTMENT, reservationCaptor.getValue().getKind());
        assertSame(invitation, appointmentCaptor.getValue().getInvitation());
        assertEquals(IDEMPOTENCY_KEY, appointmentCaptor.getValue().getIdempotencyKey());
    }

    @Test
    void replaysTheOriginalConfirmationWithoutWritingAgain() {
        AppointmentEntity appointment = committedAppointment();
        when(appointmentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(appointment));

        ConfirmationResult result = service.confirm(
                RAW_TOKEN,
                IDEMPOTENCY_KEY,
                STARTS_AT,
                60);

        assertTrue(result.replayed());
        assertEquals(REFERENCE, result.reference());
        verifyNoInteractions(reservationRepository, invitationRepository, referenceIssuer);
        verify(appointmentRepository, never()).saveAndFlush(any(AppointmentEntity.class));
    }

    @Test
    void rejectsReuseOfAnIdempotencyKeyForDifferentInput() {
        when(appointmentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(committedAppointment()));

        assertThrows(
                IdempotencyKeyReusedException.class,
                () -> service.confirm(
                        RAW_TOKEN,
                        IDEMPOTENCY_KEY,
                        STARTS_AT.plusSeconds(900),
                        60));

        verifyNoInteractions(reservationRepository, invitationRepository, referenceIssuer);
    }

    @Test
    void rechecksForACommittedRetryAfterWaitingForTheInvitationLock() {
        AppointmentEntity appointment = committedAppointment();
        when(appointmentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(appointment));
        when(invitationRepository.findByTokenHashForUpdate(secretHasher.sha256(RAW_TOKEN)))
                .thenReturn(Optional.of(appointment.getInvitation()));

        ConfirmationResult result = service.confirm(
                RAW_TOKEN,
                IDEMPOTENCY_KEY,
                STARTS_AT,
                60);

        assertTrue(result.replayed());
        assertEquals(REFERENCE, result.reference());
        verifyNoInteractions(reservationRepository, referenceIssuer);
        verify(appointmentRepository, never()).saveAndFlush(any(AppointmentEntity.class));
    }

    @Test
    void translatesTheAuthoritativeCalendarConflictAndLeavesInvitationActive() {
        InvitationEntity invitation = activeInvitation();
        stubNewCommand(invitation);
        when(reservationRepository.saveAndFlush(any(CalendarReservationEntity.class)))
                .thenThrow(overlapViolation());

        assertThrows(
                SlotUnavailableException.class,
                () -> service.confirm(RAW_TOKEN, IDEMPOTENCY_KEY, STARTS_AT, 60));

        assertEquals(InvitationStatus.ACTIVE, invitation.getStatus());
        verify(appointmentRepository, never()).saveAndFlush(any(AppointmentEntity.class));
        verify(invitationRepository, never()).flush();
    }

    @Test
    void returnsOneGenericFailureForUnknownOrNoLongerBookableCapabilities() {
        String tokenHash = secretHasher.sha256(RAW_TOKEN);
        when(appointmentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(invitationRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.empty());

        assertThrows(
                InvitationUnavailableException.class,
                () -> service.confirm(RAW_TOKEN, IDEMPOTENCY_KEY, STARTS_AT, 60));

        InvitationEntity revoked = activeInvitation();
        revoked.revoke(NOW.minusSeconds(60));
        when(invitationRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(revoked));

        assertThrows(
                InvitationUnavailableException.class,
                () -> service.confirm(RAW_TOKEN, IDEMPOTENCY_KEY, STARTS_AT, 60));
    }

    private void stubNewCommand(InvitationEntity invitation) {
        when(appointmentRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        when(invitationRepository.findByTokenHashForUpdate(secretHasher.sha256(RAW_TOKEN)))
                .thenReturn(Optional.of(invitation));
    }

    private AppointmentEntity committedAppointment() {
        InvitationEntity invitation = activeInvitation();
        CalendarReservationEntity reservation = CalendarReservationEntity.appointment(
                invitation.getBranch(),
                invitation.getStaffUser(),
                STARTS_AT,
                ENDS_AT,
                NOW);
        ReflectionTestUtils.setField(reservation, "id", APPOINTMENT_ID);
        String fingerprint = secretHasher.sha256(
                INVITATION_ID + "|" + STARTS_AT + "|60");
        AppointmentEntity appointment = AppointmentEntity.create(
                reservation,
                invitation,
                REFERENCE,
                IDEMPOTENCY_KEY,
                fingerprint,
                NOW);
        ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
        invitation.consume(NOW);
        return appointment;
    }

    private InvitationEntity activeInvitation() {
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
        ReflectionTestUtils.setField(staff, "id", STAFF_ID);
        staff.activate("encoded-password", NOW.minusSeconds(1800));
        InvitationEntity invitation = InvitationEntity.draft(
                branch,
                staff,
                "Account discussion",
                "Review account options.",
                "Alex Guest",
                "alex@example.test",
                (short) 60,
                NOW.minusSeconds(600));
        ReflectionTestUtils.setField(invitation, "id", INVITATION_ID);
        invitation.activate(secretHasher.sha256(RAW_TOKEN), NOW.minusSeconds(300));
        return invitation;
    }

    private DataIntegrityViolationException overlapViolation() {
        ConstraintViolationException hibernateViolation = new ConstraintViolationException(
                "calendar overlap",
                new SQLException("calendar overlap"),
                "ex_calendar_reservation_staff_time");
        return new DataIntegrityViolationException("calendar overlap", hibernateViolation);
    }
}
