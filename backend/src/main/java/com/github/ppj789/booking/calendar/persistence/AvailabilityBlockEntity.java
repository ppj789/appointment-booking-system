package com.github.ppj789.booking.calendar.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "availability_block")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityBlockEntity {

    @Id
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", nullable = false)
    private CalendarReservationEntity reservation;

    private AvailabilityBlockEntity(CalendarReservationEntity reservation) {
        this.reservation = reservation;
    }

    public static AvailabilityBlockEntity create(CalendarReservationEntity reservation) {
        return new AvailabilityBlockEntity(reservation);
    }
}
