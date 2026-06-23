# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Kotlin/Spring Boot backend. Application code lives under `src/main/kotlin/cmc/mody`, with `ModyApplication.kt` as the entry point. Current packages include `health` for the health API and `common/api` for shared response and exception types. Runtime configuration is in `src/main/resources/application.yaml`; test configuration is in `src/test/resources/application.yaml`. Tests live under `src/test/kotlin` and should mirror the production package structure. Architecture notes and templates are in `docs/adr`, `docs/lld`, and `docs/templates`.

## Build, Test, and Development Commands

- `./gradlew build`: compile, test, and assemble the application.
- `./gradlew test`: run the JUnit 5 test suite.
- `./gradlew test --tests "cmc.mody.SomeTest"`: run one test class or pattern.
- `./gradlew bootRun`: start the service locally on the configured Spring port.
- `./gradlew ktlintCheck` / `./gradlew ktlintFormat`: check or apply Kotlin formatting.
- `./gradlew detekt`: run static analysis using `config/detekt/detekt.yml`.
- `docker-compose up -d`: run MySQL and the app image when required environment variables are set.

## Coding Style & Naming Conventions

Use Kotlin 2.0.21, Java 21, and Spring Boot 3.4.1 conventions. Keep the root package `cmc.mody`; organize new code by feature package where practical. Follow 4-space indentation, UTF-8, LF endings, final newlines, and a 120-character line limit. Prefer clear Kotlin names: classes in `PascalCase`, functions and properties in `camelCase`, and enum entries in uppercase style. Use `ApiResponse` and existing status types for API responses.

## Testing Guidelines

Use JUnit 5 with Spring Boot test support. Name tests after the unit under test, for example `HealthControllerTest` or `ModyApplicationTests`. Add focused tests for new behavior and keep integration tests explicit about required database setup; H2 is available for tests.

## Commit & Pull Request Guidelines

Recent history follows Conventional Commits, such as `feat(health): ...`, `build: ...`, and `chore(ci): ...`. Keep commits small and scoped. Pull requests should include a concise description, linked issue when applicable, test results, and any API/configuration changes. Update ADR or LLD docs when a change records an architectural decision or detailed design.

## Security & Configuration Tips

Do not commit secrets. Supply database credentials and Docker image settings through environment variables such as `DB_USERNAME`, `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `DB_NAME`, and `DOCKER_REPO`.
