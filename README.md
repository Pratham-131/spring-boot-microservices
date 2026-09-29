# Spring Boot Microservices

A small Java 17 and Spring Boot 3.2 project with an API gateway, authentication service, product service, MongoDB, JWT, and Resilience4j circuit breakers. Each service is an independent Maven project.

## Architecture

```text
Clients -> API Gateway :8080 -> Auth service :4001 -> MongoDB authdb
                             -> Product service :4002 -> MongoDB productdb
```

The gateway forwards `/auth/**` and `/products/**` to their services. The services are not published on host ports by Compose; only the gateway (`8080`) and MongoDB (`27017`) are. MongoDB stores separate databases for auth and product data. Circuit-breaker fallbacks return HTTP 503 using the standard error JSON format.

## Services and Endpoints

| Service | Container port | Endpoints |
| --- | ---: | --- |
| `auth-service` | 4001 | `POST /auth/register`, `POST /auth/login`, `GET /actuator/health`, `/v3/api-docs`, `/swagger-ui/index.html` |
| `product-service` | 4002 | `GET /products`, `GET /products/{id}`, `POST /products`, `PUT /products/{id}`, `DELETE /products/{id}`, `GET /actuator/health`, `/v3/api-docs`, `/swagger-ui/index.html` |
| `api-gateway` | 8080 | Routes auth and product requests; `GET /actuator/health` |

Product reads are public. Create, update, and delete require `Authorization: Bearer <token>`; JWT signature and expiry are checked in `product-service`. Registration stores a BCrypt password hash but returns only the user ID and email. Login issues a one-hour JWT. Product request bodies require a nonblank name and a price greater than zero. Duplicate registration returns 409; invalid credentials or missing/invalid write tokens return 401; validation errors return 400; missing products return 404.

Errors use this JSON shape:

```json
{"timestamp":"2026-09-29T12:00:00Z","status":404,"error":"Not Found","message":"Product not found"}
```

## Run With Docker Compose

Copy `.env.example` to `.env` and replace `JWT_SECRET` with a random value at least 32 bytes long. `.env` is ignored by Git. Then run from the repository root:

```bash
docker compose up --build
```

Compose waits for MongoDB and each service healthcheck before starting dependents. The gateway is at `http://localhost:8080` and its health endpoint is `http://localhost:8080/actuator/health`. Auth and product Swagger UI and health endpoints are available on their service ports when running those services directly; Compose keeps those ports internal. MongoDB is published on `27017` and the gateway on `8080` by default; set `MONGO_HOST_PORT` or `GATEWAY_HOST_PORT` in `.env` if either host port is already in use. Stop the stack with `Ctrl+C` or `docker compose down`; the MongoDB volume is retained.

## Environment Variables

| Variable | Used by | Purpose |
| --- | --- | --- |
| `JWT_SECRET` | Auth and product services | Shared HMAC signing key; required, no committed default |
| `AUTH_MONGO_URI` | Compose | Auth MongoDB connection URI; defaults to `mongodb://mongo:27017/authdb` |
| `PRODUCT_MONGO_URI` | Compose | Product MongoDB connection URI; defaults to `mongodb://mongo:27017/productdb` |
| `MONGO_HOST_PORT` | Compose | Host port for MongoDB; defaults to `27017` |
| `GATEWAY_HOST_PORT` | Compose | Host port for the gateway; defaults to `8080` |
| `MONGO_URI` | Auth or product when run directly | Service MongoDB URI; defaults to its local database on `localhost:27017` |
| `AUTH_SERVICE_URL` | Gateway | Auth service address; defaults to `http://localhost:4001` |
| `PRODUCT_SERVICE_URL` | Gateway | Product service address; defaults to `http://localhost:4002` |

Compose passes the same `JWT_SECRET` to both JWT services. The example URIs use the internal Compose hostname `mongo`; for direct local runs, set `MONGO_URI` to the appropriate localhost database.

## API Walkthrough

These Bash commands exercise the complete flow through the gateway. They require `curl` and `jq`.

Register and log in:

```bash
curl -i -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"change-me"}'

TOKEN=$(curl -sS -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"change-me"}' | jq -r '.token')
```

Create and read a product without a token for GET:

```bash
PRODUCT_ID=$(curl -sS -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard","price":49.99}' | jq -r '.id')

curl http://localhost:8080/products
curl "http://localhost:8080/products/$PRODUCT_ID"
```

Update and delete with the same token:

```bash
curl -i -X PUT "http://localhost:8080/products/$PRODUCT_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical Keyboard","price":59.99}'

curl -i -X DELETE "http://localhost:8080/products/$PRODUCT_ID" \
  -H "Authorization: Bearer $TOKEN"
```

## Build and Test

Run each independent Maven verification from the repository root:

```bash
mvn -B verify -f auth-service/pom.xml
mvn -B verify -f product-service/pom.xml
mvn -B verify -f api-gateway/pom.xml
```

Tests cover registration and login success/failure, duplicate emails, password-hash omission, product CRUD and not-found cases, validation, JWT requirements, gateway routes, and fallback responses.

## Design Decisions

- **Database per service:** auth and product own separate MongoDB databases, keeping their data and schemas independent without adding another database technology.
- **Circuit breaker:** Resilience4j prevents repeated calls to an unavailable downstream service and returns a clear 503 fallback.
- **JWT:** the auth service issues signed, expiring tokens; product-service verifies them locally for write requests, avoiding a database lookup on each product mutation.
- **Scaling later:** services can be replicated independently behind a load balancer; a service registry or orchestration platform is not needed for this small Compose deployment.

## CI

GitHub Actions runs Maven `verify` for all three services on Java 17, caches Maven dependencies, and runs on pushes and pull requests targeting `main`.

[![CI](https://github.com/Pratham-131/spring-boot-microservices/actions/workflows/ci.yml/badge.svg)](https://github.com/Pratham-131/spring-boot-microservices/actions/workflows/ci.yml)