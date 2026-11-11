package com.github.ppj789.booking.platform.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestObservabilityFilterTests {

    private static final String CORRELATION_ID =
            "7c1ad3d0-b0b3-42ed-903b-e131bc8cb6b1";

    private final RequestObservabilityFilter filter =
            new RequestObservabilityFilter(new CorrelationIdGenerator());

    @Test
    void preservesAValidCorrelationIdAcrossRequestLogsAndTheResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/guest/invitation");
        request.addHeader(RequestObservabilityFilter.CORRELATION_HEADER, CORRELATION_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (servletRequest, servletResponse) -> {
            assertEquals(
                    CORRELATION_ID,
                    request.getAttribute(RequestObservabilityFilter.CORRELATION_ATTRIBUTE));
            assertEquals(
                    CORRELATION_ID,
                    MDC.get(RequestObservabilityFilter.CORRELATION_MDC_KEY));
            response.setStatus(204);
        });

        assertEquals(
                CORRELATION_ID,
                response.getHeader(RequestObservabilityFilter.CORRELATION_HEADER));
        assertNull(MDC.get(RequestObservabilityFilter.CORRELATION_MDC_KEY));
    }

    @Test
    void replacesAnInvalidExternalCorrelationValueWithANewUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/guest/invitation");
        request.addHeader(
                RequestObservabilityFilter.CORRELATION_HEADER,
                "guest@example.test-secret-value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(
                request,
                response,
                (servletRequest, servletResponse) -> response.setStatus(200));

        String generated = response.getHeader(RequestObservabilityFilter.CORRELATION_HEADER);
        assertEquals(generated, UUID.fromString(generated).toString());
        assertFalse(generated.contains("guest@example.test"));
        assertNull(MDC.get(RequestObservabilityFilter.CORRELATION_MDC_KEY));
    }
}
