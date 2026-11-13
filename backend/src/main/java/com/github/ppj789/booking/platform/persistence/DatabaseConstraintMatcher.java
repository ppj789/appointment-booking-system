package com.github.ppj789.booking.platform.persistence;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;

public final class DatabaseConstraintMatcher {

    private DatabaseConstraintMatcher() {
    }

    public static boolean matches(Throwable failure, String expectedName) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && expectedName.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            if (cause instanceof SQLException sqlException
                    && sqlException.getMessage() != null
                    && sqlException.getMessage().contains(expectedName)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
