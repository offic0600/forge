# ADR-004: Web IDE Architecture — Delivery Surface

## Status

Accepted

## Context

Phase 2 delivered full SuperAgent intelligence (32 Skills, 5 Profiles, OODA loop, baselines). However, the AI could only "talk" — it had no mechanism to write generated code into workspace files. The IDE and AI were two isolated islands.

The OODA loop's Act phase had no delivery target. The baseline v1.2 treated the workspace as "user's manual editing space" rather than "AI's workbench."

## Decision

Introduce the **Delivery Surface** concept: AI operates on workspace files through MCP tools, and the IDE provides real-time feedback.

### Architecture

```
AI (Claude) ──tool_use──→ McpProxyService ──→ WorkspaceService ──→ Files
                                │
                          file_changed event
                                │
                                ▼
                     Frontend (file tree refresh + auto-open)
```

### Key Design Choices

1. **Workspace tools as MCP tools**: `workspace_write_file`, `workspace_read_file`, `workspace_list_files` are registered alongside existing MCP tools (search_knowledge, query_schema, etc.). This means Claude can use them naturally in the agentic loop — no special handling needed.

2. **System prompt delivery guidance**: The SystemPromptAssembler injects behavioral rules telling Claude to always write files rather than just showing code in chat. This makes the AI a "delivery assistant" by default.

3. **file_changed SSE events**: When AI writes a file, the backend emits an SSE event through the existing streaming channel. The frontend listens and refreshes the file tree + auto-opens the new file. No polling needed.

4. **Apply button on code blocks**: As a fallback, users can manually apply code from chat to workspace via an "Apply" button on code blocks. This covers cases where AI shows code without using the write tool.

## Consequences

### Positive

- AI can complete end-to-end delivery tasks (user request → code in workspace)
- OODA Act phase has a real target
- Existing agentic loop infrastructure is reused (no new streaming protocol)
- Users see AI's work immediately in the editor

### Negative

- In-memory WorkspaceService means files are lost on restart (acceptable for trial mode; production would use persistent storage)
- No diff/merge — AI overwrites entire files (acceptable for Phase 2.5; Phase 3 can add diff view)

## Related

- ADR-001: SuperAgent over Multi-Agent
- ADR-003: Baseline Quality Gates (baselines can now check AI-written files)
