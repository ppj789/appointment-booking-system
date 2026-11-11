package com.github.ppj789.booking.platform.web;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CorrelationIdGenerator {

    String resolve(String candidate) {
        if (candidate != null && candidate.length() == 36) {
            try {
                UUID parsed = UUID.fromString(candidate);
                if (parsed.toString().equalsIgnoreCase(candidate)) {
                    return parsed.toString();
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid external values are replaced rather than reflected into logs.
            }
        }
        return UUID.randomUUID().toString();
    }
}
