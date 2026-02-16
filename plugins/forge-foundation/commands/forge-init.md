---
name: forge-init
description: Initialize a project with Forge configuration
trigger: /forge-init
---

# /forge-init -- Project Initialization

When the user runs `/forge-init`, perform a full Forge platform initialization for the current project. Follow every step below in order. Do NOT skip steps. If a step fails, report the error clearly and continue to the next step where possible.

---

## Step 1: Detect Project Type and Gather Context

Scan the current working directory to determine the project type, language(s), and build system. Check for the following files **in order of priority**:

### 1.1 Build System Detection

| File to Check | Project Type | Build Command | Test Command |
|---|---|---|---|
| `build.gradle.kts` | Kotlin/Gradle (Kotlin DSL) | `./gradlew build` | `./gradlew test` |
| `build.gradle` | Java/Gradle (Groovy DSL) | `./gradlew build` | `./gradlew test` |
| `pom.xml` | Java/Maven | `./mvnw clean install` | `./mvnw test` |
| `package.json` | Node.js/TypeScript | `npm run build` | `npm test` |
| `Cargo.toml` | Rust | `cargo build` | `cargo test` |
| `go.mod` | Go | `go build ./...` | `go test ./...` |
| `pyproject.toml` or `setup.py` | Python | `pip install -e .` | `pytest` |

If multiple build files exist (e.g., a monorepo with both `build.gradle.kts` and `package.json`), treat it as a **multi-language project** and note all detected types.

### 1.2 Framework Detection

After identifying the build system, look for framework indicators:

| Indicator | Framework |
|---|---|
| `org.springframework.boot` in build file | Spring Boot |
| `io.ktor` in build file | Ktor |
| `next.config.js` or `next.config.ts` | Next.js |
| `angular.json` | Angular |
| `vite.config.ts` | Vite/React |
| `@nestjs/core` in package.json | NestJS |

### 1.3 Module Structure Detection

For multi-module projects, read the following to enumerate modules:

- **Gradle**: Read `settings.gradle.kts` (or `settings.gradle`) and extract all `include(":module-name")` declarations.
- **Maven**: Read root `pom.xml` and extract `<modules>` section.
- **Node.js monorepo**: Check for `workspaces` in `package.json` or `pnpm-workspace.yaml`.

### 1.4 Existing Configuration Detection

Check if Forge has already been initialized by looking for:

- `CLAUDE.md` in project root -- if it exists, **ask the user** whether to overwrite or merge.
- `.mcp.json` in project root -- if it exists, ask whether to overwrite or merge.
- `.claude/` directory -- check for existing commands or settings.

Record all detection results. You will use them in subsequent steps.

---

## Step 2: Generate CLAUDE.md

Create a `CLAUDE.md` file in the **project root directory**. This file is the primary instruction file that Claude Code reads on every conversation. It must be comprehensive and accurate.

### 2.1 Template

Use the following template. Replace all `{PLACEHOLDER}` values with actual detected values from Step 1. Remove any sections that do not apply to the project.

```markdown
# {PROJECT_NAME} -- {SHORT_DESCRIPTION}

## Quick Start

{For each detected build/test command from Step 1, include it here}

```bash
# Build the project
{BUILD_COMMAND}

# Run tests
{TEST_COMMAND}

# Run the application (if applicable)
{RUN_COMMAND}

# Lint / format (if applicable)
{LINT_COMMAND}
```

## Architecture

{PROJECT_NAME} is a {BUILD_SYSTEM} {MONOREPO_OR_SINGLE} project with the following structure:

{For each detected module, list it with a brief description derived from its directory name and build file}
- **{module-name}/**: {Brief description based on directory contents}

## Key Design Decisions

{Scan for any ADR files in docs/adr/ or similar directories. If found, summarize them. If not, leave placeholder notes.}

1. {Decision 1 -- derived from codebase analysis}
2. {Decision 2}

## Language & Conventions

- **Primary Language**: {LANGUAGE} {VERSION if detectable from build files}
- **Framework**: {FRAMEWORK} {VERSION if detectable}
- **Build**: {BUILD_SYSTEM}
- **Testing**: {TEST_FRAMEWORK detected from dependencies}

## Module Dependency Rules

{If multi-module, document which modules can depend on which. Derive from build files.}

## Security Rules

- NEVER hardcode credentials -- use environment variables
- All secrets must be in .env files (which are .gitignored)
- {Any additional security rules detected from existing configs}

## Forge MCP Servers

This project is configured with Forge MCP servers. Available tools:
- **forge-knowledge**: Search wiki, ADRs, runbooks, API docs via `forge-knowledge.*` tools
- **forge-database**: Query databases (read-only) via `forge-database.*` tools
- **forge-service-graph**: Explore service dependencies via `forge-service-graph.*` tools
```

### 2.2 Content Population Rules

When filling in the template:

1. **Project name**: Use the directory name or the `name` field from `settings.gradle.kts`, `pom.xml`, or `package.json`.
2. **Build/test commands**: Use the exact commands detected in Step 1. If a Gradle wrapper (`gradlew`) exists, always use `./gradlew` (not `gradle`). If a Maven wrapper (`mvnw`) exists, use `./mvnw`.
3. **Module descriptions**: Read each module's `build.gradle.kts` or `pom.xml` to infer purpose from dependencies and directory contents. For example, if a module depends on `spring-boot-starter-web`, it is likely a web service module.
4. **Test frameworks**: Look in build dependencies for JUnit 5, MockK, Mockito, AssertJ, Jest, Playwright, pytest, etc.
5. **Design decisions**: Check for `docs/adr/`, `docs/decisions/`, or `ARCHITECTURE.md` files. Summarize any found.

### 2.3 Write the File

Write the populated template to `./CLAUDE.md` in the project root.

If a `CLAUDE.md` already exists and the user chose to merge, read the existing content first and incorporate existing sections that are not covered by the template (e.g., custom notes the user may have added).

---

## Step 3: Generate .mcp.json

Create a `.mcp.json` file in the **project root directory**. This configures Claude Code to connect to Forge MCP servers running in Docker containers.

### 3.1 Exact Content to Generate

Write the following JSON content to `./.mcp.json`:

```json
{
  "mcpServers": {
    "forge-knowledge": {
      "command": "docker",
      "args": ["exec", "-i", "forge-knowledge-mcp", "java", "-jar", "app.jar"],
      "env": {}
    },
    "forge-database": {
      "command": "docker",
      "args": ["exec", "-i", "forge-database-mcp", "java", "-jar", "app.jar"],
      "env": {}
    },
    "forge-service-graph": {
      "command": "docker",
      "args": ["exec", "-i", "forge-service-graph-mcp", "java", "-jar", "app.jar"],
      "env": {}
    }
  }
}
```

### 3.2 Merge Logic

If `.mcp.json` already exists:

1. Read the existing file and parse its JSON content.
2. For each server in the template above, check if a key with the same name already exists.
3. If it exists, **preserve the existing configuration** (the user may have customized URLs or added auth headers).
4. If it does not exist, **add the new server entry**.
5. Write the merged result back.

This ensures existing custom MCP server configurations are not lost.

---

## Step 4: Install Plugin References

Set up the Forge Foundation plugin so that Claude Code can discover skills, commands, and hooks.

### 4.1 Create .claude Directory

If it does not already exist, create the `.claude/` directory in the project root:

```
.claude/
```

### 4.2 Create or Update settings.json

Create or update `.claude/settings.json` to reference the Forge Foundation plugin. If the file already exists, merge the new content with existing content.

The file should contain (at minimum):

```json
{
  "plugins": [
    {
      "name": "forge-foundation",
      "path": "plugins/forge-foundation",
      "enabled": true
    }
  ]
}
```

If `.claude/settings.json` already exists, read it first and add the `forge-foundation` entry to the `plugins` array only if it is not already present.

### 4.3 Verify Plugin Structure

Confirm that the following paths exist relative to the plugin path. Report any missing components:

| Path | Purpose |
|---|---|
| `plugins/forge-foundation/.claude-plugin/plugin.json` | Plugin manifest |
| `plugins/forge-foundation/skills/` | Skill definitions (SKILL.md files) |
| `plugins/forge-foundation/commands/` | Command definitions (this file and others) |
| `plugins/forge-foundation/hooks/hooks.json` | Hook configuration |
| `plugins/forge-foundation/.mcp.json` | Plugin-level MCP config |

For each path that exists, report it as OK. For each missing path, report it as MISSING with a warning.

---

## Step 5: Run Health Checks

Verify that the Forge setup is functional by running the following checks. Report each check as PASS or FAIL.

### 5.1 File Existence Checks

Verify these files were created successfully:

```
[ ] CLAUDE.md exists and is non-empty
[ ] .mcp.json exists and contains valid JSON
[ ] .claude/settings.json exists and contains valid JSON
```

### 5.2 MCP Server Connectivity Checks

For each MCP server defined in `.mcp.json`, attempt to verify the Docker container is accessible:

```bash
docker ps --filter "name=forge-knowledge-mcp" --format "{{.Status}}"
docker ps --filter "name=forge-database-mcp" --format "{{.Status}}"
docker ps --filter "name=forge-service-graph-mcp" --format "{{.Status}}"
```

For each command:
- If the container shows a status like `Up X minutes`, report **PASS**.
- If the container is not found or shows `Exited`, report **WARN** with the message: "MCP server container `{name}` is not running. Start it with: `docker-compose -f infrastructure/docker/docker-compose.yml up -d`"
- If Docker itself is not available, report **WARN**: "Docker is not running. MCP servers will not be available until Docker is started."

### 5.3 Build System Check

Verify the build system works:

```bash
# For Gradle projects:
./gradlew --version

# For Maven projects:
./mvnw --version

# For Node.js projects:
node --version && npm --version
```

Report PASS if the command exits successfully, FAIL otherwise.

### 5.4 Git Repository Check

```bash
git status
```

- If the project is a git repo, report PASS.
- If not, report **INFO**: "This directory is not a git repository. Some Forge commands (like /forge-review) require git. Initialize with `git init`."

---

## Step 6: Generate Initial Codebase Profile

Trigger the codebase-profiler skill to create an initial system profile. This provides Forge with deep knowledge about the project structure.

### 6.1 How to Trigger

Invoke the codebase-profiler skill by following the instructions in `plugins/forge-foundation/skills/codebase-profiler/SKILL.md`. Specifically, perform these profiling actions:

1. **Module Dependency Analysis**: Scan `settings.gradle.kts` / `build.gradle.kts` for module structure. Extract inter-module dependencies from `project(":module")` declarations. Generate a Mermaid dependency graph.

2. **Domain Model Catalog**: Find all `@Entity` classes and `data class` models. Map relationships (`@OneToMany`, `@ManyToOne`, `@ManyToMany`). List field types, constraints, and validations.

3. **API Inventory**: Scan all `@Controller` / `@RestController` classes. Extract endpoints: method, path, request/response types.

4. **Database Relationship Map**: Scan Flyway migrations (if present) in `src/main/resources/db/migration/` for schema structure.

5. **Business Flow Catalog**: Identify service method call chains. Map event-driven flows (listeners, publishers).

### 6.2 Output Location

Save the profile output to `knowledge-base/profiles/{project-name}/` with the following files:

- `overview.md` -- System summary
- `modules.md` -- Module dependency graph
- `domain-model.md` -- Entity/model catalog
- `api-inventory.md` -- API endpoint catalog
- `database-schema.md` -- ER diagram + schema docs
- `business-flows.md` -- Key flow sequence diagrams

If the `knowledge-base/profiles/` directory does not exist, create it.

### 6.3 Minimal Profile for Small Projects

If the project is small (fewer than 5 source files or no recognizable framework), generate only the `overview.md` with a basic summary and skip the detailed profiling sub-steps.

---

## Step 7: Present Results to User

After all steps complete, present a summary report to the user in the following format:

```
=== Forge Initialization Complete ===

Project: {PROJECT_NAME}
Type: {PROJECT_TYPE} ({FRAMEWORK})
Modules: {NUMBER_OF_MODULES} detected

Files created/updated:
  [CREATED] CLAUDE.md
  [CREATED] .mcp.json
  [CREATED] .claude/settings.json
  [CREATED] knowledge-base/profiles/{project-name}/overview.md

Health Checks:
  [PASS] CLAUDE.md exists and valid
  [PASS] .mcp.json exists and valid JSON
  [PASS] .claude/settings.json exists and valid JSON
  [WARN] MCP server forge-database-mcp is not running
  [PASS] Build system (Gradle 8.x) detected
  [PASS] Git repository detected

MCP Servers:
  forge-knowledge  -> docker exec forge-knowledge-mcp
  forge-database   -> docker exec forge-database-mcp
  forge-service-graph -> docker exec forge-service-graph-mcp

Loaded Skills (15):
  java-conventions, kotlin-conventions, spring-boot-patterns,
  gradle-build, testing-standards, api-design, database-patterns,
  error-handling, logging-observability, security-practices,
  codebase-profiler, convention-miner, domain-payment,
  domain-order, domain-inventory

Next Steps:
  1. Start MCP servers: docker-compose -f infrastructure/docker/docker-compose.yml up -d
  2. Run /forge-review to review current code
  3. Run /forge-profile development to activate development skills
  4. Ask questions -- Forge skills and MCP tools are now available
```

Adjust the report based on actual results. Replace all placeholders with real values. Show PASS/FAIL/WARN/INFO with the appropriate status for each health check. List only the files that were actually created or updated.

---

## Error Handling

| Error Condition | Action |
|---|---|
| Cannot detect project type (no build files found) | Warn the user: "No recognized build system found. CLAUDE.md will be created with minimal content. Please update manually." Create a minimal CLAUDE.md with just the project name and a placeholder structure. |
| CLAUDE.md already exists and user says no to overwrite | Skip CLAUDE.md generation. Continue with other steps. |
| .mcp.json already exists and user says no to overwrite | Skip .mcp.json generation. Continue with other steps. |
| Docker not installed | Report WARN for all MCP health checks. Do not fail the entire initialization. |
| Not a git repository | Report INFO. Continue with all steps except git-dependent ones. |
| Plugin directory does not exist | Report ERROR: "Forge Foundation plugin not found at expected path. The plugin may need to be installed first." |
| File write permission denied | Report ERROR with the exact file path and suggest the user check permissions. |
