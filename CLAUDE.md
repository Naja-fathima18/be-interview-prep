# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Java backend service built with **Spring Boot 3.x on Java 21**, built with **Maven** (always via the wrapper).
Base package: `com.example.beinterviewprep` (update this line if the real base package differs).

## Commands

Always use the Maven wrapper (`./mvnw` in Bash, `mvnw.cmd` in PowerShell) — never a global `mvn`.

| Task | Command |
|---|---|
| Compile main + test sources | `./mvnw -q -B test-compile` |
| Run all unit tests | `./mvnw -B test` |
| Run one test class | `./mvnw -B test -Dtest=OrderServiceTest` |
| Run one test method | `./mvnw -B test -Dtest='OrderServiceTest#createsOrder'` |
| Unit + integration tests | `./mvnw -B verify` |
| Format code | `./mvnw -q spotless:apply` |
| Run the app locally | `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` |

Prefer the narrowest command that proves a change: a single test class before the full suite.
Integration tests (`*IT.java`) use Testcontainers and need Docker running.

## Architecture

Package-by-feature, layered inside each feature:

```
com.example.beinterviewprep
├── <feature>/                 e.g. order/, customer/
│   ├── api/                   @RestController + request/response DTOs (records)
│   ├── domain/                JPA entities, domain logic, enums
│   ├── service/               @Service classes — transactions live here
│   └── persistence/           Spring Data repositories
├── common/                    cross-cutting: error handling, config, utilities
└── Application.java
```

- Dependencies flow `api → service → persistence/domain`. Controllers never touch repositories.
- Features talk to each other through services, never through another feature's repository.

## Conventions

- **Injection**: constructor injection only (`final` fields; Lombok `@RequiredArgsConstructor` is fine). No field `@Autowired`.
- **DTOs**: Java `record`s with Jakarta Bean Validation annotations. Never return JPA entities from controllers.
- **Validation**: `@Valid` on `@RequestBody`; business rule violations throw domain exceptions.
- **Errors**: one `@RestControllerAdvice` in `common/` maps exceptions to RFC 7807 `ProblemDetail`. Don't build ad-hoc error bodies in controllers.
- **Transactions**: `@Transactional` on service methods (use `readOnly = true` for queries). Never on controllers.
- **JPA**: default to `FetchType.LAZY`; avoid N+1 with `@EntityGraph` or fetch joins; no `open-in-view` reliance (`spring.jpa.open-in-view=false`).
- **Config**: typed `@ConfigurationProperties` records, not scattered `@Value`.
- **Logging**: SLF4J, parameterized messages (`log.info("Created order {}", id)`); never log secrets or full PII.
- **Nulls**: return `Optional` from lookups; don't pass `Optional` as a parameter.
- **No comments**: don't add comments to Java code — no `//`, no `/* */`, no Javadoc. Make intent clear through naming, small methods and tests. Leave existing comments as they are unless they're now wrong. A hook enforces this.
- **Formatting**: Spotless with google-java-format; a hook runs it automatically on edited `.java` files.

## Database

- Schema is managed by **Flyway**: `src/main/resources/db/migration/V<n>__<description>.sql`.
- **Never edit a migration that is already committed** — add a new one. (A hook enforces this.)
- `spring.jpa.hibernate.ddl-auto` must stay `validate` (or `none`) outside tests.

## Testing

- JUnit 5 + AssertJ + Mockito. Test names describe behaviour: `rejectsOrderWhenStockIsInsufficient()`.
- Slice tests first: `@WebMvcTest` for controllers, `@DataJpaTest` (+ Testcontainers Postgres) for repositories, plain unit tests for services.
- Full `@SpringBootTest` only for end-to-end flows; those are `*IT.java` and run under `verify` (Failsafe).
- Every bug fix comes with a test that fails without the fix.

## Don'ts

- Don't add dependencies to `pom.xml` without saying why in your summary.
- Don't read or write `.env*`, keystores, or anything under `secrets/`.
- Don't disable or `@Disabled` failing tests to get green — fix them or report them.
- Don't run `git push` or `./mvnw deploy` unless explicitly asked.

## Claude Code harness

This repo ships a harness in `.claude/`:

- **Hooks** (`.claude/hooks/`, Node scripts — run on Windows/macOS/Linux):
  - `guard-files.mjs` (PreToolUse) — blocks edits to committed Flyway migrations and secret files.
  - `no-comments.mjs` (PreToolUse) — rejects edits that add new comments to `.java` files.
  - `format-java.mjs` (PostToolUse) — runs Spotless on each edited `.java` file if Spotless is configured.
  - `verify-build.mjs` (Stop) — runs `./mvnw -q -B test-compile` when Java/pom changes are pending; a broken build sends Claude back to fix it.
- **Subagents** (`.claude/agents/`): `senior-java-developer`, `test-writer`.
- **Skills** (`.claude/skills/`): `/new-endpoint`, `/add-migration`, `/run-tests`, `/start-question`, `/review-pr`, `/open-pr`.

## Submission workflow

One branch → one PR → one merge per question, Q1 to Q5 in order: `feature/q1-library`, `feature/q2-expenses`, `feature/q3-file-upload`, `feature/q4-rate-limit`, `feature/q5-booking`.

1. `/start-question q<n> <spec>` — pull `main`, create the branch, save the spec to `.claude/questions/q<n>.md`, design note, implement in small meaningful commits.
2. `/review-pr --fix` — review against the spec, conventions, tests and commit hygiene; fix findings.
3. `/open-pr` — push and open the PR with the template (Problem / Approach / Decisions & trade-offs / How to test); `/open-pr merge` also merges and pulls `main`.

Personal overrides go in `.claude/settings.local.json` (git-ignored).
