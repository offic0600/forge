# Forge — AI-Driven Intelligent Software Delivery Platform

<div align="center">

**Not an AI coding assistant — an AI-driven software delivery platform with autonomous execution capabilities.**

[![License](https://img.shields.io/badge/license-Source%20Available-lightgrey.svg)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-21-orange.svg)](https://adoptium.net/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org/)
[![Next.js](https://img.shields.io/badge/Next.js-15-black.svg)](https://nextjs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green.svg)](https://spring.io/projects/spring-boot)

</div>

---

## 📖 Overview

Forge is an intelligent delivery platform that deeply embeds AI agents into the entire software development lifecycle. It uses a **SuperAgent architecture** — a single agent dynamically switching between 6 Skill Profiles to cover the complete delivery chain from planning → design → development → testing → operations → evaluation.

### Core Philosophy

| Principle | Description |
|-----------|-------------|
| **SuperAgent over Multi-Agent** | One agent dynamically switches roles via Skills, avoiding multi-agent coordination complexity |
| **Skills over Prompts** | Professional knowledge encoded into 32 reusable, composable Skill assets |
| **Baselines guarantee quality floor** | Quality baseline scripts must pass regardless of model capability changes |
| **Dual-loop drives continuous improvement** | Delivery Loop solves "what to do", Learning Loop solves "getting better" |
| **Human-In-The-Loop (HITL)** | Critical decisions require human approval, not a fully automated black box |

---

## ✨ Key Features

### 🤖 SuperAgent Autonomous Execution Engine

Users declare intent; SuperAgent autonomously completes the full cycle of planning → coding → verification → delivery:

- **50-turn autonomous execution**: No step-by-step instructions needed — the agent decides tool calls, file operations, and code generation independently
- **OODA Loop**: Observe → Orient → Decide → Act, with real-time visualization of each phase
- **18 MCP tools**: File operations, knowledge search, database queries, code compilation, test execution, and more
- **Self-repair on failure**: When baseline checks fail, automatically analyzes the cause, modifies code, and re-validates (up to 2 rounds)
- **Self-managed context**: Automatically compresses message history when approaching token limits, without interrupting execution

### 🧠 6 Skill Profiles with Intelligent Routing

| Profile | Responsibility | Typical Skills |
|---------|---------------|----------------|
| **planning** | Requirements analysis, PRD writing | requirement-engineering, delivery-methodology |
| **design** | Architecture design, ADR writing | api-design, database-patterns |
| **development** | Code implementation, code generation | kotlin-conventions, spring-boot-patterns |
| **testing** | Test strategy, test case writing | testing-standards |
| **ops** | Deployment, operations, troubleshooting | logging-observability, deployment-readiness-check |
| **evaluation** | Progress evaluation, knowledge distillation, doc generation | progress-evaluation, knowledge-distillation |

Routing supports 4 priority levels: explicit tags (`@development`) → keyword detection → branch name patterns → default fallback.

### 💾 Three-Layer Cross-Session Memory System

Solves the core pain point of AI agents "starting fresh every time":

| Layer | Name | Scope | Capacity |
|-------|------|-------|----------|
| Layer 1 | **Workspace Memory** | Workspace-level | 4,000 chars |
| Layer 2 | **Stage Memory** | Profile × Workspace | 8,000 chars |
| Layer 3 | **Session Summary** | Single session | 2,000 chars/entry |

Effect: New sessions immediately have project context, saving 30-40% token consumption.

### 🛡️ Quality Assurance System

- **Automatic baseline checks**: After code generation, automatically runs code-style / security / api-contract / architecture baselines
- **HITL approval checkpoints**: Pauses at key delivery nodes for human approval — supports Approve / Reject / Modify
- **Four-dimensional evaluation learning loop**: Intent understanding + Completion + Quality + Experience, auto-generates improvement suggestions
- **Prompt Caching**: System Prompt cached for 5 minutes, 90% cost savings on cache hits

### 🌐 Web IDE

```
┌─────────────────────────────────────────────────────────────────┐
│ Header — Role Switch + Model Selector + User Menu               │
├──────────┬──────────────────────────────┬───────────────────────┤
│ File     │ Monaco Editor                │ AI Chat Sidebar       │
│ Explorer │   - 25+ language highlights  │  4-Tab:               │
│          │   - Multi-tab file editing   │  [Chat|Quality|Skills │
│  CRUD:   │   - AI Explain button        │   |Memory]            │
│  Create  │   - 5s auto-save             │   - Streaming output  │
│  Rename  ├──────────────────────────────┤   - Tool call expand  │
│  Delete  │ Terminal Panel (collapsible)  │   - OODA indicator    │
└──────────┴──────────────────────────────┴───────────────────────┘
```

### 🔌 Multi-Model Support

| Provider | Supported Models | Context Window |
|----------|-----------------|----------------|
| **Anthropic Claude** | Opus 4.6 / Sonnet 4.5 / Haiku 4.5 | 200K |
| **Google Gemini** | Gemini Pro | 30K |
| **Alibaba Qwen** | Qwen2.5-7B / 72B | 32K |
| **AWS Bedrock** | Claude via AWS | Provider-specific |
| **MiniMax** | M2.5 / M2.5-lightning / M2.5-highspeed | 1M |
| **OpenAI-compatible** | Any service compatible with OpenAI API | Provider-specific |

---

## 🏗️ Architecture

### System Architecture Diagram

```
┌─ User Interaction Layer ────────────────────────────────────────┐
│  Web IDE (Next.js 15)  │  CLI (Kotlin + GraalVM Native)         │
└────────────────────────────────────────────────────────────────┘
                         │
┌─ Application Layer ─────────────────────────────────────────────┐
│  Spring Boot 3 Backend                                           │
│  ├─ AgenticLoopOrchestrator (50-turn autonomous execution)       │
│  ├─ ProfileRouter (6 Profile intelligent routing)               │
│  ├─ SkillLoader (32 Skills dynamic loading)                     │
│  ├─ SystemPromptAssembler (dynamic prompt assembly)             │
│  ├─ McpProxyService (18 tool call proxy)                        │
│  ├─ MemoryContextLoader (three-layer memory injection)          │
│  ├─ HitlCheckpointManager (HITL approval management)            │
│  └─ LearningLoopPipelineService (learning feedback loop)        │
└────────────────────────────────────────────────────────────────┘
                         │
┌─ MCP Tool Layer ────────────────────────────────────────────────┐
│  forge-knowledge-mcp:8081  │  forge-database-mcp:8082           │
│  forge-service-graph-mcp   │  forge-artifact-mcp                │
│  forge-observability-mcp   │  (built-in workspace/baseline tools)│
└────────────────────────────────────────────────────────────────┘
                         │
┌─ Model Adapter Layer ───────────────────────────────────────────┐
│  ClaudeAdapter  │  GeminiAdapter  │  QwenAdapter                │
│  BedrockAdapter │  OpenAIAdapter  │  (unified ModelAdapter interface)│
└────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend Language | Kotlin | 1.9+ |
| Backend Framework | Spring Boot | 3.3+ |
| Runtime | JDK | 21 |
| Frontend Framework | Next.js + React | 15 + 19 |
| Frontend Language | TypeScript | 5.x |
| Database (Dev) | H2 file persistence | — |
| Database (Prod) | PostgreSQL | 16 |
| DB Migration | Flyway | 8 versions (V1-V8) |
| Code Editor | Monaco Editor | 4.6+ |
| Workflow Canvas | ReactFlow | 12.3+ |
| Diagram Rendering | Mermaid | 11.4+ |
| Authentication | Keycloak | 24.0 |
| Monitoring | Prometheus + Micrometer | — |
| Containerization | Docker Compose | 6 containers |

### Gradle Module Structure

```
forge-platform/
├── web-ide/
│   ├── backend/          # Spring Boot 3 backend (Kotlin)
│   └── frontend/         # Next.js 15 frontend (TypeScript)
├── mcp-servers/
│   ├── forge-mcp-common/        # MCP protocol common library
│   ├── forge-knowledge-mcp/     # Knowledge base MCP server
│   ├── forge-database-mcp/      # Database MCP server
│   ├── forge-service-graph-mcp/ # Service topology MCP server
│   ├── forge-artifact-mcp/      # Build artifact MCP server
│   └── forge-observability-mcp/ # Observability MCP server
├── adapters/
│   ├── model-adapter/    # Model adapters (Claude/Gemini/Qwen/Bedrock)
│   └── runtime-adapter/  # Runtime adapter
├── plugins/
│   ├── forge-foundation/  # Foundation Skills (Kotlin/Java/Spring/API conventions)
│   ├── forge-superagent/  # SuperAgent Skills + 6 Profiles
│   ├── forge-knowledge/   # Knowledge Skills
│   └── forge-deployment/  # Deployment Skills (K8s/CI-CD)
├── cli/                   # Forge CLI (Kotlin + GraalVM Native)
├── agent-eval/            # SuperAgent evaluation framework
├── skill-tests/           # Skill validation framework
├── knowledge-base/        # Knowledge base documents (13+ docs)
└── infrastructure/
    └── docker/            # Docker Compose deployment configuration
```

---

## 🚀 Quick Start

### Prerequisites

| Item | Requirement | Verification |
|------|-------------|-------------|
| JDK | **21** (required! JDK 8/17 will fail to compile) | `java -version` |
| Docker Engine | 24+ | `docker --version` |
| Docker Compose | v2+ | `docker compose version` |
| Node.js | 20+ | `node --version` |
| Available Memory | ≥ 8 GB (allocated to Docker) | Docker Desktop → Resources |
| API Key | At least 1 Provider | See configuration below |

### Step 1: Clone the Repository

```bash
git clone git@github.com:pan94u/forge.git
cd forge
```

### Step 2: Configure Environment Variables

```bash
cp .env.example infrastructure/docker/.env
```

Edit `infrastructure/docker/.env` and fill in at least one Provider's API Key:

```bash
# Anthropic Claude (recommended)
ANTHROPIC_API_KEY=sk-ant-api03-your-key

# Or Google Gemini
# GEMINI_API_KEY=AIza...

# Or Alibaba DashScope (Qwen)
# DASHSCOPE_API_KEY=sk-...

# Or OpenAI-compatible (Ollama/vLLM/LocalAI, etc.)
# LOCAL_MODEL_URL=http://localhost:11434
# LOCAL_MODEL_NAME=llama3.1:8b
```

### Step 3: Build Locally

```bash
# Ensure JDK 21 (macOS example)
export JAVA_HOME=/opt/homebrew/opt/openjdk@21

# Build backend JAR (skip tests for speed)
./gradlew :web-ide:backend:bootJar -x test --no-daemon

# Build frontend
cd web-ide/frontend && npm install && npm run build && cd ../..
```

### Step 4: Start with Docker

```bash
cd infrastructure/docker
docker compose -f docker-compose.trial.yml --env-file .env up --build -d
```

### Step 5: Verify Startup

```bash
# Check all 6 containers (all should be healthy or running)
docker compose -f docker-compose.trial.yml ps

# Test API
curl -s http://localhost:9000/api/models | python3 -m json.tool
```

### Step 6: Access the Platform

Open your browser and visit **http://localhost:9000**

---

## 📊 Technical Specifications

| Dimension | Value |
|-----------|-------|
| REST API Endpoints | 68 (11 Controllers) |
| SSE Event Types | 14 |
| MCP Tools | 18 built-in + external auto-discovery |
| JPA Entities | 12 |
| Flyway Migrations | 8 versions (V1-V8) |
| Total Skills | 32 (6 Profiles) |
| Unit Tests | 156 (all passing) |
| Docker Containers | 6 |
| Knowledge Base Docs | 13+ |
| Code Volume | ~50K+ lines (Kotlin + TypeScript) |
| Frontend Routes | 7 |
| Supported Models | 6 Providers (13+ models) |
| Context Window | Up to 200K tokens (Claude Opus) |
| Autonomous Execution Turns | Up to 50 |

---

## 🗺️ Roadmap

| Phase | Keyword | Status | Key Capabilities |
|-------|---------|--------|-----------------|
| Phase 0 | Foundation | ✅ | Foundation Skills + MCP Servers + CLI + Plugin system |
| Phase 1 | Web IDE Connected | ✅ | Real streaming + Agentic Loop + Cross-stack profiling |
| Phase 1.5 | Design Guard + Docker | ✅ | Docker deployment + E2E validation + Design baseline freeze |
| Phase 1.6 | AI Delivery Loop + SSO | ✅ | AI→Workspace file writing + Keycloak SSO + Context Picker |
| Phase 2 | Quality Infrastructure | ✅ | CI/CD + SkillLoader + Real MCP services + Multi-model |
| Phase 3 | HITL + Memory | ✅ | Three-layer memory + HITL approval + Learning loop + Quality panel |
| Phase 4 | Skill Architecture | ✅ | Progressive loading + Skill management + Usage tracking |
| Phase 5 | Product Usability | ✅ | Workspace persistence + Git Clone + User API Keys |
| Phase 6 | Knowledge Write + Multi-model | ✅ | MiniMax + Local knowledge write + 50-turn Agentic Loop |
| Phase 7 | Async + Knowledge Scope | ✅ | Async Git Clone + Three-tier knowledge scope + CRUD |
| Phase 8+ | Learning Loop Closure | 🔄 | ForgeNativeRuntime + Methodology platformization |

---

## 🔧 Development Guide

### Run Unit Tests

```bash
# Backend tests
./gradlew :web-ide:backend:test :adapters:model-adapter:test

# Evaluation framework tests
./gradlew :agent-eval:test
```

### Local Development Mode (without Docker)

```bash
# Start backend
cd web-ide/backend
./gradlew bootRun

# Start frontend (new terminal)
cd web-ide/frontend
npm run dev
# Access http://localhost:3000
```

### Common Issues

| Issue | Solution |
|-------|---------|
| JDK version error | Must use JDK 21: `export JAVA_HOME=/opt/homebrew/opt/openjdk@21` |
| Frontend type errors | Use `npm run build` not `npm run dev` (dev doesn't check types) |
| WebSocket CORS | `forge.websocket.allowed-origins` must be a comma-separated string |
| Enum serialization | All enums must add `@JsonValue` returning lowercase |
| Empty string vs null | Use `isNullOrBlank()` instead of `?: default` |

---

## 🆚 Competitive Positioning

### vs. GitHub Copilot / Cursor

| Dimension | Copilot / Cursor | Forge |
|-----------|-----------------|-------|
| Positioning | Code completion / editor-level AI | **Autonomous full-delivery SuperAgent** |
| Execution Mode | User gives line-by-line instructions | **User declares intent, Agent autonomously plans→executes→verifies→delivers** |
| Coverage | Development phase only | Planning→Design→Dev→Test→Ops→Evaluation |
| Execution Depth | Single completion / single conversation | **Up to 50 autonomous turns (tool chain orchestration + self-repair)** |
| Role Switching | None | 6 Profiles with automatic routing |
| Quality Assurance | None | **Automatic baseline checks + self-repair retry** |
| Human-in-the-Loop | None | HITL approval checkpoints |
| Knowledge System | None | 32 Skills + 13+ knowledge docs + knowledge writing |
| Memory | Single-file context | **3-layer cross-session memory (project + stage + session summary)** |
| Learning | None | **Four-dimensional evaluation + learning loop pipeline** |

### vs. Traditional DevOps Platforms (Jenkins / GitLab CI)

| Dimension | Traditional DevOps | Forge |
|-----------|-------------------|-------|
| Automation Target | CI/CD pipelines | **Full delivery lifecycle (including planning, design, coding, evaluation)** |
| Execution Mode | Predefined scripts, static pipelines | **AI Agent dynamic decision-making, autonomously selects tools and strategies** |
| AI Capability | None / limited | **Multi-turn Agentic Loop + 18 tool orchestration + knowledge base** |
| Learning | Static rules | **Skill evolution + memory accumulation + four-dimensional evaluation feedback** |

---

## 📁 Documentation Index

| Document | Path | Description |
|----------|------|-------------|
| Product Feature List | `docs/product/feature-list.md` | Complete feature description (user-facing) |
| Design Baseline | `docs/baselines/design-baseline-v1.md` | Validated UI/API/data model baseline (v12) |
| Planning Baseline | `docs/baselines/planning-baseline-v1.5.md` | Design-driven planning document |
| Development Logbook | `docs/planning/dev-logbook.md` | Complete development records for 32 sessions |
| Architecture Overview | `docs/architecture/overview.md` | System architecture documentation |
| Trial Guide | `docs/product/TRIAL-GUIDE.md` | Internal trial operation manual |
| Acceptance Tests | `docs/acceptance-tests/` | Phase-by-phase acceptance test reports |
| Bug List | `docs/analysis/buglist.md` | Known issue tracking |

---

## 🤝 Contributing

Contributions are welcome! Here's how you can participate:

- **Submit Issues**: Report bugs, suggest features, or propose improvements
- **Submit Pull Requests**: Fix bugs, improve documentation, add new Skills
- **Extend Skills**: Add new Skills or Profiles in the `plugins/` directory
- **Enrich Knowledge Base**: Add documentation in the `knowledge-base/` directory

Before submitting a PR, please ensure:
1. All unit tests pass: `./gradlew :web-ide:backend:test`
2. Frontend builds without errors: `cd web-ide/frontend && npm run build`
3. Code follows existing style conventions (see `plugins/forge-foundation/skills/kotlin-conventions/`)

> **Note**: By submitting a contribution, you agree to license your contribution to Forge Ltd as described in the [LICENSE](LICENSE).

---

## 📄 License

This project is licensed under the [Forge Source Available License v1.0](LICENSE).

- ✅ Permitted: View source, personal learning, non-commercial use and modification, submit contributions
- ❌ Restricted: Commercial use (SaaS, internal business tooling, consulting services) requires written authorization

Commercial licensing inquiries: legal@forge.ltd

---

<div align="center">

**Forge — Making AI truly participate in software delivery, not just assist with coding**

</div>
