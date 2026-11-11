package com.github.ppj789.booking.platform.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.ppj789.booking.platform.web.RequestObservabilityFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SecurityProblemResponseWriterTests {

    private static final String CORRELATION_ID =
            "7c1ad3d0-b0b3-42ed-903b-e131bc8cb6b1";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityProblemResponseWriter writer =
            new SecurityProblemResponseWriter(objectMapper);

    @Test
    void writesSafeCorrelatedAuthenticationProblemDetails() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.writeAuthenticationRequired(request, response);

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, response.getContentType());
        assertEquals("AUTHENTICATION_REQUIRED", body.get("code").stringValue());
        assertEquals(CORRELATION_ID, body.get("correlationId").stringValue());
        assertEquals("/api/v1/me/calendar", body.get("instance").stringValue());
    }

    @Test
    void writesSafeCorrelatedAccessDeniedProblemDetails() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.writeAccessDenied(request, response);

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertEquals("ACCESS_DENIED", body.get("code").stringValue());
        assertEquals(CORRELATION_ID, body.get("correlationId").stringValue());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/v1/me/calendar");
        request.setAttribute(
                RequestObservabilityFilter.CORRELATION_ATTRIBUTE,
                CORRELATION_ID);
        return request;
    }
}
