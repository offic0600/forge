package com.forge.webide.service.memory

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Unified loader for the 3-layer memory architecture.
 * Loads all memory layers and assembles a MemoryContext for system prompt injection.
 */
@Service
class MemoryContextLoader(
    private val workspaceMemoryService: WorkspaceMemoryService,
    private val stageMemoryService: StageMemoryService,
    private val sessionSummaryService: SessionSummaryService
) {
    private val logger = LoggerFactory.getLogger(MemoryContextLoader::class.java)

    /**
     * Load all 3 memory layers for the given workspace and profile.
     */
    fun loadMemoryContext(workspaceId: String, profile: String): MemoryContext {
        // Layer 1: Workspace Memory (always loaded)
        val workspaceMemory = try {
            workspaceMemoryService.getMemory(workspaceId)
        } catch (e: Exception) {
            logger.debug("Failed to load workspace memory: {}", e.message)
            ""
        }

        // Layer 2: Stage Memory (profile-scoped)
        val stageMemory = try {
            stageMemoryService.getStageMemory(workspaceId, profile)?.let {
                stageMemoryService.formatForPrompt(it)
            } ?: ""
        } catch (e: Exception) {
            logger.debug("Failed to load stage memory: {}", e.message)
            ""
        }

        // Layer 3: Recent Session Summaries (last 3)
        val recentSessions = try {
            sessionSummaryService.getRecentSummaries(workspaceId, 3).map {
                sessionSummaryService.formatSummaryForPrompt(it)
            }
        } catch (e: Exception) {
            logger.debug("Failed to load session summaries: {}", e.message)
            emptyList()
        }

        logger.debug(
            "Memory context loaded: workspace={}c, stage={}c, sessions={}",
            workspaceMemory.length, stageMemory.length, recentSessions.size
        )

        return MemoryContext(
            workspaceMemory = workspaceMemory,
            stageMemory = stageMemory,
            recentSessions = recentSessions
        )
    }

    /**
     * Post-session hook: update Stage Memory and Workspace Memory from a session summary.
     */
    fun updateFromSessionSummary(workspaceId: String, profile: String, sessionId: String) {
        val summary = sessionSummaryService.getSummary(sessionId) ?: return

        try {
            stageMemoryService.updateFromSummary(workspaceId, profile, summary)
        } catch (e: Exception) {
            logger.warn("Failed to update stage memory from summary: {}", e.message)
        }

        try {
            workspaceMemoryService.appendFromSummary(workspaceId, summary)
        } catch (e: Exception) {
            logger.warn("Failed to update workspace memory from summary: {}", e.message)
        }
    }
}

/**
 * Container for the 3-layer memory context to be injected into system prompt.
 */
data class MemoryContext(
    val workspaceMemory: String = "",
    val stageMemory: String = "",
    val recentSessions: List<String> = emptyList()
)
