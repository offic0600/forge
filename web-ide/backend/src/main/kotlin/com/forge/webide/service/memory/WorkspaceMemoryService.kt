package com.forge.webide.service.memory

import com.forge.webide.entity.SessionSummaryEntity
import com.forge.webide.entity.WorkspaceMemoryEntity
import com.forge.webide.repository.WorkspaceMemoryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Layer 1: Workspace Memory — always injected into system prompt.
 * Stores workspace-level persistent knowledge (project facts, tech stack, key constraints).
 */
@Service
class WorkspaceMemoryService(
    private val workspaceMemoryRepository: WorkspaceMemoryRepository
) {
    private val logger = LoggerFactory.getLogger(WorkspaceMemoryService::class.java)
    private val gson = Gson()

    companion object {
        private const val MAX_CONTENT_CHARS = 4000
    }

    fun getMemory(workspaceId: String): String {
        return workspaceMemoryRepository.findByWorkspaceId(workspaceId)?.content ?: ""
    }

    fun updateMemory(workspaceId: String, content: String) {
        val entity = workspaceMemoryRepository.findByWorkspaceId(workspaceId)
        if (entity != null) {
            entity.content = content.take(MAX_CONTENT_CHARS)
            entity.version += 1
            entity.updatedAt = Instant.now()
            workspaceMemoryRepository.save(entity)
            logger.info("Workspace memory updated: workspace={}, version={}, length={}",
                workspaceId, entity.version, entity.content.length)
        } else {
            workspaceMemoryRepository.save(WorkspaceMemoryEntity(
                workspaceId = workspaceId,
                content = content.take(MAX_CONTENT_CHARS)
            ))
            logger.info("Workspace memory created: workspace={}, length={}",
                workspaceId, content.take(MAX_CONTENT_CHARS).length)
        }
    }

    /**
     * Append key decisions from a session summary to workspace memory.
     * Only appends new facts, respecting the 4K char limit.
     */
    fun appendFromSummary(workspaceId: String, summary: SessionSummaryEntity) {
        val current = getMemory(workspaceId)
        val decisions = parseJsonArray(summary.decisions)
        if (decisions.isEmpty()) return

        val newFacts = buildString {
            appendLine()
            appendLine("### 决策 (${summary.createdAt.toString().take(10)})")
            for (decision in decisions) {
                appendLine("- $decision")
            }
        }

        val updated = current + newFacts
        if (updated.length <= MAX_CONTENT_CHARS) {
            updateMemory(workspaceId, updated)
        } else {
            logger.debug("Workspace memory at capacity, skipping append for workspace {}", workspaceId)
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
