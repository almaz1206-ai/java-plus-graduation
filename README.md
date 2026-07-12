# Explore With Me — microservices

The project is a Maven multi-module Spring Boot system. Public traffic enters through `gateway-server`; service discovery is provided by Eureka and configuration by Spring Cloud Config.

## Modules

- `core/interaction-api` — internal DTO and service contracts, without Spring Boot or JPA.
- `core/service-common` — reusable error primitives and technical utilities.
- `core/user-service` — user administration; owns the `users` database.
- `core/event-service` — event lifecycle and search; owns the `events` database.
- `core/request-service` — participation requests; owns the `requests` database.
- `core/category-service` — categories, compilations, comments and likes; owns the `category` database.
- `ewm-stats-service/stats-server` — hit statistics; owns the `stats` database.
- `infra/config-server` — centralized configuration.
- `infra/discovery-server` — Eureka registry.
- `infra/gateway-server` — public API routing.

The former `core/main-service` was removed: after extraction it contained no business capability or orchestration responsibility.

## Run

```shell
mvn clean verify
docker compose up --build
```

Gateway is available on port `8080`, Eureka on `8761`, and Config Server on `8888`. Database credentials can be overridden with `USER_DB_PASSWORD`, `EVENT_DB_PASSWORD`, `REQUEST_DB_PASSWORD`, `CATEGORY_DB_PASSWORD`, and `STATS_DB_PASSWORD`.
