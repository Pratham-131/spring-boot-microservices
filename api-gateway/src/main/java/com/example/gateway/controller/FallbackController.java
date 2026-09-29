package com.example.gateway.controller;

import com.example.gateway.model.ApiError;
import com.example.gateway.service.FallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {
    private final FallbackService fallbackService;

    public FallbackController(FallbackService fallbackService) {
        this.fallbackService = fallbackService;
    }

    @RequestMapping("/fallback")
    public ResponseEntity<ApiError> fallback() {
        return fallbackService.unavailable();
    }
}