package com.example.apigatewayservice.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @GetMapping("/fallback/identity")
    public Mono<ResponseEntity<String>> identityFallback() {
        return Mono.just(
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Identity Service unavailable. Try later.")
        );
    }

    @GetMapping("/fallback/demo")
    public Mono<ResponseEntity<String>> demoFallback() {
        return Mono.just(
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Demo Service unavailable. Try later.")
        );
    }
}