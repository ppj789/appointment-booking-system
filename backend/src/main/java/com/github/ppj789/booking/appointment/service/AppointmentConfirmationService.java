package com.github.ppj789.booking.appointment.service;

import com.github.ppj789.booking.appointment.exception.IdempotencyKeyReusedException;
import com.github.ppj789.booking.appointment.exception.SlotUnavailableException;
import com.github.ppj789.booking.appointment.persistence.AppointmentEntity;
import com.github.ppj789.booking.appointment.persistence.AppointmentRepository;
import com.github.ppj789.booking.appointment.service.AppointmentBookingPolicy.AppointmentInterval;
import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationEntity;
import com.github.ppj789.booking.calendar.persistence.CalendarReservationRepository;
import com.github.ppj789.booking.identity.security.SecretHasher;
import com.github.ppj789.booking.invitation.exception.InvitationUnavailableException;
import com.github.ppj789.booking.invitation.persistence.InvitationEntity;
import com.github.ppj789.booking.invitation.persistence.InvitationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentConfirmationService {

    private static final int MAXIMUM_TOKEN_LENGTH = 128;
    private static final String CALENDAR_OVERLAP_CONSTRAINT =
            "ex_calendar_reservation_staff_time";
    private static final String IDEMPOTENCY_KEY_CONSTRAINT =
            "uq_appointment_idempotency_key";

    private final AppointmentRepository appointmentRepository;
    private final CalendarReservationRepository reservationRepository;
    private final InvitationRepository invitationRepository;
    private final AppointmentReferenceIssuer referenceIssuer;
    private final SecretHasher secretHasher;
    private final Clock clock;

    @Transactional
    public ConfirmationResult confirm(
            String rawToken,
            UUID idempotencyKey,
            Instant startsAt,
            int durationMinutes) {
        String tokenHash = hashToken(rawToken);
        Optional<AppointmentEntity> existing =
                appointmentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), tokenHash, startsAt, durationMinutes);
        }

        InvitationEntity invitation = invitationRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(InvitationUnavailableException::new);

        Optional<AppointmentEntity> committedWhileWaiting =
                appointmentRepository.findByIdempotencyKey(idempotencyKey);
        if (committedWhileWaiting.isPresent()) {
            return replay(
                    committedWhileWaiting.get(),
                    tokenHash,
                    startsAt,
                    durationMinutes);
        }
        if (!invitation.isBookable()) {
            throw new InvitationUnavailableException();
        }

        Instant createdAt = clock.instant();
        AppointmentInterval interval = AppointmentBookingPolicy.validate(
                createdAt,
                startsAt,
                durationMinutes);
        String fingerprint = requestFingerprint(
                invitation.getId(),
                interval.startsAt(),
                durationMinutes);

        CalendarReservationEntity reservation = CalendarReservationEntity.appointment(
                invitation.getBranch(),
                invitation.getStaffUser(),
                interval.startsAt(),
                interval.endsAt(),
                createdAt);
        saveReservation(reservation);

        AppointmentEntity appointment = AppointmentEntity.create(
                reservation,
                invitation,
                referenceIssuer.issue(),
                idempotencyKey,
                fingerprint,
                createdAt);
        saveAppointment(appointment);

        invitation.consume(createdAt);
        invitationRepository.flush();
        return ConfirmationResult.from(appointment, false);
    }

    private ConfirmationResult replay(
            AppointmentEntity existing,
            String tokenHash,
            Instant startsAt,
            int durationMinutes) {
        InvitationEntity invitation = existing.getInvitation();
        String fingerprint = startsAt == null
                ? ""
                : requestFingerprint(invitation.getId(), startsAt, durationMinutes);
        if (!tokenHash.equals(invitation.getTokenHash())
                || !fingerprint.equals(existing.getRequestFingerprint())) {
            throw new IdempotencyKeyReusedException();
        }
        return ConfirmationResult.from(existing, true);
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > MAXIMUM_TOKEN_LENGTH) {
            throw new InvitationUnavailableException();
        }
        return secretHasher.sha256(rawToken);
    }

    private String requestFingerprint(
            UUID invitationId,
            Instant startsAt,
            int durationMinutes) {
        return secretHasher.sha256(
                invitationId + "|" + startsAt + "|" + durationMinutes);
    }

    private void saveReservation(CalendarReservationEntity reservation) {
        try {
            reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException conflict) {
            if (!hasConstraint(conflict, CALENDAR_OVERLAP_CONSTRAINT)) {
                throw conflict;
            }
            throw new SlotUnavailableException();
        }
    }

    private void saveAppointment(AppointmentEntity appointment) {
        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException conflict) {
            if (!hasConstraint(conflict, IDEMPOTENCY_KEY_CONSTRAINT)) {
                throw conflict;
            }
            throw new IdempotencyKeyReusedException();
        }
    }

    private static boolean hasConstraint(
            DataIntegrityViolationException exception,
            String expectedName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && expectedName.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public record ConfirmationResult(
            UUID appointmentId,
            String reference,
            String title,
            String description,
            String hostDisplayName,
            String guestDisplayName,
            String guestEmail,
            Instant startAt,
            Instant endAt,
            Instant createdAt,
            ConfirmationBranch branch,
            boolean replayed) {

        private static ConfirmationResult from(
                AppointmentEntity appointment,
                boolean replayed) {
            InvitationEntity invitation = appointment.getInvitation();
            CalendarReservationEntity reservation = appointment.getReservation();
            BranchEntity branch = appointment.getBranch();
            return new ConfirmationResult(
                    appointment.getId(),
                    appointment.getPublicReference(),
                    invitation.getTitle(),
                    invitation.getDescription(),
                    appointment.getStaffUser().getDisplayName(),
                    invitation.getGuestName(),
                    invitation.getGuestEmail(),
                    reservation.getStartsAt(),
                    reservation.getEndsAt(),
                    appointment.getCreatedAt(),
                    new ConfirmationBranch(
                            branch.getPublicCode(),
                            branch.getName(),
                            branch.getDisplayAddress(),
                            branch.getTimezone()),
                    replayed);
        }
    }

    public record ConfirmationBranch(
            String publicCode,
            String name,
            String displayAddress,
            String timezone) {
    }
}
