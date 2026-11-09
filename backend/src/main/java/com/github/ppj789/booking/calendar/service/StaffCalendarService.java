package com.github.ppj789.booking.calendar.service;

import static com.github.ppj789.booking.identity.security.AccountAccessPolicy.requireActiveStaff;

import com.github.ppj789.booking.appointment.persistence.AppointmentEntity;
import com.github.ppj789.booking.appointment.persistence.AppointmentRepository;
import com.github.ppj789.booking.calendar.exception.AvailabilityBlockNotFoundException;
import com.github.ppj789.booking.calendar.exception.AvailabilityBlockNotRemovableException;
import com.github.ppj789.booking.calendar.exception.CalendarConflictException;
import com.github.ppj789.booking.calendar.persistence.AvailabilityBlockEntity;
import com.github.ppj789.booking.calendar.persistence.AvailabilityBlockRepository;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationRepository;
import com.github.ppj789.booking.identity.persistence.AccountStatus;
import com.github.ppj789.booking.identity.persistence.AccountType;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountRepository;
import com.github.ppj789.booking.identity.security.AuthenticatedAccountPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffCalendarService {

    private static final String CALENDAR_OVERLAP_CONSTRAINT =
            "ex_calendar_reservation_staff_time";

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityBlockRepository availabilityBlockRepository;
    private final CalendarReservationRepository reservationRepository;
    private final UserAccountRepository userAccountRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CalendarView getCalendar(
            AuthenticatedAccountPrincipal actor,
            Instant from,
            Instant to) {
        requireActiveStaff(actor);
        CalendarIntervalPolicy.validateQueryWindow(from, to);
        loadActiveStaff(actor);

        List<CalendarAppointment> appointments = appointmentRepository
                .findCalendarAppointments(actor.accountId(), from, to)
                .stream()
                .map(StaffCalendarService::toCalendarAppointment)
                .toList();
        List<CalendarBlock> blocks = availabilityBlockRepository
                .findCalendarBlocks(actor.accountId(), from, to)
                .stream()
                .map(StaffCalendarService::toCalendarBlock)
                .toList();

        return new CalendarView(from, to, appointments, blocks);
    }

    @Transactional
    public CalendarBlock createBlock(
            AuthenticatedAccountPrincipal actor,
            Instant startsAt,
            Instant endsAt) {
        requireActiveStaff(actor);
        Instant now = clock.instant();
        CalendarIntervalPolicy.validateFutureBlock(startsAt, endsAt, now);
        UserAccountEntity staff = loadActiveStaff(actor);

        CalendarReservationEntity reservation = CalendarReservationEntity.block(
                staff.getBranch(),
                staff,
                startsAt,
                endsAt,
                now);
        try {
            reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException conflict) {
            if (!isCalendarOverlap(conflict)) {
                throw conflict;
            }
            throw new CalendarConflictException();
        }

        AvailabilityBlockEntity block = availabilityBlockRepository.saveAndFlush(
                AvailabilityBlockEntity.create(reservation));
        return toCalendarBlock(block);
    }

    @Transactional
    public void removeBlock(AuthenticatedAccountPrincipal actor, UUID blockId) {
        requireActiveStaff(actor);
        loadActiveStaff(actor);
        AvailabilityBlockEntity block = availabilityBlockRepository
                .findOwnedByIdForUpdate(blockId, actor.accountId())
                .orElseThrow(AvailabilityBlockNotFoundException::new);
        if (!block.getReservation().getStartsAt().isAfter(clock.instant())) {
            throw new AvailabilityBlockNotRemovableException();
        }

        CalendarReservationEntity reservation = block.getReservation();
        availabilityBlockRepository.delete(block);
        availabilityBlockRepository.flush();
        reservationRepository.delete(reservation);
    }

    private UserAccountEntity loadActiveStaff(AuthenticatedAccountPrincipal actor) {
        UserAccountEntity staff = userAccountRepository.findById(actor.accountId())
                .filter(account -> account.getAccountType() == AccountType.STAFF)
                .filter(account -> account.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(account -> account.getBranch() != null)
                .filter(account -> account.getBranch().getId().equals(actor.branch().id()))
                .orElseThrow(() -> new AccessDeniedException("Active Staff access is required"));
        return staff;
    }

    private static boolean isCalendarOverlap(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && CALENDAR_OVERLAP_CONSTRAINT.equals(
                            constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static CalendarAppointment toCalendarAppointment(AppointmentEntity appointment) {
        CalendarReservationEntity reservation = appointment.getReservation();
        return new CalendarAppointment(
                appointment.getId(),
                appointment.getPublicReference(),
                appointment.getInvitation().getTitle(),
                appointment.getInvitation().getGuestName(),
                reservation.getStartsAt(),
                reservation.getEndsAt());
    }

    private static CalendarBlock toCalendarBlock(AvailabilityBlockEntity block) {
        CalendarReservationEntity reservation = block.getReservation();
        return new CalendarBlock(
                block.getId(),
                reservation.getStartsAt(),
                reservation.getEndsAt());
    }

    public record CalendarView(
            Instant from,
            Instant toExclusive,
            List<CalendarAppointment> appointments,
            List<CalendarBlock> blocks) {
    }

    public record CalendarAppointment(
            UUID id,
            String reference,
            String title,
            String guestDisplayName,
            Instant startAt,
            Instant endAt) {
    }

    public record CalendarBlock(UUID id, Instant startAt, Instant endAt) {
    }
}
