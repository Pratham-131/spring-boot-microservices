# Spring Boot Microservices — Identity, Config & Gateway

A microservices-based backend system built with Spring Boot, demonstrating service discovery, centralized configuration, JWT-based authentication, and API gateway routing.

## What This Project Does

This system is made up of four independent Spring Boot services that work together:

1. **Eureka Naming Server** (`localhost:8761`) — Service registry. Every other service registers itself here and discovers other services through it instead of using hardcoded URLs.
2. **Spring Cloud Config Server** (`localhost:8888`) — Centralized configuration server. Services fetch their configuration from here at startup instead of keeping it locally.
3. **Identity Service** (`localhost:8080`) — Handles user registration, login, and JWT token issuance/validation. Uses Spring Security, JPA, and an H2 in-memory database.
4. **API Gateway Service** (`localhost:8765`) — Single entry point that routes incoming requests to the correct downstream service using Spring Cloud Gateway, and validates JWT tokens before forwarding requests.

## Architecture

```
                        ┌─────────────────────┐
                        │  Eureka Naming Server │
                        │      (port 8761)      │
                        └──────────┬────────────┘
                                   │ service registration
                 ┌─────────────────┼─────────────────┐
                 │                 │                 │
        ┌────────▼────────┐ ┌──────▼──────┐  ┌───────▼────────┐
        │  Config Server   │ │  Identity    │  │  API Gateway   │
        │   (port 8888)    │ │  Service     │  │   (port 8765)  │
        │                  │ │ (port 8080)  │  │                │
        └──────────────────┘ └──────┬───────┘  └───────┬────────┘
                                     │                   │
                              ┌──────▼──────┐            │
                              │ H2 Database │◄───────────┘
                              │ (in-memory) │   routes requests here
                              └─────────────┘
```

All requests from a client go through the **API Gateway**, which looks up the target service via **Eureka**, validates the JWT (for protected routes), and forwards the request.

## Tech Stack

- **Java 17**, **Spring Boot 3.2.4**
- **Spring Cloud** — Netflix Eureka (service discovery), Config Server, Gateway
- **Spring Security** + **JWT** (jjwt) — stateless authentication
- **Spring Data JPA** + **H2** (in-memory database for local development; MySQL-ready via included connector)
- **Lombok** — boilerplate reduction
- **Maven** — build tool

## How to Run

### Prerequisites
- JDK 17 or newer
- No need to install Maven separately — each service includes the Maven wrapper (`mvnw`)

### Startup order matters

Services must be started in this order, each in its own terminal:

**1. Eureka Naming Server**
```bash
cd eureka-naming-server
./mvnw spring-boot:run
```
Wait for `Started EurekaNamingServerApplication`. Confirm at `http://localhost:8761`.

**2. Spring Cloud Config Server**
```bash
cd spring-cloud-config-server
./mvnw spring-boot:run
```

**3. Identity Service**
```bash
cd identity-service
./mvnw spring-boot:run
```
Requires a `.env` file in this folder with:
```
SECRET_KEY=<base64-encoded-256-bit-key>
```

**4. API Gateway Service**
```bash
cd api-gateway-service
./mvnw spring-boot:run
```
Also requires a matching `.env` file with the **same** `SECRET_KEY` as identity-service, since the gateway validates tokens issued by identity-service.

### Verifying it's working

- Eureka dashboard: `http://localhost:8761` — should list `IDENTITY-SERVICE`, `SPRING-CLOUD-CONFIG-SERVER`, and `API-GATEWAY-SERVICE` as UP.
- H2 console: `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:identitydb`, username `sa`, no password.
- Routing through the gateway: requests to `http://localhost:8765/identity-service/**` are forwarded to the Identity Service.

## Debugging Notes (What I Fixed)

Getting this system running locally involved resolving several real issues, documented here for transparency:

- **Lombok not generating code** — Maven wasn't running Lombok's annotation processor by default under the project's JDK setup. Fixed by explicitly declaring the Lombok version and adding an `annotationProcessorPaths` block to the compiler plugin.
- **JDK version mismatch** — The project targets Java 17, but a newer locally-installed JDK (25) caused Lombok to fail with an internal compiler error. Installed JDK 17 (Eclipse Temurin) alongside the existing JDK and pointed `JAVA_HOME` at it.
- **Missing environment secrets** — Both Identity Service and API Gateway read a `SECRET_KEY` from a `.env` file that isn't (and shouldn't be) committed to source control. Generated a proper Base64-encoded 256-bit key and created matching `.env` files for both services.
- **No database configured** — The original setup expected a MySQL connection that wasn't available locally. Swapped in an H2 in-memory database for local development (MySQL connector is still present for production use).
- **Config Server needed a Git-backed repository** — By default, Spring Cloud Config Server expects a Git URI for configuration. Switched it to `native` profile mode, which serves configuration from the local classpath instead — no external Git dependency needed for local development.

## Notes

- This project is set up for local development. For production, the H2 database would be replaced with MySQL/PostgreSQL, the JWT secret would be pulled from a proper secrets manager, and the Config Server would point to a real Git repository or config store.
- `.env` files are excluded from version control via `.gitignore` — you'll need to create your own for each service that requires one.

## License

For learning and portfolio purposes.