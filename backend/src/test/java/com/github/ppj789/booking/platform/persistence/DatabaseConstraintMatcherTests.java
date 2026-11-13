package com.github.ppj789.booking.platform.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DatabaseConstraintMatcherTests {

    private static final String CALENDAR_CONSTRAINT =
            "ex_calendar_reservation_staff_time";

    @Test
    void matchesTheNamedHibernateConstraint() {
        ConstraintViolationException hibernateFailure = new ConstraintViolationException(
                "calendar overlap",
                new SQLException("calendar overlap"),
                CALENDAR_CONSTRAINT);

        assertTrue(DatabaseConstraintMatcher.matches(
                new DataIntegrityViolationException("calendar overlap", hibernateFailure),
                CALENDAR_CONSTRAINT));
    }

    @Test
    void matchesThePostgresMessageWhenHibernateOmitsTheConstraintName() {
        SQLException postgresFailure = new SQLException(
                "violates exclusion constraint \"" + CALENDAR_CONSTRAINT + "\"",
                "23P01");
        ConstraintViolationException hibernateFailure = new ConstraintViolationException(
                "calendar overlap",
                postgresFailure,
                null);

        assertTrue(DatabaseConstraintMatcher.matches(
                new DataIntegrityViolationException("calendar overlap", hibernateFailure),
                CALENDAR_CONSTRAINT));
    }

    @Test
    void doesNotReclassifyAnUnrelatedIntegrityFailure() {
        SQLException postgresFailure = new SQLException(
                "violates constraint \"some_other_constraint\"",
                "23505");

        assertFalse(DatabaseConstraintMatcher.matches(
                new DataIntegrityViolationException("unexpected", postgresFailure),
                CALENDAR_CONSTRAINT));
    }
}
