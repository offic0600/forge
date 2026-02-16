---
name: forge-review
description: AI-powered code review combining multiple Skills
trigger: /forge-review
---

# /forge-review -- Comprehensive Code Review

When the user runs `/forge-review`, perform a thorough, multi-dimensional code review using Forge Foundation skills. This command analyzes code changes, checks them against all loaded conventions and best practices, and produces a structured report with severity-classified findings.

---

## Arguments

Parse the following optional arguments from the user's command. If no arguments are provided, use the defaults.

| Argument | Format | Default | Description |
|---|---|---|---|
| `--staged` | flag (no value) | **Default mode if no args** | Review only staged changes (`git diff --cached`) |
| `--branch` | `--branch <name>` | Current branch vs its merge base with `main` | Review all changes on a branch compared to a base branch. Uses `git diff $(git merge-base main HEAD)..HEAD` |
| `--files` | `--files <glob>` | All changed files | Restrict review to files matching the glob pattern (e.g., `--files "*.kt"` or `--files "src/main/**"`) |
| `--severity` | `--severity <level>` | `all` | Filter output to show only findings at or above this level. Values: `critical`, `warning`, `info`, `all` |
| `--base` | `--base <branch>` | `main` | Override the base branch for `--branch` comparisons |

### Argument Parsing Examples

- `/forge-review` -- Review staged changes (default)
- `/forge-review --staged` -- Explicit staged changes review
- `/forge-review --branch feature/payment-refund` -- Review all changes on the feature branch vs main
- `/forge-review --branch feature/payment-refund --base develop` -- Compare against develop instead of main
- `/forge-review --files "**/*Service.kt"` -- Review only service files in staged changes
- `/forge-review --severity critical` -- Show only critical issues
- `/forge-review --branch feature/new-api --severity warning --files "src/main/**"` -- Combined filters

---

## Step 1: Gather Changes

Based on the parsed arguments, collect the set of changed files and their diffs.

### 1.1 Get the Diff

Execute the appropriate git command based on arguments:

```bash
# Default / --staged:
git diff --cached --name-only    # List of changed files
git diff --cached                # Full diff content

# --branch <name>:
git diff $(git merge-base {base} {branch})..{branch} --name-only
git diff $(git merge-base {base} {branch})..{branch}

# If neither --staged nor --branch, and there are no staged changes:
# Fall back to comparing HEAD~1
git diff HEAD~1 --name-only
git diff HEAD~1
```

### 1.2 Filter Files

If `--files` was specified, filter the changed file list to only include files matching the glob pattern.

### 1.3 Classify Changed Files

Group the changed files by type for targeted review:

| File Pattern | Category | Skills to Load |
|---|---|---|
| `*.java` | Java source | java-conventions, error-handling |
| `*.kt` | Kotlin source | kotlin-conventions, error-handling |
| `*Controller*.java`, `*Controller*.kt` | API layer | api-design, spring-boot-patterns |
| `*Service*.java`, `*Service*.kt` | Service layer | spring-boot-patterns, error-handling |
| `*Repository*.java`, `*Repository*.kt` | Data layer | database-patterns |
| `*Test*.java`, `*Test*.kt`, `*Spec*.kt` | Test files | testing-standards |
| `*.sql`, `V*__*.sql` | Database migrations | database-patterns |
| `build.gradle.kts`, `build.gradle` | Build config | gradle-build |
| `*.yml`, `*.yaml` (in resources) | App config | spring-boot-patterns, security-practices |
| `*Config*.java`, `*Config*.kt` | Configuration | spring-boot-patterns, security-practices |
| `*Security*`, `*Auth*` | Security | security-practices |
| `*.ts`, `*.tsx`, `*.js`, `*.jsx` | Frontend | (basic review only) |

### 1.4 Handle Edge Cases

- **No changes found**: If `git diff` returns empty, report: "No changes detected. Use `--branch <name>` to review a branch, or stage changes with `git add` first." Stop execution.
- **Binary files**: Skip binary files from review. Note them in the report under a "Skipped Files" section.
- **Very large diffs** (over 500 changed files): Warn the user that the review may be slow. Suggest using `--files` to narrow scope.

---

## Step 2: Load Relevant Skills

Based on the file classification from Step 1.3, read the appropriate SKILL.md files to load review criteria. Each skill file contains the conventions and patterns to check against.

### 2.1 Skill Loading Table

Load skills from `plugins/forge-foundation/skills/`. Read each SKILL.md file fully to understand the rules.

| Skill | Path | When to Load |
|---|---|---|
| Java Conventions | `plugins/forge-foundation/skills/java-conventions/SKILL.md` | Any `.java` files changed |
| Kotlin Conventions | `plugins/forge-foundation/skills/kotlin-conventions/SKILL.md` | Any `.kt` files changed |
| Spring Boot Patterns | `plugins/forge-foundation/skills/spring-boot-patterns/SKILL.md` | Any Spring annotations detected in changed files |
| Testing Standards | `plugins/forge-foundation/skills/testing-standards/SKILL.md` | Any test files changed OR new non-test source files without corresponding tests |
| API Design | `plugins/forge-foundation/skills/api-design/SKILL.md` | Any controller/endpoint files changed |
| Database Patterns | `plugins/forge-foundation/skills/database-patterns/SKILL.md` | Any repository, entity, or migration files changed |
| Error Handling | `plugins/forge-foundation/skills/error-handling/SKILL.md` | Any service or controller files changed |
| Logging & Observability | `plugins/forge-foundation/skills/logging-observability/SKILL.md` | Any files with logging statements changed |
| Security Practices | `plugins/forge-foundation/skills/security-practices/SKILL.md` | **Always loaded** (security review is mandatory) |
| Gradle Build | `plugins/forge-foundation/skills/gradle-build/SKILL.md` | Any `build.gradle.kts` or `settings.gradle.kts` changed |

### 2.2 Loading Process

For each skill that needs to be loaded:

1. Read the full SKILL.md file content.
2. Extract the rules, patterns, and anti-patterns defined in the skill.
3. Apply those rules to the relevant changed files during the review steps that follow.

---

## Step 3: Security Review (CRITICAL)

This step runs on ALL changed files regardless of type. Load `plugins/forge-foundation/skills/security-practices/SKILL.md` and check for the following patterns. Every finding in this category is severity **CRITICAL** unless noted otherwise.

### 3.1 Hardcoded Credentials

Search all changed files for patterns that indicate hardcoded secrets:

```
# Patterns to detect (case-insensitive):
password\s*=\s*["'][^"']+["']
secret\s*=\s*["'][^"']+["']
api[_-]?key\s*=\s*["'][^"']+["']
token\s*=\s*["'][^"']+["']
private[_-]?key\s*=\s*["']
-----BEGIN (RSA |EC |DSA )?PRIVATE KEY-----
jdbc:.*password=
Basic [A-Za-z0-9+/=]{20,}
Bearer [A-Za-z0-9._-]{20,}
```

**Exclude** from detection:
- Test files using obviously fake values (e.g., `"test-password"`, `"dummy-key"`)
- Configuration property placeholders (e.g., `${DB_PASSWORD}`, `"${API_KEY}"`)
- Comments explaining what NOT to do

### 3.2 SQL Injection Vulnerabilities

Search for string concatenation or interpolation in SQL contexts:

```kotlin
// CRITICAL: String concatenation in queries
"SELECT.*" + variable
"SELECT.*$variable"
"INSERT.*" + variable
"UPDATE.*" + variable
"DELETE.*" + variable
```

**Safe patterns to ignore**: `@Query` annotations with `:paramName` syntax, JPA Criteria API, Spring Data derived queries.

### 3.3 Missing Input Validation

For any new REST endpoint (method annotated with `@PostMapping`, `@PutMapping`, `@PatchMapping`):
- Check that request body parameters have `@Valid` annotation
- Check that the request DTO class has validation annotations (`@NotBlank`, `@NotNull`, `@Size`, etc.)

### 3.4 Insecure Configuration

Check for:
- CORS configured with `allowedOrigins("*")` -- CRITICAL
- CSRF disabled without comment explaining why -- WARNING
- Security endpoints with `.permitAll()` on non-health-check paths -- WARNING
- Debug/dev profiles enabled in production configs -- CRITICAL
- Actuator endpoints exposed without authentication -- WARNING

### 3.5 Sensitive Data Exposure

Check for:
- Logging statements that might include passwords, tokens, or PII: `logger.info(".*password.*")`, `logger.debug(".*token.*")`
- `toString()` methods on entities containing sensitive fields
- API responses returning internal IDs, stack traces, or full entity objects

---

## Step 4: Convention Review (WARNING)

Apply the loaded convention skills to check code style and patterns.

### 4.1 Java/Kotlin Convention Checks

From `java-conventions` and `kotlin-conventions` skills:

| Check | Rule | Severity |
|---|---|---|
| Package naming | Must follow `com.{org}.{domain}.{layer}` | WARNING |
| Class naming suffix | Controllers end with `Controller`, services with `Service`, etc. | WARNING |
| Method ordering | Static fields, instance fields, constructor, public, private | INFO |
| Null handling | No `null` returns from public methods; use `Optional<T>` (Java) or nullable types (Kotlin) | WARNING |
| Import ordering | `java.*`, `javax.*`, `org.*`, `com.*`, static imports last | INFO |
| No wildcard imports | `import java.util.*` is not allowed | WARNING |
| Kotlin `!!` operator | No `!!` in production code (allowed in tests) | WARNING |
| Data class misuse | Data classes with >7 fields or mutable state | INFO |
| Scope function misuse | Nested scope functions, wrong scope function choice | INFO |

### 4.2 Spring Boot Pattern Checks

From `spring-boot-patterns` skill:

| Check | Rule | Severity |
|---|---|---|
| Layer violation | Controller accessing Repository directly | CRITICAL |
| Service importing HTTP classes | Service using `HttpServletRequest`, `ResponseEntity` | WARNING |
| Field injection | `@Autowired` on fields instead of constructor injection | WARNING |
| `@Value` usage | Using `@Value` instead of `@ConfigurationProperties` | INFO |
| Missing `@Transactional` | Service methods modifying data without `@Transactional` | WARNING |
| `@Transactional` on wrong layer | `@Transactional` on Controller or Repository | WARNING |

### 4.3 API Design Checks

From `api-design` skill:

| Check | Rule | Severity |
|---|---|---|
| URL naming | Must be lowercase, hyphenated, plural nouns | WARNING |
| Verbs in URLs | Endpoints like `/createOrder` instead of `POST /orders` | WARNING |
| Wrong status codes | `POST` returning 200 instead of 201, missing Location header | WARNING |
| Missing error format | Error responses not using RFC 7807 `ProblemDetail` | INFO |
| Missing pagination | List endpoints returning unbounded results | WARNING |

### 4.4 Database Pattern Checks

From `database-patterns` skill:

| Check | Rule | Severity |
|---|---|---|
| N+1 queries | `@OneToMany` without `@EntityGraph`, `JOIN FETCH`, or `@BatchSize` | WARNING |
| Missing audit columns | New tables without `created_at`, `updated_at` | WARNING |
| Modifying existing migration | Changes to applied Flyway migration files | CRITICAL |
| Schema naming | Non-snake_case table or column names | WARNING |
| Transaction in wrong layer | `@Transactional` on Repository or Controller | WARNING |

### 4.5 Error Handling Checks

From `error-handling` skill:

| Check | Rule | Severity |
|---|---|---|
| Catching generic Exception | `catch (e: Exception)` or `catch (Exception e)` outside of `@ControllerAdvice` | WARNING |
| Empty catch blocks | `catch (e: ...) { }` with no handling | CRITICAL |
| Swallowing exceptions | Catching and only logging without rethrowing or handling | WARNING |
| Missing correlation ID | Error responses without `correlationId` | INFO |
| Retrying non-idempotent | Retry on POST/PUT without idempotency key | WARNING |

---

## Step 5: Test Coverage Review (WARNING)

Load `plugins/forge-foundation/skills/testing-standards/SKILL.md` and check test adequacy.

### 5.1 Missing Tests

For each new or modified **non-test** source file, check if a corresponding test file exists:

```
# Source file -> Expected test file mapping:
src/main/kotlin/com/forge/order/service/OrderService.kt
  -> src/test/kotlin/com/forge/order/service/OrderServiceTest.kt

src/main/java/com/forge/payment/PaymentController.java
  -> src/test/java/com/forge/payment/PaymentControllerTest.java
```

- If a new public method is added to a class and no corresponding test exists, report as **WARNING**: "No test found for new method `{ClassName}.{methodName}()`"
- If an entirely new class is added with no test file, report as **WARNING**: "No test file found for new class `{ClassName}`"

### 5.2 Test Quality Checks

For changed test files, check:

| Check | Rule | Severity |
|---|---|---|
| Naming convention | Test methods must follow `should_expectedBehavior_when_condition` pattern | WARNING |
| AAA structure | Tests should have clear Arrange/Act/Assert sections (look for comments or structural separation) | INFO |
| Mocking the SUT | Mocking the class under test instead of its dependencies | WARNING |
| No assertions | Test methods without any `assert*`, `verify`, or `assertThat` calls | CRITICAL |
| Testing implementation | Tests that only verify mock interactions without asserting behavior | INFO |

---

## Step 6: Architecture Review (WARNING/CRITICAL)

Check for architecture-level violations across the changed files.

### 6.1 Dependency Direction

Verify that dependencies flow in the correct direction:

```
Controller -> Service -> Repository -> Entity
    |                                     ^
    +--------- NEVER directly -----------+
```

- Controller importing from repository package: **CRITICAL**
- Service importing from controller package: **CRITICAL**
- Repository containing business logic (conditional logic beyond simple queries): **WARNING**

### 6.2 Cross-Module Dependencies

For multi-module projects, verify that changed files respect module boundaries:

- Read `settings.gradle.kts` to understand module structure.
- Check that no module depends on another module's internal packages (only public API).
- Flag circular dependencies between modules: **CRITICAL**

### 6.3 API Contract Consistency

If any controller/endpoint files changed:

- Check if an OpenAPI spec exists (`openapi.yaml`, `openapi.json`, `swagger.yaml`).
- If it exists, verify that the changed endpoints still match the spec.
- If endpoints changed but the spec was not updated: **WARNING**: "API endpoint changed but OpenAPI spec was not updated."

---

## Step 7: Run Baseline Scripts

If baseline scripts exist in the project, execute them and include results in the review.

### 7.1 Discover Baselines

Check for baseline scripts in these locations:

```
baselines/
scripts/baselines/
plugins/forge-foundation/baselines/
```

### 7.2 Execute Baselines

For each discovered baseline script:

```bash
# Run the baseline and capture output
bash baselines/{script-name}.sh
```

Report the exit code and output:
- Exit code 0: **PASS**
- Exit code non-zero: **FAIL** -- include the script output in the review report

### 7.3 Built-in Checks (if no baseline scripts found)

If no baseline scripts are found, run these built-in checks:

```bash
# Compile check (Gradle)
./gradlew compileKotlin compileJava 2>&1

# Static analysis (if configured)
./gradlew detekt 2>&1      # Kotlin static analysis
./gradlew checkstyle 2>&1  # Java static analysis

# Unit tests
./gradlew test 2>&1
```

Only run commands for tools that are configured in the build files. Do not fail if a tool is not configured.

---

## Step 8: Generate Review Report

Compile all findings into a structured Markdown report. Apply the `--severity` filter if specified.

### 8.1 Report Template

Output the following report format:

```markdown
## Forge Code Review Report

**Reviewed at**: {TIMESTAMP}
**Branch**: {BRANCH_NAME}
**Base**: {BASE_BRANCH}
**Diff scope**: {--staged | --branch | HEAD~1}
**Files reviewed**: {COUNT}
**Skills loaded**: {LIST_OF_LOADED_SKILLS}

---

### Summary

| Severity | Count |
|----------|-------|
| CRITICAL | {N}   |
| WARNING  | {N}   |
| INFO     | {N}   |
| **Total** | **{N}** |

{IF CRITICAL > 0}
> **MERGE BLOCKED**: {N} critical issue(s) must be resolved before merge.
{ENDIF}

{IF CRITICAL == 0 AND WARNING > 0}
> **REVIEW RECOMMENDED**: No critical issues, but {N} warning(s) should be addressed.
{ENDIF}

{IF CRITICAL == 0 AND WARNING == 0}
> **LOOKS GOOD**: No critical issues or warnings found. Ship it!
{ENDIF}

---

### Critical Issues

These issues MUST be fixed before merging.

#### [SECURITY] Hardcoded API key
- **File**: `src/main/kotlin/com/forge/payment/PaymentConfig.kt:15`
- **Code**: `val apiKey = "sk-live-abc123def456"`
- **Rule**: security-practices / Secret Management
- **Fix**: Use environment variable: `val apiKey = System.getenv("PAYMENT_API_KEY")`

#### [SQL_INJECTION] String concatenation in SQL query
- **File**: `src/main/kotlin/com/forge/user/UserRepository.kt:42`
- **Code**: `"SELECT * FROM users WHERE name = '$name'"`
- **Rule**: security-practices / Parameterized Queries
- **Fix**: Use parameterized query with `@Query` and `:paramName`

---

### Warnings

These should be addressed but do not block the merge.

#### [CONVENTION] Test naming does not follow standard
- **File**: `src/test/kotlin/com/forge/order/OrderServiceTest.kt:30`
- **Code**: `fun testCreateOrder() { ... }`
- **Rule**: testing-standards / Naming Convention
- **Fix**: Rename to `should_createOrder_when_validRequest()`

#### [COVERAGE] No tests for new method
- **File**: `src/main/kotlin/com/forge/refund/RefundService.kt:55`
- **Code**: `fun cancelRefund(refundId: UUID): Refund`
- **Rule**: testing-standards / Coverage Targets
- **Fix**: Add test in `RefundServiceTest.kt` covering this method

#### [PATTERN] Field injection detected
- **File**: `src/main/kotlin/com/forge/notification/NotificationService.kt:12`
- **Code**: `@Autowired private lateinit var emailClient: EmailClient`
- **Rule**: spring-boot-patterns / Constructor Injection
- **Fix**: Convert to constructor injection: `class NotificationService(private val emailClient: EmailClient)`

---

### Info / Suggestions

Optional improvements that would enhance code quality.

#### [IMPROVEMENT] Consider @ConfigurationProperties
- **File**: `src/main/kotlin/com/forge/gateway/GatewayConfig.kt:20`
- **Code**: `@Value("\${gateway.timeout}") private lateinit var timeout: String`
- **Rule**: spring-boot-patterns / Configuration Management
- **Suggestion**: Use `@ConfigurationProperties(prefix = "gateway")` with a typed data class for type safety and validation

#### [STYLE] Import ordering
- **File**: `src/main/kotlin/com/forge/order/OrderController.kt:3-8`
- **Rule**: java-conventions / Import Ordering
- **Suggestion**: Reorder imports: `java.*` first, then `javax.*`, `org.*`, `com.*`, static imports last

---

### Baseline Results

| Baseline | Status | Details |
|----------|--------|---------|
| compile check | PASS | Clean compilation |
| unit tests | PASS | 142 tests passed |
| detekt | FAIL | 3 findings (see below) |

**detekt output**:
```
src/main/kotlin/com/forge/order/OrderService.kt:45 - LongMethod: ...
```

---

### Skipped Files

| File | Reason |
|------|--------|
| `assets/logo.png` | Binary file |
| `docs/generated-api.html` | Generated file |

---

### Files Reviewed

| File | Status | Findings |
|------|--------|----------|
| `src/main/kotlin/.../OrderService.kt` | Modified | 1 WARNING |
| `src/main/kotlin/.../PaymentConfig.kt` | Added | 1 CRITICAL |
| `src/test/kotlin/.../OrderServiceTest.kt` | Modified | 1 WARNING |
| `src/main/kotlin/.../RefundService.kt` | Added | 1 WARNING (no tests) |
```

### 8.2 Report Output Rules

1. **Sort findings by severity**: CRITICAL first, then WARNING, then INFO.
2. **Include file path and line number** for every finding.
3. **Include the offending code snippet** (the specific line or block).
4. **Include the rule reference** (which skill and which specific rule was violated).
5. **Include a concrete fix suggestion** whenever possible -- do not just say "fix this", show exactly how to fix it.
6. **Apply severity filter**: If `--severity critical` was specified, only show CRITICAL findings. If `--severity warning`, show CRITICAL and WARNING. If `--severity info` or `all` (default), show everything.
7. **Merge verdict**: The top-level summary must clearly state whether the changes are safe to merge.

---

## Error Handling

Handle the following error conditions gracefully:

| Error Condition | Action |
|---|---|
| `git` not installed or not in PATH | Report ERROR: "Git is required for /forge-review. Please install git and try again." Stop execution. |
| Not inside a git repository | Report ERROR: "Not a git repository. Run `git init` or navigate to a git repository." Stop execution. |
| `git diff` returns error | Report the git error message verbatim. Suggest common fixes (e.g., "Did you mean to specify `--branch`?"). Stop execution. |
| No changes detected | Report INFO: "No changes detected." Suggest using `--branch <name>` to review a branch or `git add` to stage changes. Stop execution. |
| Skill file not found | Report WARN: "Skill file `{path}` not found. Skipping {skill-name} checks." Continue with other skills. |
| Baseline script fails to execute | Report WARN: "Baseline script `{name}` failed to execute: {error}." Include in report but continue review. |
| Branch not found | Report ERROR: "Branch `{name}` not found. Available branches: {list}." Stop execution. |
| Too many files (>500) | Report WARN: "Large diff ({N} files). Review may be slow. Consider using `--files` to narrow scope." Continue but suggest narrowing. |
| File in diff was deleted | Skip deleted files from convention checks. Note in report: "{file} was deleted." |

---

## Performance Notes

- Load skill files only once per review session, not once per file.
- Process files in parallel where possible (security scan all files simultaneously).
- For very large reviews, prioritize CRITICAL checks first so the user gets immediate feedback on blocking issues.
- Skip generated files (files in `build/`, `target/`, `node_modules/`, `dist/`, `.gradle/`, files matching `**/generated/**`).
