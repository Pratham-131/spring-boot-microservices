package com.example.apigatewayservice.routing;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/identity-service/api/auth/register",
                                "/identity-service/api/auth/authenticate")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://identity-service"))
                .route(r -> r.path("/identity-service/api/auth/logout")
                        .filters(f -> f.stripPrefix(1)
                                .circuitBreaker(c -> c.setName("identityCB")
                                        .setFallbackUri("forward:/fallback/identity")))
                        .uri("lb://identity-service"))
                .route(r -> r.path("/demo-controller/api/test/demo-controller/greet")
                        .filters(f -> f.stripPrefix(1)
                                .circuitBreaker(c -> c.setName("demoCB")
                                        .setFallbackUri("forward:/fallback/demo")))
                        .uri("lb://demo-controller"))
                .build();
    }
}