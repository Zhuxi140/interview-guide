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
- Add concise block-level comments inside generated method bodies before major steps such as validation, strategy selection, persistence, external calls, and compensation. Do not satisfy this rule only with a comment above the method, and do not narrate every line. Every abstract interface method must have Javadoc containing a description, every `@param`, and `@return` for non-void methods.
- Annotate every newly created Req and VO with Swagger `@Schema` at both class and exposed-field/record-component level. Include examples when they help clarify request or response values.

## Service, Conversion & SQL Rules (from `规范.md`)

### 1. 查询（Service 层）
- 简单查询：一律使用 **LambdaQuery**。
- 查询 `isDeleted = true` 时，LambdaQuery 无法自动追加，**必须手写 SQL**（在 Mapper 或 XML 中）。

### 2. 更新（Service 层）
- **全量/半量更新**：使用实体类（自动填充生效）。
- **非实体类更新**：使用 **LambdaUpdate**，注意：
  - 自动追加 `isDeleted = false`（无需手写）。
  - 自动填充字段（如 `UpdateBy`、`traceId`）**不会生效**，必须显式 `.set()`。

### 3. VO / BO / 转换
- **普通查询接口**：允许直接用 VO 接收和返回。
- **非查询接口**（如增删改）且返回的 VO 与 Service 操作结果字段不一致：
  - Service 层必须返回 **BO**。
  - Controller 使用 **MapStruct** 转换 BO → VO 后返回。

### 4. Mapper 层手写 SQL
- 复杂 SQL **不允许**使用 `@Select`、`@Update` 等注解，必须写在对应 **XML** 中。
- 手写 SQL 时，**实体类上所有 MyBatis-Plus 自动填充/注解（如主键、`createAt`、`isDeleted` 等）均失效**，需自行处理。

### 5. 总结速记
| 场景 | 工具 | 注意 |
|------|------|------|
| 简单查询 | LambdaQuery | 查 `isDeleted=true` 需手写 SQL |
| 更新（实体类） | 实体对象 | 自动填充生效 |
| 更新（非实体） | LambdaUpdate | 自动填充失效，需显式 set；自动追加 `isDeleted=false` |
| 非查询接口返回 | BO + MapStruct | VO 与 BO 不一致时强制转换 |
| 复杂手写 SQL | XML | 所有自动注解失效，自行处理 |
| 三表及以上互联 | 禁止 | 拆为"两表 JOIN(XML) + 单表 LambdaQuery(Service 层组装)" |
| 批量补展示字段（名称/标题） | 模块内 Service 单表 LambdaQuery | `IN` 批量查询，避免 N+1 |

### 6. 数据互联（JOIN 上限）—— 硬性约束
- 一条 SQL **最多关联两张表**（1 个 JOIN）。禁止三表及以上互联，包括标量子查询跨表引用
  （FROM 只留两表但 `(SELECT ... FROM third_table)` 同样禁止）。
- 需要第三张表的数据（如 `job_title` 等展示字段）时，采用两层组装：
  - 两表 JOIN 写在 **XML**（仅取主表 + 最近邻一张表，带上所需外键）；
  - 缺失字段由关联模块的 Service 提供**单表 LambdaQuery** 批量方法（如 `getJobTitlesByIds`），
    在 Service 层以 Map 按外键合并补齐。
- 单表查询一律 **LambdaQuery**；只有 JOIN 才写 XML。批量补齐必须用 `IN` 查询，禁止 N+1 逐条调用。

## Testing Guidelines

Tests use JUnit Jupiter through `spring-boot-starter-test`, with Spring and Mockito facilities where appropriate. Name test classes `*Test` and mirror the production package. Cover successful behavior, validation failures, authorization boundaries, and persistence edge cases. No coverage threshold is configured; add regression tests for every bug fix.

## Commit & Pull Request Guidelines

History follows Conventional Commit-style subjects, commonly `feat(scope): description` and `fix(scope): description`; keep scopes module- or feature-specific, for example `feat(resume): add parsing status`. Keep commits focused. Pull requests should summarize behavior changes, identify affected modules, link relevant issues or phase documents, list verification commands, and call out schema/configuration changes. Include API examples or screenshots when Swagger-visible behavior changes.
