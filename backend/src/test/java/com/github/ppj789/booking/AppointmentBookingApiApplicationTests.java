package com.github.ppj789.booking;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class AppointmentBookingApiApplicationTests {

    @Test
    void applicationEntryPointIsConfigured() {
        assertTrue(AppointmentBookingApiApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }
}
