# Spring Boot Microservices — Identity, Config, Gateway & Discovery

A distributed backend system of 5 independently deployable Spring Boot services, demonstrating service discovery, centralized configuration, JWT-based authentication, API gateway routing, fault tolerance, distributed tracing, and containerized orchestration with CI/CD.

## What This Project Does

A client sends a request to a single entry point — the **API Gateway** — which looks up the correct downstream service via **Eureka**, validates the caller's JWT (for protected routes), and forwards the request. Each service is independently deployable, discovers its peers dynamically instead of relying on hardcoded addresses, and pulls its configuration from a shared **Config Server** at startup rather than keeping it locally. The gateway wraps every downstream route in a **Resilience4j circuit breaker** with a fallback response, so a failing service degrades gracefully instead of cascading. Every request is traced end-to-end across services via **Micrometer + Zipkin**. The whole system spins up with one command via Docker Compose, and every push is automatically built and tested across all 5 services through a CI pipeline.

The five services:

1. **Eureka Naming Server** (`localhost:8761`) — Service registry. Every other service registers itself here and discovers peers through it.
2. **Spring Cloud Config Server** (`localhost:8888`) — Centralized configuration, served from the local classpath (native profile) — no external Git dependency required.
3. **Identity Service** (`localhost:8080`) — User registration, login, and JWT token issuance/validation. Spring Security + Spring Data JPA, backed by **PostgreSQL** (H2 retained for the test profile only).
4. **Demo Controller Service** (`localhost:8081`) — A sample downstream business service exposing a JWT-protected endpoint, used to demonstrate the gateway → discovery → protected-resource flow end to end.
5. **API Gateway Service** (`localhost:8765`) — Single entry point. Routes requests via Spring Cloud Gateway, validates JWTs, and applies circuit breakers with fallback routes before forwarding to any downstream service.

## Architecture

```
                        ┌───────────────────────┐
                        │  Eureka Naming Server │
                        │      (port 8761)      │
                        └──────────┬────────────┘
                                   │ service registration
                 ┌─────────────────┼─────────────────┬──────────────────┐
                 │                 │                 │                  │
        ┌────────▼────────┐ ┌──────▼──────┐  ┌───────▼────────┐ ┌───────▼────────┐
        │  Config Server   │ │  Identity   │  │  Demo Controller│ │  API Gateway   │
        │   (port 8888)    │ │  Service    │  │   (port 8081)   │ │   (port 8765)  │
        │                  │ │ (port 8080) │  │                 │ │  circuit       │
        └──────────────────┘ └──────┬──────┘  └────────┬────────┘ │  breaker +     │
                                     │                  │          │  fallback here │
                              ┌──────▼──────┐           │          └───────┬────────┘
                              │  PostgreSQL │           └──────────────────┘
                              │  (Docker)   │       routes + validates JWT here
                              └─────────────┘

        All 3 request-path services (Gateway, Identity, Demo Controller)
        emit trace spans to → Zipkin (port 9411) for end-to-end tracing.
```

## Tech Stack

- **Java 17**, **Spring Boot 3.2.4**
- **Spring Cloud** — Netflix Eureka (service discovery), Config Server, Gateway
- **Spring Security** + **JWT** (jjwt) — stateless authentication
- **Spring Data JPA** + **PostgreSQL** (production datasource; H2 used only in the test profile for isolation)
- **Resilience4j** — circuit breaker + timeout on gateway-to-service calls, with dedicated fallback endpoints
- **Micrometer Tracing (Brave) + Zipkin** — distributed request tracing across the gateway and downstream services
- **Docker** + **Docker Compose** — one-command orchestration of all services plus Postgres and Zipkin
- **GitHub Actions** — CI pipeline builds and tests every service on each push (matrix build)
- **Lombok**, **Maven**

## How to Run

### Option A — Docker Compose (recommended)

```bash
docker compose up --build
```

Brings up all services — Postgres, Zipkin, Eureka, Config Server, Identity, Demo Controller, Gateway — networked together in the correct healthcheck-gated startup order. Check the Eureka dashboard at `http://localhost:8761` — all services should register as UP within a minute or so.

### Option B — Run locally without Docker

Each service includes the Maven wrapper (`mvnw`), so no separate Maven install is needed. Requires JDK 17+. Start in this order, each in its own terminal:

```bash
cd eureka-naming-server && ./mvnw spring-boot:run
cd spring-cloud-config-server && ./mvnw spring-boot:run
cd identity-service && ./mvnw spring-boot:run       # needs .env with SECRET_KEY
cd demo-controller-microservice && ./mvnw spring-boot:run
cd api-gateway-service && ./mvnw spring-boot:run    # needs same SECRET_KEY as identity-service
```

`identity-service` and `api-gateway-service` each require a `.env` file (not committed) containing:
```
SECRET_KEY=<base64-encoded-256-bit-key>
```
Both must use the **same** key, since the gateway validates tokens issued by the identity service.

### Verifying it's working

- Eureka dashboard: `http://localhost:8761` — lists all services as UP.
- End-to-end flow: register/login via `identity-service` through the gateway (`http://localhost:8765/identity-service/**`) to get a JWT, then call the protected demo endpoint (`http://localhost:8765/demo-controller/api/test/demo-controller/greet`) with that token.
- Circuit breaker: stop `identity-service` (`docker stop identity-service`) mid-flow — the gateway returns a `503` fallback response instead of hanging or erroring. Restart it — the breaker auto-recovers and requests succeed again.
- Distributed tracing: open `http://localhost:9411` (Zipkin), click **Run Query** after making a request through the gateway — you'll see a trace spanning the gateway and whichever downstream service handled it, with per-hop timing.

## Fault Tolerance

The API Gateway wraps its routes to `identity-service` and `demo-controller-microservice` in Resilience4j circuit breakers with a 4s timeout and dedicated fallback controllers (`/fallback/identity`, `/fallback/demo`). If a downstream service is unavailable or slow, callers get a clear `503` instead of a hung connection or a raw 500 — and the breaker auto-transitions back to closed once the service recovers.

## Observability

All three services in the request path (Gateway, Identity, Demo Controller) are instrumented with Micrometer Tracing (Brave bridge) and export spans to Zipkin. Every request gets a trace ID that's propagated through headers and shows up in each service's logs (`logging.pattern.level` includes `traceId`/`spanId`), making it possible to follow one request across service boundaries — not just read isolated per-service logs.

## CI/CD

Every push and pull request to `main` triggers a GitHub Actions workflow that builds and runs tests for all 5 services independently (matrix build) — catching compilation or integration issues before merge.

## Debugging Notes

Getting this system running locally involved resolving several real issues:

- **Lombok not generating code** under the project's JDK setup — fixed by explicitly declaring the Lombok version and adding an `annotationProcessorPaths` block to the compiler plugin.
- **JDK version mismatch** — project targets Java 17; a newer local JDK (25) broke Lombok's annotation processing. Installed JDK 17 (Eclipse Temurin) and pointed `JAVA_HOME` at it.
- **Missing environment secrets** — generated proper Base64-encoded 256-bit keys and created matching `.env` files for both services that need them.
- **No database configured** — moved to PostgreSQL via Docker Compose for the running system, with env-var-driven connection settings (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`); H2 kept only for the test profile so tests stay fast and isolated.
- **Config Server needed a Git-backed repo by default** — switched to `native` profile mode to serve config from the local classpath instead.

## Notes

- The JWT secret is pulled from a root `.env` file (gitignored) and injected via Docker Compose (`SECRET_KEY: ${SECRET_KEY}`). In production this would come from a proper secrets manager instead.
- Config Server currently serves config from the local classpath (`native` profile); for production this would point at a real Git repo or config store.
- `.env` files are excluded via `.gitignore` — create your own per service.

## License

For learning and portfolio purposes.