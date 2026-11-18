# tourist-backend

Backend of the [`mobile/`](../mobile) app (Spring Boot 3 / Java 21).

It has its own PostgreSQL 16 + PostGIS database and talks to
[`admin-backend/`](../admin-backend) only over the private network.

## Build and test

Java 21 and Maven are required.

```bash
mvn verify -DskipITs     # build, unit tests, contract tests, format check
mvn verify               # also runs the integration tests (*IT), which need Docker
mvn spotless:apply       # fix formatting
```

The integration tests start PostgreSQL 16 with PostGIS in Docker and run every
Flyway migration in `src/main/resources/db/migration`.

## Configuration

The database comes from the environment: `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. The application
runs the migrations when it starts.

`TRAVELER_ADMIN_BACKEND_BASE_URL` is the address of the admin backend (default `http://admin-backend:8080`).

## Endpoints

- `/actuator/health`: health, including the database
- `/v3/api-docs`: OpenAPI description
- `/swagger-ui.html`: browsable OpenAPI description
