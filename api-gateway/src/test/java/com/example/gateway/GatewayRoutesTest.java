package com.example.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

@SpringBootTest
class GatewayRoutesTest {
    @Autowired RouteLocator routeLocator;

    @Test
    void authRouteUsesConfiguredDestination() {
        assertEquals(URI.create("http://localhost:4001"), route("auth-service").getUri());
    }

    @Test
    void authRouteMatchesAuthRequests() {
        assertTrue(Mono.from(route("auth-service").getPredicate().apply(exchange("/auth/login"))).block());
    }

    @Test
    void authRouteDoesNotMatchProductRequests() {
        assertFalse(Mono.from(route("auth-service").getPredicate().apply(exchange("/products"))).block());
    }

    @Test
    void productRouteUsesConfiguredDestination() {
        assertEquals(URI.create("http://localhost:4002"), route("product-service").getUri());
    }

    @Test
    void productRouteMatchesProductRequests() {
        assertTrue(Mono.from(route("product-service").getPredicate().apply(exchange("/products/1"))).block());
    }

    @Test
    void productRouteDoesNotMatchAuthRequests() {
        assertFalse(Mono.from(route("product-service").getPredicate().apply(exchange("/auth/login"))).block());
    }

    @Test
    void bothRoutesHaveCircuitBreakerFilters() {
        assertFalse(route("auth-service").getFilters().isEmpty());
        assertFalse(route("product-service").getFilters().isEmpty());
    }

    private Route route(String id) {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        return routes.stream().filter(route -> route.getId().equals(id)).findFirst().orElseThrow();
    }

    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }
}