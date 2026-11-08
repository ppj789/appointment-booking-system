package com.github.ppj789.booking.identity.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("booking.identity-security")
public record IdentitySecurityProperties(
        Duration credentialSetupExpiry,
        Duration absoluteSessionTimeout,
        RateLimit loginRateLimit,
        RateLimit setupRateLimit,
        RateLimit provisioningRateLimit) {

    public IdentitySecurityProperties {
        requirePositive(credentialSetupExpiry, "credential setup expiry");
        requirePositive(absoluteSessionTimeout, "absolute session timeout");
        if (loginRateLimit == null || setupRateLimit == null || provisioningRateLimit == null) {
            throw new IllegalArgumentException("Identity rate limits must be configured");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    public record RateLimit(int attempts, Duration window) {

        public RateLimit {
            if (attempts < 1) {
                throw new IllegalArgumentException("Rate-limit attempts must be positive");
            }
            requirePositive(window, "rate-limit window");
        }
    }
}
