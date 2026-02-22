package com.forge.webide.service.memory

import com.forge.adapter.model.*
import com.forge.webide.entity.SessionSummaryEntity
import com.forge.webide.model.ToolCallRecord
import com.forge.webide.repository.SessionSummaryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Generates structured session summaries at the end of each session.
 * Summaries are used by Layer 3 (Session Memory) for cross-session continuity.
 */
@Service
class SessionSummaryService(
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val claudeAdapter: ModelAdapter
) {
    private val logger = LoggerFactory.getLogger(SessionSummaryService::class.java)
    private val gson = Gson()

    @Value("\${forge.model.name:\${forge.claude.model:claude-sonnet-4-6}}")
    private var model: String = "claude-sonnet-4-6"

    companion object {
        private const val MAX_SUMMARY_CHARS = 2000
        private const val SUMMARY_PROMPT = """请为本次会话生成结构化摘要，严格使用以下 JSON 格式输出（不要包含 markdown 代码块标记）：
{
  "summary": "2-3 句话概述本次会话做了什么",
  "completedWork": ["已完成的工作项1", "..."],
  "artifacts": ["产出的文件路径1", "..."],
  "decisions": ["做出的关键决策1", "..."],
  "unresolved": ["未解决的问题1", "..."],
  "nextSteps": ["建议的下一步1", "..."]
}"""
    }

    /**
     * Generate a structured summary for the session and persist it.
     * This is called asynchronously after the session completes.
     */
    fun generateAndSave(
        sessionId: String,
        workspaceId: String,
        profile: String,
        conversationHistory: List<Message>,
        toolCalls: List<ToolCallRecord>
    ) {
        // Skip if summary already exists for this session
        if (sessionSummaryRepository.findBySessionId(sessionId) != null) {
            logger.debug("Summary already exists for session {}", sessionId)
            return
        }

        try {
            val summaryJson = generateSummaryViaLLM(conversationHistory)
            saveSummary(sessionId, workspaceId, profile, summaryJson, conversationHistory.size, toolCalls.size)
            logger.info("Session summary generated for session {} ({} chars)", sessionId, summaryJson.length)
        } catch (e: Exception) {
            logger.warn("Failed to generate session summary for {}: {}", sessionId, e.message)
            // Fallback: create a minimal summary from tool calls
            saveFallbackSummary(sessionId, workspaceId, profile, conversationHistory, toolCalls)
        }
    }

    /**
     * Get recent session summaries for a workspace, ordered by creation time descending.
     */
    fun getRecentSummaries(workspaceId: String, limit: Int = 3): List<SessionSummaryEntity> {
        return sessionSummaryRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
            .take(limit)
    }

    /**
     * Get recent summaries filtered by profile.
     */
    fun getRecentSummariesByProfile(workspaceId: String, profile: String, limit: Int = 3): List<SessionSummaryEntity> {
        return sessionSummaryRepository.findByWorkspaceIdAndProfileOrderByCreatedAtDesc(workspaceId, profile)
            .take(limit)
    }

    /**
     * Get a single summary by session ID.
     */
    fun getSummary(sessionId: String): SessionSummaryEntity? {
        return sessionSummaryRepository.findBySessionId(sessionId)
    }

    /**
     * Format a session summary entity as a Markdown snippet for system prompt injection.
     */
    fun formatSummaryForPrompt(entity: SessionSummaryEntity): String {
        val completedWork = parseJsonArray(entity.completedWork)
        val artifacts = parseJsonArray(entity.artifacts)
        val decisions = parseJsonArray(entity.decisions)
        val unresolved = parseJsonArray(entity.unresolved)

        return buildString {
            appendLine("### Session (${entity.createdAt.toString().take(16)})")
            appendLine("**Profile**: ${entity.profile} | **Turns**: ${entity.turnCount} | **Tools**: ${entity.toolCallCount}")
            appendLine()
            appendLine(entity.summary)
            if (completedWork.isNotEmpty()) {
                appendLine()
                appendLine("**完成工作**: ${completedWork.joinToString("; ")}")
            }
            if (artifacts.isNotEmpty()) {
                appendLine("**产出物**: ${artifacts.joinToString(", ")}")
            }
            if (decisions.isNotEmpty()) {
                appendLine("**决策**: ${decisions.joinToString("; ")}")
            }
            if (unresolved.isNotEmpty()) {
                appendLine("**未解决**: ${unresolved.joinToString("; ")}")
            }
        }.take(MAX_SUMMARY_CHARS)
    }

    // ---- Internal ----

    private fun generateSummaryViaLLM(conversationHistory: List<Message>): String {
        // Build a condensed version of the conversation for summary generation
        val condensedHistory = condenseForSummary(conversationHistory)

        val messages = condensedHistory + Message(
            role = Message.Role.USER,
            content = SUMMARY_PROMPT
        )

        val options = CompletionOptions(
            model = model,
            maxTokens = 1024,
            systemPrompt = "你是一个会话摘要生成器。根据对话历史生成结构化 JSON 摘要。只输出 JSON，不要包含任何其他文本。",
            temperature = 0.3
        )

        val result = runBlocking {
            val events = claudeAdapter.streamWithTools(messages, options, emptyList()).toList()
            events.filterIsInstance<StreamEvent.ContentDelta>()
                .joinToString("") { it.text }
        }

        return result.trim()
    }

    /**
     * Condense conversation history for summary generation.
     * Keep user messages and assistant content, truncate tool outputs.
     */
    private fun condenseForSummary(messages: List<Message>): List<Message> {
        return messages.map { msg ->
            when (msg.role) {
                Message.Role.TOOL -> Message(
                    role = msg.role,
                    content = msg.content.take(200),
                    toolResults = msg.toolResults?.map { tr ->
                        ToolResult(
                            toolUseId = tr.toolUseId,
                            content = tr.content.take(200),
                            isError = tr.isError
                        )
                    }
                )
                else -> Message(
                    role = msg.role,
                    content = msg.content.take(1000),
                    toolUses = msg.toolUses
                )
            }
        }.takeLast(10) // Keep only the most recent messages
    }

    private fun saveSummary(
        sessionId: String,
        workspaceId: String,
        profile: String,
        summaryJson: String,
        turnCount: Int,
        toolCallCount: Int
    ) {
        try {
            val parsed = parseSummaryJson(summaryJson)
            val entity = SessionSummaryEntity(
                sessionId = sessionId,
                workspaceId = workspaceId,
                profile = profile,
                summary = (parsed["summary"] as? String) ?: summaryJson.take(500),
                completedWork = gson.toJson(parsed["completedWork"] ?: emptyList<String>()),
                artifacts = gson.toJson(parsed["artifacts"] ?: emptyList<String>()),
                decisions = gson.toJson(parsed["decisions"] ?: emptyList<String>()),
                unresolved = gson.toJson(parsed["unresolved"] ?: emptyList<String>()),
                nextSteps = gson.toJson(parsed["nextSteps"] ?: emptyList<String>()),
                turnCount = turnCount,
                toolCallCount = toolCallCount
            )
            sessionSummaryRepository.save(entity)
        } catch (e: Exception) {
            logger.warn("Failed to parse summary JSON, saving raw: {}", e.message)
            val entity = SessionSummaryEntity(
                sessionId = sessionId,
                workspaceId = workspaceId,
                profile = profile,
                summary = summaryJson.take(500),
                turnCount = turnCount,
                toolCallCount = toolCallCount
            )
            sessionSummaryRepository.save(entity)
        }
    }

    private fun saveFallbackSummary(
        sessionId: String,
        workspaceId: String,
        profile: String,
        messages: List<Message>,
        toolCalls: List<ToolCallRecord>
    ) {
        val toolNames = toolCalls.map { it.name }.distinct()
        val artifacts = toolCalls
            .filter { it.name == "workspace_write_file" && it.status != "error" }
            .mapNotNull { it.input["path"] as? String }

        val summary = "会话使用了 ${toolCalls.size} 个工具调用（${toolNames.joinToString(", ")}），共 ${messages.size} 轮对话。"

        val entity = SessionSummaryEntity(
            sessionId = sessionId,
            workspaceId = workspaceId,
            profile = profile,
            summary = summary,
            artifacts = gson.toJson(artifacts),
            turnCount = messages.size,
            toolCallCount = toolCalls.size
        )

        try {
            sessionSummaryRepository.save(entity)
            logger.info("Fallback summary saved for session {}", sessionId)
        } catch (e: Exception) {
            logger.warn("Failed to save fallback summary for {}: {}", sessionId, e.message)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSummaryJson(json: String): Map<String, Any?> {
        // Strip markdown code block markers if present
        val cleaned = json
            .replace(Regex("^```json\\s*"), "")
            .replace(Regex("```\\s*$"), "")
            .trim()

        return try {
            gson.fromJson(cleaned, Map::class.java) as? Map<String, Any?> ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun parseJsonArray(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
