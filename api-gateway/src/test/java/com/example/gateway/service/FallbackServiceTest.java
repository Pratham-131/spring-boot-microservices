package com.example.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class FallbackServiceTest {
    private final FallbackService fallbackService = new FallbackService();

    @Test
    void unavailableReturnsServiceUnavailableResponse() {
        var response = fallbackService.unavailable();
        var body = response.getBody();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(body);
        assertEquals(503, body.status());
        assertEquals("Service Unavailable", body.error());
        assertEquals("Service unavailable", body.message());
        assertNotNull(body.timestamp());
    }
}