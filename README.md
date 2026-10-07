# be-interview-prep

Five Spring Boot features, each delivered as its own pull request.

**Stack:** Java 21, Spring Boot 3.5, Maven (wrapper), Spring Data JPA, Flyway, PostgreSQL 16 (Docker), Testcontainers, JUnit 5 + AssertJ + MockMvc.

Requires Docker running (Docker Desktop on Windows/macOS).

## Run

```bash
./mvnw spring-boot:run          # starts Postgres from compose.yaml, then the app on http://localhost:8080
```

Windows PowerShell: `mvnw.cmd spring-boot:run`. Spring Boot's Docker Compose support starts the `postgres` service from `compose.yaml` automatically (or run `docker compose up -d` yourself). Override the connection with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Test

```bash
./mvnw test                     # all tests
./mvnw test -Dtest=ClassName    # one test class
```

Tests run against a real PostgreSQL 16 container started by Testcontainers (`src/test/resources/config/application.yml` uses the `jdbc:tc:` URL), so Docker must be running. The schema is created by the Flyway migrations in `src/main/resources/db/migration`.

## Structure

Package-by-feature under `com.example.beinterviewprep`; each feature has `api/` (controllers and DTO records), `service/` (transactions), `domain/` (entities) and `persistence/` (repositories). `common/error` maps every exception to one RFC 7807 `application/problem+json` error format.

## Questions

| # | Question | PR link |
|---|---|---|
| 1 | Library API | https://github.com/Naja-fathima18/be-interview-prep/pull/1 |
| 2 | Expense Tracker | https://github.com/Naja-fathima18/be-interview-prep/pull/2 |
| 3 | File Upload Service | https://github.com/Naja-fathima18/be-interview-prep/pull/3 |
| 4 | API Rate Limiting | https://github.com/Naja-fathima18/be-interview-prep/pull/4 |
| 5 | Appointment Booking | https://github.com/Naja-fathima18/be-interview-prep/pull/5 |

Video:
