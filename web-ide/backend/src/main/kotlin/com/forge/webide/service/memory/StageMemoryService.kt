package com.forge.webide.service.memory

import com.forge.webide.entity.SessionSummaryEntity
import com.forge.webide.entity.StageMemoryEntity
import com.forge.webide.repository.StageMemoryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Layer 2: Stage Memory — Profile-scoped, cross-session aggregation.
 * Tracks cumulative progress within a delivery stage (e.g., design, development).
 */
@Service
class StageMemoryService(
    private val stageMemoryRepository: StageMemoryRepository
) {
    private val logger = LoggerFactory.getLogger(StageMemoryService::class.java)
    private val gson = Gson()

    fun getStageMemory(workspaceId: String, profile: String): StageMemoryEntity? {
        return stageMemoryRepository.findByWorkspaceIdAndProfile(workspaceId, profile)
    }

    fun getAllStageMemories(workspaceId: String): List<StageMemoryEntity> {
        return stageMemoryRepository.findByWorkspaceId(workspaceId)
    }

    /**
     * Update stage memory from a session summary.
     * Merges completed work and decisions (append), overwrites unresolved and next steps (latest state).
     */
    fun updateFromSummary(workspaceId: String, profile: String, summary: SessionSummaryEntity) {
        val stage = stageMemoryRepository.findByWorkspaceIdAndProfile(workspaceId, profile)
            ?: StageMemoryEntity(workspaceId = workspaceId, profile = profile)

        stage.completedWork = mergeJsonArrays(stage.completedWork, summary.completedWork)
        stage.keyDecisions = mergeJsonArrays(stage.keyDecisions, summary.decisions)
        stage.unresolvedIssues = summary.unresolved  // Overwrite with latest
        stage.nextSteps = summary.nextSteps           // Overwrite with latest
        stage.sessionCount += 1
        stage.updatedAt = Instant.now()

        stageMemoryRepository.save(stage)
        logger.info("Stage memory updated: workspace={}, profile={}, sessions={}",
            workspaceId, profile, stage.sessionCount)
    }

    /**
     * Format stage memory for system prompt injection.
     */
    fun formatForPrompt(entity: StageMemoryEntity): String {
        val completedWork = parseJsonArray(entity.completedWork)
        val keyDecisions = parseJsonArray(entity.keyDecisions)
        val unresolvedIssues = parseJsonArray(entity.unresolvedIssues)
        val nextSteps = parseJsonArray(entity.nextSteps)

        return buildString {
            appendLine("**Profile**: ${entity.profile} | **Sessions**: ${entity.sessionCount}")
            if (completedWork.isNotEmpty()) {
                appendLine()
                appendLine("**已完成工作**:")
                completedWork.takeLast(10).forEach { appendLine("- $it") }
            }
            if (keyDecisions.isNotEmpty()) {
                appendLine()
                appendLine("**关键决策**:")
                keyDecisions.takeLast(8).forEach { appendLine("- $it") }
            }
            if (unresolvedIssues.isNotEmpty()) {
                appendLine()
                appendLine("**未解决问题**:")
                unresolvedIssues.forEach { appendLine("- $it") }
            }
            if (nextSteps.isNotEmpty()) {
                appendLine()
                appendLine("**下一步**:")
                nextSteps.forEach { appendLine("- $it") }
            }
        }
    }

    // ---- Internal ----

    /**
     * Merge two JSON arrays, deduplicating entries.
     */
    private fun mergeJsonArrays(existing: String, newItems: String): String {
        val existingList = parseJsonArray(existing)
        val newList = parseJsonArray(newItems)
        val merged = (existingList + newList).distinct()
        return gson.toJson(merged)
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
