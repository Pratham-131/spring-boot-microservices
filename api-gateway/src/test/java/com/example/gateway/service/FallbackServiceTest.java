package com.example.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class FallbackServiceTest {
    private final FallbackService fallbackService = new FallbackService();

    @Test
    void unavailableReturnsServiceUnavailableResponse() {
        var response = fallbackService.unavailable();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Service unavailable", response.getBody().get("message"));
    }
}