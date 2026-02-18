# Forge MCP Tools Reference

## Overview

Forge exposes tools via the Model Context Protocol (MCP) that the AI agent can invoke during conversations. Tools are registered in `McpProxyService` and dispatched through the agentic loop in `ClaudeAgentService`.

## Workspace Tools (AI Delivery Surface)

### workspace_write_file

Create or overwrite a file in the current workspace.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | yes | File path relative to workspace root (e.g., `src/main/App.kt`) |
| `content` | string | yes | Complete file content to write |

**Behavior**: Creates parent directories if needed. Emits `file_changed` event to refresh the IDE file tree.

### workspace_read_file

Read a file's content from the current workspace.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | yes | File path relative to workspace root |

**Returns**: Full file content as text.

### workspace_list_files

List all files in the current workspace.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| (none) | — | — | — |

**Returns**: Indented file tree showing directory structure.

## Knowledge Tools

### search_knowledge

Search the knowledge base for relevant documents.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | yes | Search query (matched against filenames and content) |

**Returns**: Matching file names and content excerpts.

### read_file

Read a specific file from the knowledge base.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | yes | Path relative to knowledge-base directory |

**Security**: Path traversal is blocked.

## Infrastructure Tools

### get_service_info

Get information about platform services.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `service` | string | no | Service name (backend/frontend/nginx/keycloak), or empty for all |

### query_schema

Query database schema information.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | yes | SQL query (SELECT only) |

**Security**: Only SELECT statements allowed. DDL/DML rejected.

## Baseline Tools

### run_baseline

Execute baseline quality checks.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `baselines` | string[] | no | Specific baselines to run, or empty for all |
| `workspaceId` | string | no | Workspace context |

### list_baselines

List all available baseline scripts.

No parameters required.

## Tool Calling Flow

```
User message → ClaudeAgentService → Claude API → tool_use response
  → McpProxyService.callTool(name, args, workspaceId)
  → tool result → back to Claude → next turn or final response
```

Maximum 5 agentic turns per conversation message.
