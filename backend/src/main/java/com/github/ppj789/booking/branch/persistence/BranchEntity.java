package com.github.ppj789.booking.branch.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "branch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BranchEntity {

    @Id
    private UUID id;

    @Column(name = "public_code", nullable = false, length = 30, unique = true)
    private String publicCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "display_address", nullable = false, length = 300)
    private String displayAddress;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    private BranchEntity(
            UUID id,
            String publicCode,
            String name,
            String displayAddress,
            String timezone) {
        this.id = id;
        this.publicCode = publicCode;
        this.name = name;
        this.displayAddress = displayAddress;
        this.timezone = timezone;
        this.active = true;
    }

    public static BranchEntity demo(
            UUID id,
            String publicCode,
            String name,
            String displayAddress,
            String timezone) {
        return new BranchEntity(id, publicCode, name, displayAddress, timezone);
    }

    public void refreshReferenceData(
            String name,
            String displayAddress,
            String timezone) {
        this.name = name;
        this.displayAddress = displayAddress;
        this.timezone = timezone;
        this.active = true;
    }

}
