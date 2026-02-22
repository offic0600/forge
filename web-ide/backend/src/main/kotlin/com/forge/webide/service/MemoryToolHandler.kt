package com.forge.webide.service

import com.forge.webide.model.McpContent
import com.forge.webide.model.McpToolCallResponse
import com.forge.webide.service.memory.SessionSummaryService
import com.forge.webide.service.memory.WorkspaceMemoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Handles memory and codebase-analysis MCP tools: update_workspace_memory,
 * get_session_history, analyze_codebase.
 */
@Service
class MemoryToolHandler(
    private val workspaceMemoryService: WorkspaceMemoryService,
    private val sessionSummaryService: SessionSummaryService,
    private val workspaceService: WorkspaceService
) {

    private val logger = LoggerFactory.getLogger(MemoryToolHandler::class.java)

    @Value("\${forge.plugins.base-path:plugins}")
    private var pluginsBasePath: String = "plugins"

    fun handle(toolName: String, args: Map<String, Any?>, workspaceId: String?): McpToolCallResponse {
        return when (toolName) {
            "update_workspace_memory" -> {
                if (!workspaceId.isNullOrBlank()) {
                    handleUpdateWorkspaceMemory(args, workspaceId)
                } else {
                    handleUpdateWorkspaceMemory(args)
                }
            }
            "get_session_history" -> {
                if (!workspaceId.isNullOrBlank()) {
                    handleGetSessionHistory(args, workspaceId)
                } else {
                    handleGetSessionHistory(args)
                }
            }
            "analyze_codebase" -> handleAnalyzeCodebase(workspaceId)
            else -> McpProxyService.errorResponse("Unknown memory tool: $toolName")
        }
    }

    /**
     * Update workspace memory content (workspace-scoped version).
     */
    private fun handleUpdateWorkspaceMemory(args: Map<String, Any?>, workspaceId: String): McpToolCallResponse {
        val content = args["content"] as? String
            ?: return McpProxyService.errorResponse("'content' parameter is required")
        workspaceMemoryService.updateMemory(workspaceId, content)
        return McpToolCallResponse(
            content = listOf(McpContent(type = "text", text = "Workspace memory updated (${content.length} chars)")),
            isError = false
        )
    }

    /**
     * Update workspace memory content (built-in tool version, needs workspaceId in args).
     */
    private fun handleUpdateWorkspaceMemory(args: Map<String, Any?>): McpToolCallResponse {
        val workspaceId = args["workspaceId"] as? String
            ?: return McpProxyService.errorResponse("'workspaceId' parameter is required")
        val content = args["content"] as? String
            ?: return McpProxyService.errorResponse("'content' parameter is required")
        workspaceMemoryService.updateMemory(workspaceId, content)
        return McpToolCallResponse(
            content = listOf(McpContent(type = "text", text = "Workspace memory updated (${content.length} chars)")),
            isError = false
        )
    }

    /**
     * Get session history summaries (workspace-scoped version).
     */
    private fun handleGetSessionHistory(args: Map<String, Any?>, workspaceId: String): McpToolCallResponse {
        val limit = (args["limit"] as? Number)?.toInt() ?: 5
        val summaries = sessionSummaryService.getRecentSummaries(workspaceId, limit)
        if (summaries.isEmpty()) {
            return McpToolCallResponse(
                content = listOf(McpContent(type = "text", text = "No session history found for this workspace.")),
                isError = false
            )
        }
        val text = buildString {
            appendLine("Session History (${summaries.size} sessions):")
            appendLine()
            for (s in summaries) {
                appendLine(sessionSummaryService.formatSummaryForPrompt(s))
                appendLine()
            }
        }
        return McpToolCallResponse(
            content = listOf(McpContent(type = "text", text = text)),
            isError = false
        )
    }

    /**
     * Get session history summaries (built-in tool version, needs workspaceId in args).
     */
    private fun handleGetSessionHistory(args: Map<String, Any?>): McpToolCallResponse {
        val workspaceId = args["workspaceId"] as? String
            ?: return McpProxyService.errorResponse("'workspaceId' parameter is required")
        return handleGetSessionHistory(args, workspaceId)
    }

    // ---- Codebase Analysis ----

    private fun handleAnalyzeCodebase(workspaceId: String?): McpToolCallResponse {
        if (workspaceId.isNullOrBlank()) {
            return McpProxyService.errorResponse("analyze_codebase requires a workspace context (workspaceId)")
        }

        val wsDir = try {
            workspaceService.getWorkspace(workspaceId)
                ?: return McpProxyService.errorResponse("Workspace not found: $workspaceId")
            File("./data/workspaces/$workspaceId")
        } catch (e: Exception) {
            return McpProxyService.errorResponse("Failed to resolve workspace directory: ${e.message}")
        }

        if (!wsDir.exists() || !wsDir.isDirectory) {
            return McpProxyService.errorResponse("Workspace directory not found: ${wsDir.absolutePath}")
        }

        val scriptPath = resolveAnalyzeScript()
            ?: return McpProxyService.errorResponse("analyze-structure.py script not found in codebase-profiler skill")

        return try {
            val process = ProcessBuilder("python3", scriptPath.absolutePath, wsDir.absolutePath)
                .redirectErrorStream(false)
                .start()

            val finished = process.waitFor(60, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return McpProxyService.errorResponse("Codebase analysis timed out after 60 seconds")
            }

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()

            if (process.exitValue() != 0) {
                return McpProxyService.errorResponse("Analysis script failed (exit=${process.exitValue()}): $stderr")
            }

            McpToolCallResponse(
                content = listOf(McpContent(type = "text", text = stdout)),
                isError = false
            )
        } catch (e: Exception) {
            logger.error("analyze_codebase failed: {}", e.message)
            McpProxyService.errorResponse("Codebase analysis error: ${e.message}")
        }
    }

    private fun resolveAnalyzeScript(): File? {
        val candidates = listOf(
            File(pluginsBasePath, "forge-foundation/skills/codebase-profiler/scripts/analyze-structure.py"),
            File("plugins/forge-foundation/skills/codebase-profiler/scripts/analyze-structure.py"),
            File("/plugins/forge-foundation/skills/codebase-profiler/scripts/analyze-structure.py")
        )
        return candidates.firstOrNull { it.exists() }
    }
}
