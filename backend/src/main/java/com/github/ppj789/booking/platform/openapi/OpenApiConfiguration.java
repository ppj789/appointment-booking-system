package com.github.ppj789.booking.platform.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Appointment Booking API",
                version = "0.1.0",
                description = "MVP API for Branch Admin, Staff calendar, Invitation, and Guest appointment journeys."))
public class OpenApiConfiguration {
}
