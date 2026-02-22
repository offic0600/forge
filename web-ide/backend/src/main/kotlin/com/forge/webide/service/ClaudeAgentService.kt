package com.forge.webide.service

import com.forge.adapter.model.*
import com.forge.webide.model.*
import com.forge.webide.entity.ChatMessageEntity
import com.forge.webide.entity.ExecutionRecordEntity
import com.forge.webide.entity.HitlCheckpointEntity
import com.forge.webide.entity.ToolCallEntity
import com.forge.webide.repository.ChatMessageRepository
import com.forge.webide.repository.ChatSessionRepository
import com.forge.webide.repository.ExecutionRecordRepository
import com.forge.webide.service.memory.MemoryContext
import com.forge.webide.service.memory.MemoryContextLoader
import com.forge.webide.service.memory.SessionSummaryService
import com.forge.webide.service.skill.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Integrates with the Claude API via [ModelAdapter] for AI-powered chat capabilities.
 *
 * Supports:
 * - Real streaming via SSE (no artificial delays)
 * - Multi-turn agentic tool calling loop (max 5 turns)
 * - Database persistence of conversations
 *
 * Delegates to:
 * - [AgenticLoopOrchestrator] for the multi-turn agentic streaming loop
 * - [HitlCheckpointManager] for human-in-the-loop checkpoint management
 * - [BaselineAutoChecker] for baseline quality gate auto-checks
 */
@Service
class ClaudeAgentService(
    private val claudeAdapter: ModelAdapter,
    private val mcpProxyService: McpProxyService,
    private val knowledgeGapDetectorService: KnowledgeGapDetectorService,
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val executionRecordRepository: ExecutionRecordRepository,
    private val profileRouter: ProfileRouter,
    private val skillLoader: SkillLoader,
    private val systemPromptAssembler: SystemPromptAssembler,
    private val metricsService: MetricsService,
    private val sessionSummaryService: SessionSummaryService,
    private val memoryContextLoader: MemoryContextLoader,
    private val userModelConfigService: UserModelConfigService,
    // Extracted services
    private val agenticLoopOrchestrator: AgenticLoopOrchestrator,
    private val hitlCheckpointManager: HitlCheckpointManager,
    private val baselineAutoChecker: BaselineAutoChecker
) {
    private val logger = LoggerFactory.getLogger(ClaudeAgentService::class.java)
    private val executor = Executors.newFixedThreadPool(10)

    @Value("\${forge.model.name:\${forge.claude.model:claude-sonnet-4-6}}")
    private var model: String = "claude-sonnet-4-6"

    /**
     * Resolve the user's API key for the given session.
     * Looks up userId from the chat session entity, then queries UserModelConfigService.
     * Returns null if no user key is configured (adapter falls back to server default).
     */
    private fun resolveUserApiKey(sessionId: String): String? {
        return try {
            val session = chatSessionRepository.findById(sessionId).orElse(null) ?: return null
            val userId = session.userId.takeIf { it.isNotBlank() && it != "anonymous" } ?: return null
            userModelConfigService.getDecryptedApiKey(userId, "anthropic")
        } catch (e: Exception) {
            logger.debug("Failed to resolve user API key for session {}: {}", sessionId, e.message)
            null
        }
    }

    /**
     * Build a dynamic system prompt based on the user's message.
     * Injects 3-layer memory context for cross-session continuity.
     * Falls back to a static prompt if skill loading fails.
     */
    private fun buildDynamicSystemPrompt(message: String, workspaceId: String = ""): DynamicPromptResult {
        return try {
            val routing = profileRouter.route(message)
            // Load memory context for cross-session continuity
            val memoryContext = if (workspaceId.isNotBlank()) {
                try {
                    memoryContextLoader.loadMemoryContext(workspaceId, routing.profile.name)
                } catch (e: Exception) {
                    logger.debug("Failed to load memory context: {}", e.message)
                    MemoryContext()
                }
            } else {
                MemoryContext()
            }
            val skills = skillLoader.loadSkillsForProfile(routing.profile, message)
            val systemPrompt = systemPromptAssembler.assemble(routing.profile, skills, memoryContext)
            metricsService.recordProfileRoute(routing.profile.name, routing.reason)
            metricsService.recordSkillLoaded(routing.profile.name, skills.size)
            DynamicPromptResult(
                systemPrompt = systemPrompt,
                activeProfile = routing.profile.name,
                loadedSkills = skills.map { it.name },
                routingReason = routing.reason,
                confidence = routing.confidence
            )
        } catch (e: Exception) {
            logger.warn("Failed to build dynamic system prompt, using fallback: {}", e.message)
            DynamicPromptResult(
                systemPrompt = systemPromptAssembler.fallbackPrompt(),
                activeProfile = "fallback",
                loadedSkills = emptyList(),
                routingReason = "Fallback due to error: ${e.message}",
                confidence = 0.0
            )
        }
    }

    /**
     * Send a synchronous (non-streaming) message to Claude and return the response.
     */
    fun sendMessage(
        sessionId: String,
        message: String,
        contexts: List<ContextReference>,
        workspaceId: String
    ): ChatMessageResponse {
        val fullMessage = buildContextualMessage(message, contexts)
        val history = loadConversationHistory(sessionId)

        val messages = history + Message(role = Message.Role.USER, content = fullMessage)

        val tools = mcpProxyService.listTools().map { tool ->
            ToolDefinition(
                name = tool.name,
                description = tool.description,
                inputSchema = tool.inputSchema
            )
        }

        return try {
            val promptResult = buildDynamicSystemPrompt(message, workspaceId)
            logger.info("Profile: {}, Skills: {}", promptResult.activeProfile, promptResult.loadedSkills)

            val userApiKey = resolveUserApiKey(sessionId)
            val options = CompletionOptions(
                model = model,
                maxTokens = 4096,
                systemPrompt = promptResult.systemPrompt,
                apiKeyOverride = userApiKey
            )

            // Run single-turn completion with rate limit retry
            val result = runBlocking {
                val events = agenticLoopOrchestrator.streamWithRetry { claudeAdapter.streamWithTools(messages, options, tools) }.toList()
                agenticLoopOrchestrator.collectStreamResult(events)
            }

            // Persist messages
            persistMessage(sessionId, Message.Role.USER, fullMessage)
            persistMessage(sessionId, Message.Role.ASSISTANT, result.content, result.toolCalls)

            knowledgeGapDetectorService.analyzeForGaps(message, result.content, contexts)

            ChatMessageResponse(result.content, result.toolCalls)
        } catch (e: Exception) {
            logger.error("Claude API call failed: ${e.message}", e)
            val fallback = generateFallbackResponse(message, contexts)
            persistMessage(sessionId, Message.Role.USER, fullMessage)
            persistMessage(sessionId, Message.Role.ASSISTANT, fallback)
            ChatMessageResponse(fallback, emptyList())
        }
    }

    /**
     * Stream a message with real-time events via the agentic loop.
     *
     * Supports multi-turn tool calling: Claude requests tools -> tools execute ->
     * results feed back -> Claude continues. Max turns defined in [AgenticLoopOrchestrator].
     */
    fun streamMessage(
        sessionId: String,
        message: String,
        contexts: List<ContextReference>,
        workspaceId: String,
        onEvent: (Map<String, Any?>) -> Unit,
        onComplete: (ChatMessage) -> Unit,
        onError: (Exception) -> Unit
    ) {
        executor.submit {
            try {
                val messageStartMs = System.currentTimeMillis()
                val fullMessage = buildContextualMessage(message, contexts)
                val history = loadConversationHistory(sessionId)

                val tools = mcpProxyService.listTools().map { tool ->
                    ToolDefinition(
                        name = tool.name,
                        description = tool.description,
                        inputSchema = tool.inputSchema
                    )
                }

                // OODA: Observe -- understanding user intent
                onEvent(mapOf("type" to "ooda_phase", "phase" to "observe",
                    "detail" to "解析用户意图"))
                metricsService.recordOodaPhase("observe")
                emitSubStep(onEvent, "解析用户意图（${message.length} 字符）")

                val promptResult = buildDynamicSystemPrompt(message, workspaceId)
                logger.info("Stream profile: {}, Skills: {}", promptResult.activeProfile, promptResult.loadedSkills)

                val userApiKey = resolveUserApiKey(sessionId)
                val options = CompletionOptions(
                    model = model,
                    maxTokens = 4096,
                    systemPrompt = promptResult.systemPrompt,
                    apiKeyOverride = userApiKey
                )

                // OODA: Orient -- profile routed, context analyzed
                onEvent(mapOf("type" to "ooda_phase", "phase" to "orient",
                    "detail" to "路由到 ${promptResult.activeProfile}"))
                metricsService.recordOodaPhase("orient")
                emitSubStep(onEvent, "路由到 ${promptResult.activeProfile}，加载 ${promptResult.loadedSkills.size} 个 Skills")

                // Emit profile routing info
                onEvent(mapOf(
                    "type" to "profile_active",
                    "activeProfile" to promptResult.activeProfile,
                    "loadedSkills" to promptResult.loadedSkills,
                    "routingReason" to promptResult.routingReason,
                    "confidence" to promptResult.confidence
                ))

                // Persist the user message
                persistMessage(sessionId, Message.Role.USER, fullMessage)

                // OODA: Decide -- Claude formulating response
                onEvent(mapOf("type" to "ooda_phase", "phase" to "decide",
                    "detail" to "AI 制定响应策略"))
                metricsService.recordOodaPhase("decide")
                emitSubStep(onEvent, "组装 system prompt: ${promptResult.systemPrompt.length} 字符")

                // Run the agentic loop
                val result = runBlocking {
                    agenticLoopOrchestrator.agenticStream(
                        messages = history + Message(role = Message.Role.USER, content = fullMessage),
                        options = options,
                        tools = tools,
                        onEvent = onEvent,
                        workspaceId = workspaceId
                    )
                }

                // Baseline auto-check: if code was generated (workspace_write_file used),
                // run profile baselines and retry if they fail (max 2 retries)
                var finalResult = result
                val hasCodeGeneration = result.toolCalls.any { it.name == "workspace_write_file" && it.status != "error" }
                if (hasCodeGeneration && promptResult.activeProfile != "fallback") {
                    finalResult = baselineAutoChecker.runBaselineAutoCheck(
                        result = result,
                        promptResult = promptResult,
                        messages = history + Message(role = Message.Role.USER, content = fullMessage),
                        options = options,
                        tools = tools,
                        workspaceId = workspaceId,
                        onEvent = onEvent
                    )
                }

                // HITL checkpoint: if profile has a hitlCheckpoint defined, pause for approval
                val activeProfileDef = skillLoader.loadProfile(promptResult.activeProfile)
                if (activeProfileDef != null && activeProfileDef.hitlCheckpoint.isNotBlank()) {
                    val deliverables = finalResult.toolCalls
                        .filter { it.name == "workspace_write_file" && it.status != "error" }
                        .mapNotNull { tc -> tc.input["path"] as? String }

                    val decision = hitlCheckpointManager.awaitHitlCheckpoint(
                        sessionId = sessionId,
                        profile = activeProfileDef,
                        deliverables = deliverables,
                        baselineResults = null,
                        onEvent = onEvent
                    )

                    when (decision.action) {
                        HitlAction.REJECT -> {
                            // Terminate: send summary and return
                            val rejectContent = finalResult.content +
                                "\n\n---\n⛔ 用户拒绝了此阶段产出。反馈: ${decision.feedback ?: "无"}"
                            finalResult = AgenticResult(content = rejectContent, toolCalls = finalResult.toolCalls)
                        }
                        HitlAction.MODIFY -> {
                            // Re-enter agentic loop with modified prompt
                            val modifiedMessages = (history + Message(role = Message.Role.USER, content = fullMessage)).toMutableList()
                            modifiedMessages.add(Message(role = Message.Role.ASSISTANT, content = finalResult.content))
                            modifiedMessages.add(Message(role = Message.Role.USER, content = decision.modifiedPrompt ?: decision.feedback ?: "请修改"))

                            emitSubStep(onEvent, "根据修改指令重新执行...")
                            onEvent(mapOf("type" to "ooda_phase", "phase" to "orient", "detail" to "根据修改指令重入"))

                            finalResult = runBlocking {
                                agenticLoopOrchestrator.agenticStream(
                                    messages = modifiedMessages,
                                    options = options,
                                    tools = tools,
                                    onEvent = onEvent,
                                    workspaceId = workspaceId
                                )
                            }
                        }
                        HitlAction.APPROVE -> {
                            // Re-enter agentic loop to continue execution after approval
                            emitSubStep(onEvent, "用户已批准「${activeProfileDef.hitlCheckpoint}」，继续执行...")
                            onEvent(mapOf("type" to "ooda_phase", "phase" to "orient", "detail" to "审批通过，继续执行"))

                            val continueMessages = (history + Message(role = Message.Role.USER, content = fullMessage)).toMutableList()
                            continueMessages.add(Message(role = Message.Role.ASSISTANT, content = finalResult.content))
                            continueMessages.add(Message(role = Message.Role.USER, content =
                                "用户已审批通过「${activeProfileDef.hitlCheckpoint}」。" +
                                (if (decision.feedback.isNullOrBlank()) "" else "用户反馈: ${decision.feedback}。") +
                                "请输出本阶段的完整总结报告，包括：1) 已完成的工作 2) 产出物清单 3) 关键决策 4) 建议的下一步。"
                            ))

                            finalResult = runBlocking {
                                agenticLoopOrchestrator.agenticStream(
                                    messages = continueMessages,
                                    options = options,
                                    tools = tools,
                                    onEvent = onEvent,
                                    workspaceId = workspaceId
                                )
                            }
                        }
                    }
                }

                // OODA: Complete -- response delivered
                onEvent(mapOf("type" to "ooda_phase", "phase" to "complete"))
                metricsService.recordOodaPhase("complete")

                // Record total message duration
                val messageDurationMs = System.currentTimeMillis() - messageStartMs
                metricsService.recordMessageDuration(messageDurationMs)

                // Persist the final assistant response
                persistMessage(sessionId, Message.Role.ASSISTANT, finalResult.content, finalResult.toolCalls)

                knowledgeGapDetectorService.analyzeForGaps(message, finalResult.content, contexts)

                // Persist execution record for quality dashboard
                try {
                    val gson = com.google.gson.Gson()
                    val toolCallsSummary = finalResult.toolCalls.map { tc ->
                        mapOf("name" to tc.name, "status" to tc.status)
                    }
                    executionRecordRepository.save(ExecutionRecordEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        profile = promptResult.activeProfile,
                        skillsLoaded = promptResult.loadedSkills.size,
                        toolCalls = gson.toJson(toolCallsSummary),
                        totalDurationMs = messageDurationMs,
                        totalTurns = finalResult.toolCalls.size.coerceAtLeast(1)
                    ))
                } catch (e: Exception) {
                    logger.warn("Failed to save execution record: {}", e.message)
                }

                // Generate session summary and update memory layers asynchronously (non-blocking)
                executor.submit {
                    try {
                        sessionSummaryService.generateAndSave(
                            sessionId = sessionId,
                            workspaceId = workspaceId,
                            profile = promptResult.activeProfile,
                            conversationHistory = history + listOf(
                                Message(role = Message.Role.USER, content = fullMessage),
                                Message(role = Message.Role.ASSISTANT, content = finalResult.content)
                            ),
                            toolCalls = finalResult.toolCalls
                        )
                        // Update Stage Memory and Workspace Memory from the generated summary
                        memoryContextLoader.updateFromSessionSummary(
                            workspaceId = workspaceId,
                            profile = promptResult.activeProfile,
                            sessionId = sessionId
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to generate session summary: {}", e.message)
                    }
                }

                val assistantMessage = ChatMessage(
                    sessionId = sessionId,
                    role = MessageRole.ASSISTANT,
                    content = finalResult.content,
                    toolCalls = finalResult.toolCalls
                )

                onComplete(assistantMessage)
            } catch (e: Exception) {
                logger.error("Stream message failed: ${e.message}", e)
                onError(e)
            }
        }
    }

    /**
     * Resolve a pending HITL checkpoint (called from WebSocket handler).
     */
    fun resolveCheckpoint(sessionId: String, decision: HitlDecision) {
        hitlCheckpointManager.resolveCheckpoint(sessionId, decision)
    }

    /**
     * Check if a session has a pending HITL checkpoint (for reconnection).
     */
    fun getPendingCheckpoint(sessionId: String): HitlCheckpointEntity? {
        return hitlCheckpointManager.getPendingCheckpoint(sessionId)
    }

    // ---- Persistence helpers ----

    private fun loadConversationHistory(sessionId: String): List<Message> {
        return try {
            chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId).map { entity ->
                Message(
                    role = when (entity.role) {
                        "user" -> Message.Role.USER
                        "assistant" -> Message.Role.ASSISTANT
                        "tool" -> Message.Role.TOOL
                        else -> Message.Role.USER
                    },
                    content = entity.content
                )
            }
        } catch (e: Exception) {
            logger.debug("Could not load history for session $sessionId: ${e.message}")
            emptyList()
        }
    }

    private fun persistMessage(
        sessionId: String,
        role: Message.Role,
        content: String,
        toolCalls: List<ToolCallRecord> = emptyList()
    ) {
        try {
            val entity = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = when (role) {
                    Message.Role.USER -> "user"
                    Message.Role.ASSISTANT -> "assistant"
                    Message.Role.TOOL -> "tool"
                    Message.Role.SYSTEM -> "system"
                },
                content = content,
                createdAt = Instant.now()
            )

            val savedMessage = chatMessageRepository.save(entity)

            // Persist tool calls
            if (toolCalls.isNotEmpty()) {
                for (tc in toolCalls) {
                    val toolCallEntity = ToolCallEntity(
                        id = UUID.randomUUID().toString(),
                        messageId = savedMessage.id,
                        toolName = tc.name,
                        input = tc.input.toString(),
                        output = tc.output,
                        status = tc.status
                    )
                    // Tool call entities are saved via cascade or separate repo
                    savedMessage.toolCalls.add(toolCallEntity)
                }
                chatMessageRepository.save(savedMessage)
            }
        } catch (e: Exception) {
            logger.warn("Failed to persist message for session $sessionId: ${e.message}")
        }
    }

    // ---- Utility methods ----

    private fun buildContextualMessage(
        message: String,
        contexts: List<ContextReference>
    ): String {
        if (contexts.isEmpty()) return message

        val contextBlock = contexts.joinToString("\n\n") { ctx ->
            when (ctx.type) {
                "file" -> "<file path=\"${ctx.id}\">\n${ctx.content ?: "[file content]"}\n</file>"
                "knowledge" -> "<knowledge id=\"${ctx.id}\">\n${ctx.content ?: "[knowledge content]"}\n</knowledge>"
                "schema" -> "<schema id=\"${ctx.id}\">\n${ctx.content ?: "[schema content]"}\n</schema>"
                "service" -> "<service id=\"${ctx.id}\">\n${ctx.content ?: "[service info]"}\n</service>"
                else -> "<context type=\"${ctx.type}\" id=\"${ctx.id}\">\n${ctx.content ?: ""}\n</context>"
            }
        }

        return """
            |Context:
            |$contextBlock
            |
            |User Question:
            |$message
        """.trimMargin()
    }

    private fun generateFallbackResponse(
        message: String,
        contexts: List<ContextReference>
    ): String {
        val contextInfo = if (contexts.isNotEmpty()) {
            "\n\nI can see you've attached ${contexts.size} context item(s). " +
            "Once the Claude API connection is configured, I'll be able to analyze them in detail."
        } else {
            ""
        }

        return """
            |I received your message: "$message"
            |
            |I'm currently running in fallback mode because the Claude API key is not configured.
            |To enable full AI capabilities, please set the `forge.claude.api-key` configuration.
            |$contextInfo
            |
            |In the meantime, you can:
            |- Browse the knowledge base for documentation
            |- Use the file explorer to navigate code
            |- Create and manage workflows
        """.trimMargin()
    }
}

data class ChatMessageResponse(
    val content: String,
    val toolCalls: List<ToolCallRecord>
)
