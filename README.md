# Spring Boot Microservices

A small three-service system built with Java 17, Spring Boot 3.2, Maven, MongoDB, Spring Security, JWT, Spring Cloud Gateway, and Resilience4j. Each service is a standalone Maven project with its own dependencies, tests, and Dockerfile.

## What this project does

This project is a simple microservices-based backend for managing user authentication and product catalog data. The `auth-service` handles user registration and login, issues JWT tokens, and validates credentials. The `product-service` stores and exposes products, while the `api-gateway` sits in front of both services to route incoming requests, secure product creation, and provide fallback responses when a downstream service is unavailable.

The system demonstrates how separate Spring Boot services can work together using Docker Compose, MongoDB, JWT-based security, and gateway routing in a realistic cloud-native setup.

## Architecture

```text
                           +------------------+
                           |     Clients      |
                           +--------+---------+
                                    |
                           +--------v---------+
                           |   API Gateway    |
                           |      :8080       |
                           | Circuit breakers |
                           +----+--------+----+
                                |        |
                   /auth/**     |        |     /products/**
                         +------v--+  +--v-----------+
                         |  Auth   |  |   Product    |
                         | :4001  |  |    :4002     |
                         +----+----+  +------+-------+
                              |              |
                       +------v--------------v------+
                       |           MongoDB          |
                       | authdb          productdb  |
                       +----------------------------+
```

The gateway forwards `/auth/**` and `/products/**` unchanged. If either downstream service is unavailable, its circuit breaker forwards to a fallback that responds with `503` and `{"message":"Service unavailable"}`. Only the gateway (`8080`) and MongoDB (`27017`) are published by Docker Compose; service ports are available within the Compose network.

## Services

| Service | Port | API | Data |
| --- | ---: | --- | --- |
| `auth-service` | 4001 | `POST /auth/register`, `POST /auth/login` | MongoDB `authdb` |
| `product-service` | 4002 | `GET /products`, `POST /products` | MongoDB `productdb` |
| `api-gateway` | 8080 | Routes requests to both services | None |

Registration stores a BCrypt-hashed password. Login returns a signed JWT with a one-hour expiry. Product listing is public; creating a product requires `Authorization: Bearer <token>`. Invalid or missing tokens receive `401 Unauthorized`. Register, login, and product request bodies are validated; invalid input receives `400 Bad Request`.

## Requirements

- Java 17
- Maven 3.6.3 or later
- Docker Engine with the Docker Compose plugin
- `curl` for the examples; `jq` is used to extract the login token

## Run With Docker

From the repository root, build and start MongoDB and all three services:

```bash
docker compose up --build
```

The gateway is available at `http://localhost:8080`. Give the services a few seconds to finish starting before sending requests. Stop the stack with `Ctrl+C`, or run `docker compose down` in another terminal. The MongoDB data volume is retained when containers stop.

### Environment Variables

Docker Compose supplies the internal MongoDB and service URLs. These defaults are also available when running each service directly:

| Variable | Used by | Default |
| --- | --- | --- |
| `JWT_SECRET` | Auth and product services | `local-development-secret-key-change-before-deploying-123456` |
| `MONGO_URI` | Auth service | `mongodb://localhost:27017/authdb` |
| `MONGO_URI` | Product service | `mongodb://localhost:27017/productdb` |
| `AUTH_SERVICE_URL` | Gateway | `http://localhost:4001` |
| `PRODUCT_SERVICE_URL` | Gateway | `http://localhost:4002` |

Set the same `JWT_SECRET` for auth and product services. The checked-in default is for local development only; replace it with a random secret of at least 32 bytes outside local development. Compose reads optional overrides from the shell or a root `.env` file.

## API Walkthrough

The following Bash examples register a user, log in, create a product with the returned token, and list products through the gateway.

Register:

```bash
curl -i -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"change-me"}'
```

Log in and capture the token:

```bash
TOKEN=$(curl -sS -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"change-me"}' | jq -r '.token')
```

Create a product:

```bash
curl -i -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard","price":49.99}'
```

List products without authentication:

```bash
curl http://localhost:8080/products
```

Confirm that product creation without a token is rejected:

```bash
curl -i -X POST http://localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard","price":49.99}'
```

## Build and Test

Run verification for each independent Maven project from the repository root:

```bash
mvn -B verify -f auth-service/pom.xml
mvn -B verify -f product-service/pom.xml
mvn -B verify -f api-gateway/pom.xml
```

Each service includes two JUnit tests: a service-layer test and a controller test. GitHub Actions runs the same `mvn -B verify` command for all three services on Java 17 using a matrix.

## CI

[![CI](https://github.com/OWNER/REPOSITORY/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPOSITORY/actions/workflows/ci.yml)

Replace `OWNER/REPOSITORY` in the badge URL with the GitHub repository path.