package com.github.ppj789.booking.identity.exception;

import java.time.Instant;
import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final Instant retryAt;

    public RateLimitExceededException(Instant retryAt) {
        super("Request rate limit exceeded");
        this.retryAt = retryAt;
    }
}
