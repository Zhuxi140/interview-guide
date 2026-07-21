# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21, Spring Boot 4 Maven reactor. Run development commands from `interview-guide/`. The root `pom.xml` aggregates `bootstrap` (application entry point and runtime configuration), `common` (shared constants, annotations, exceptions, and DTO support), `api` (cross-module interfaces), `framework` (security and Spring configuration), and `modules/` (domain features). Domain modules include `system`, `infra`, `bportal`, `cportal`, `admin`, `aicore`, `engine`, and `Spark`.

Follow the standard Maven layout: production code in `src/main/java`, mapper XML and configuration in `src/main/resources`, and tests in `src/test/java`. Repository-level architecture documents live in `../docs/`; phased API, schema, and permission SQL specifications live in the adjacent Chinese-named phase directories.

## Build, Test, and Development Commands

- `mvn clean verify` - compile every module and run the full test suite.
- `mvn test` - run unit and Spring integration tests without packaging.
- `mvn -pl modules/system -am test` - test one module plus its reactor dependencies.
- `mvn -pl bootstrap -am install -DskipTests` - install the application and its reactor dependencies locally.
- `mvn -f bootstrap/pom.xml spring-boot:run` - start the service locally on port 8080 after installing dependencies.

Local startup expects PostgreSQL, Redis, and S3-compatible RustFS settings matching `bootstrap/src/main/resources/application.yml`. Keep machine-specific credentials outside committed configuration.

## Coding Style & Naming Conventions

Use four-space indentation and UTF-8. Keep packages lowercase under `interview.*`; use PascalCase for types and camelCase for methods and fields. Retain established suffixes such as `Controller`, `Service`, `ServiceImpl`, `Mapper`, `Req`, `VO`, `BO`, and `Entity`. Keep controllers thin, business rules in services, persistence in mapper interfaces/XML, and shared contracts in `api`. Lombok and MapStruct are configured through Maven; no automatic formatter or linter is enforced, so match nearby code and organize imports before committing.

- Define Redis key prefixes, complete keys, and hash field names in a shared constant class such as `AuthKeyConstant`; do not keep business Redis keys in service implementations.
- Build database conditions with MyBatis-Plus `lambdaQuery` / `lambdaUpdate` and entity method references. Do not use string column names in `QueryWrapper` or `UpdateWrapper`.
- Add concise block-level comments inside generated method bodies before major steps such as validation, strategy selection, persistence, external calls, and compensation. Do not satisfy this rule only with a comment above the method, and do not narrate every line. Every abstract interface method must have Javadoc containing a description, every `@param`, and `@return` for non-void methods.
- Annotate every newly created Req and VO with Swagger `@Schema` at both class and exposed-field/record-component level. Include examples when they help clarify request or response values.
- For partial or full updates involving several fields, populate an entity and call the entity-based update so automatic fill annotations apply. For updates of only one or two fields, use `lambdaUpdate` and explicitly set `traceId`, `updatedAt`, `updatedBy`, and other required audit fields.

## Testing Guidelines

Tests use JUnit Jupiter through `spring-boot-starter-test`, with Spring and Mockito facilities where appropriate. Name test classes `*Test` and mirror the production package. Cover successful behavior, validation failures, authorization boundaries, and persistence edge cases. No coverage threshold is configured; add regression tests for every bug fix.

## Commit & Pull Request Guidelines

History follows Conventional Commit-style subjects, commonly `feat(scope): description` and `fix(scope): description`; keep scopes module- or feature-specific, for example `feat(resume): add parsing status`. Keep commits focused. Pull requests should summarize behavior changes, identify affected modules, link relevant issues or phase documents, list verification commands, and call out schema/configuration changes. Include API examples or screenshots when Swagger-visible behavior changes.
