package com.github.ppj789.booking.calendar.persistence;

import com.github.ppj789.booking.branch.persistence.BranchEntity;
import com.github.ppj789.booking.identity.persistence.UserAccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "calendar_reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private UserAccountEntity staffUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CalendarReservationKind kind;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private CalendarReservationEntity(
            BranchEntity branch,
            UserAccountEntity staffUser,
            CalendarReservationKind kind,
            Instant startsAt,
            Instant endsAt,
            Instant createdAt) {
        this.branch = branch;
        this.staffUser = staffUser;
        this.kind = kind;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
    }

    public static CalendarReservationEntity block(
            BranchEntity branch,
            UserAccountEntity staffUser,
            Instant startsAt,
            Instant endsAt,
            Instant createdAt) {
        return new CalendarReservationEntity(
                branch,
                staffUser,
                CalendarReservationKind.BLOCK,
                startsAt,
                endsAt,
                createdAt);
    }

    public static CalendarReservationEntity appointment(
            BranchEntity branch,
            UserAccountEntity staffUser,
            Instant startsAt,
            Instant endsAt,
            Instant createdAt) {
        return new CalendarReservationEntity(
                branch,
                staffUser,
                CalendarReservationKind.APPOINTMENT,
                startsAt,
                endsAt,
                createdAt);
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }
}
