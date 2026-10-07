# be-interview-prep

Five Spring Boot features, each delivered as its own pull request.

**Stack:** Java 21, Spring Boot 3.5, Maven (wrapper), Spring Data JPA, Flyway, H2 (in-memory, PostgreSQL mode), JUnit 5 + AssertJ + MockMvc.

## Run

```bash
./mvnw spring-boot:run          # app on http://localhost:8080
```

Windows PowerShell: `mvnw.cmd spring-boot:run`.

## Test

```bash
./mvnw test                     # all tests
./mvnw test -Dtest=ClassName    # one test class
```

No external services are needed: the database is in-memory H2 and its schema is created by the Flyway migrations in `src/main/resources/db/migration`.

## Structure

Package-by-feature under `com.example.beinterviewprep`; each feature has `api/` (controllers and DTO records), `service/` (transactions), `domain/` (entities) and `persistence/` (repositories). `common/error` maps every exception to one RFC 7807 `application/problem+json` error format.

## Questions

| # | Question | PR link |
|---|---|---|
| 1 | Library API | |
| 2 | Expense Tracker | |
| 3 | File Upload Service | |
| 4 | API Rate Limiting | |
| 5 | Appointment Booking | |

Video:
