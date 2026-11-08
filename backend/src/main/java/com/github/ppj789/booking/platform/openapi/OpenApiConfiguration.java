package com.github.ppj789.booking.platform.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "sessionCookie",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "JSESSIONID",
        description = "Secure, HttpOnly, same-origin session cookie")
@SecurityScheme(
        name = "setupToken",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Setup-Token",
        description = "Single-use Staff setup capability read from the frontend URL fragment")
@OpenAPIDefinition(
        info = @Info(
                title = "Appointment Booking API",
                version = "0.1.0",
                description = "MVP API for Admin, Staff calendar, Invitation, and Guest appointment journeys."))
public class OpenApiConfiguration {
}
