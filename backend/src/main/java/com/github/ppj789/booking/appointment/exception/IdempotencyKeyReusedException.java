package com.github.ppj789.booking.appointment.exception;

public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException() {
        super("The Idempotency Key was already used for a different request");
    }
}
