package com.example.gateway.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.reactive.server.WebTestClient.bindToController;

import com.example.gateway.service.FallbackService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;

class FallbackControllerTest {
    @Test
    void fallbackRespondsWithUnavailable() {
        FallbackService fallbackService = org.mockito.Mockito.mock(FallbackService.class);
        when(fallbackService.unavailable()).thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Service unavailable")));
        WebTestClient client = bindToController(new FallbackController(fallbackService)).build();

        client.post().uri("/fallback").exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody().json("{\"message\":\"Service unavailable\"}");
    }
}