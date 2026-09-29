package com.example.gateway.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.reactive.server.WebTestClient.bindToController;

import com.example.gateway.model.ApiError;
import com.example.gateway.service.FallbackService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;

class FallbackControllerTest {
    @Test
    void fallbackRespondsWithUnavailable() {
        FallbackService fallbackService = org.mockito.Mockito.mock(FallbackService.class);
        when(fallbackService.unavailable()).thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiError(Instant.parse("2026-01-01T00:00:00Z"), 503,
                "Service Unavailable", "Service unavailable")));
        WebTestClient client = bindToController(new FallbackController(fallbackService)).build();

        client.post().uri("/fallback").exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody().jsonPath("$.status").isEqualTo(503)
            .jsonPath("$.error").isEqualTo("Service Unavailable")
            .jsonPath("$.message").isEqualTo("Service unavailable")
            .jsonPath("$.timestamp").isEqualTo("2026-01-01T00:00:00Z");
    }
}