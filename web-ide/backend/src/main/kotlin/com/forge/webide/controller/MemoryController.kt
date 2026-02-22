package com.forge.webide.controller

import com.forge.webide.service.memory.SessionSummaryService
import com.forge.webide.service.memory.StageMemoryService
import com.forge.webide.service.memory.WorkspaceMemoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST API for the 3-layer memory architecture.
 */
@RestController
@RequestMapping("/api/memory")
class MemoryController(
    private val workspaceMemoryService: WorkspaceMemoryService,
    private val stageMemoryService: StageMemoryService,
    private val sessionSummaryService: SessionSummaryService
) {

    // ---- Layer 1: Workspace Memory ----

    @GetMapping("/workspace/{workspaceId}")
    fun getWorkspaceMemory(@PathVariable workspaceId: String): ResponseEntity<WorkspaceMemoryResponse> {
        val content = workspaceMemoryService.getMemory(workspaceId)
        return ResponseEntity.ok(WorkspaceMemoryResponse(workspaceId = workspaceId, content = content))
    }

    @PutMapping("/workspace/{workspaceId}")
    fun updateWorkspaceMemory(
        @PathVariable workspaceId: String,
        @RequestBody request: UpdateWorkspaceMemoryRequest
    ): ResponseEntity<WorkspaceMemoryResponse> {
        workspaceMemoryService.updateMemory(workspaceId, request.content)
        return ResponseEntity.ok(WorkspaceMemoryResponse(
            workspaceId = workspaceId,
            content = workspaceMemoryService.getMemory(workspaceId)
        ))
    }

    // ---- Layer 2: Stage Memory ----

    @GetMapping("/stage/{workspaceId}")
    fun getAllStageMemories(@PathVariable workspaceId: String): ResponseEntity<List<StageMemoryResponse>> {
        val stages = stageMemoryService.getAllStageMemories(workspaceId).map { entity ->
            StageMemoryResponse(
                workspaceId = entity.workspaceId,
                profile = entity.profile,
                completedWork = entity.completedWork,
                keyDecisions = entity.keyDecisions,
                unresolvedIssues = entity.unresolvedIssues,
                nextSteps = entity.nextSteps,
                sessionCount = entity.sessionCount,
                updatedAt = entity.updatedAt.toString()
            )
        }
        return ResponseEntity.ok(stages)
    }

    @GetMapping("/stage/{workspaceId}/{profile}")
    fun getStageMemory(
        @PathVariable workspaceId: String,
        @PathVariable profile: String
    ): ResponseEntity<StageMemoryResponse> {
        val entity = stageMemoryService.getStageMemory(workspaceId, profile)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(StageMemoryResponse(
            workspaceId = entity.workspaceId,
            profile = entity.profile,
            completedWork = entity.completedWork,
            keyDecisions = entity.keyDecisions,
            unresolvedIssues = entity.unresolvedIssues,
            nextSteps = entity.nextSteps,
            sessionCount = entity.sessionCount,
            updatedAt = entity.updatedAt.toString()
        ))
    }

    // ---- Layer 3: Session Summaries ----

    @GetMapping("/sessions/{workspaceId}")
    fun getSessionSummaries(
        @PathVariable workspaceId: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<SessionSummaryResponse>> {
        val summaries = sessionSummaryService.getRecentSummaries(workspaceId, limit).map { entity ->
            SessionSummaryResponse(
                id = entity.id,
                sessionId = entity.sessionId,
                workspaceId = entity.workspaceId,
                profile = entity.profile,
                summary = entity.summary,
                completedWork = entity.completedWork,
                artifacts = entity.artifacts,
                decisions = entity.decisions,
                unresolved = entity.unresolved,
                nextSteps = entity.nextSteps,
                turnCount = entity.turnCount,
                toolCallCount = entity.toolCallCount,
                createdAt = entity.createdAt.toString()
            )
        }
        return ResponseEntity.ok(summaries)
    }

    @GetMapping("/sessions/{workspaceId}/{sessionId}")
    fun getSessionSummary(
        @PathVariable workspaceId: String,
        @PathVariable sessionId: String
    ): ResponseEntity<SessionSummaryResponse> {
        val entity = sessionSummaryService.getSummary(sessionId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(SessionSummaryResponse(
            id = entity.id,
            sessionId = entity.sessionId,
            workspaceId = entity.workspaceId,
            profile = entity.profile,
            summary = entity.summary,
            completedWork = entity.completedWork,
            artifacts = entity.artifacts,
            decisions = entity.decisions,
            unresolved = entity.unresolved,
            nextSteps = entity.nextSteps,
            turnCount = entity.turnCount,
            toolCallCount = entity.toolCallCount,
            createdAt = entity.createdAt.toString()
        ))
    }
}

// ---- Response DTOs ----

data class WorkspaceMemoryResponse(
    val workspaceId: String,
    val content: String
)

data class UpdateWorkspaceMemoryRequest(
    val content: String
)

data class StageMemoryResponse(
    val workspaceId: String,
    val profile: String,
    val completedWork: String,
    val keyDecisions: String,
    val unresolvedIssues: String,
    val nextSteps: String,
    val sessionCount: Int,
    val updatedAt: String
)

data class SessionSummaryResponse(
    val id: String,
    val sessionId: String,
    val workspaceId: String,
    val profile: String,
    val summary: String,
    val completedWork: String,
    val artifacts: String,
    val decisions: String,
    val unresolved: String,
    val nextSteps: String,
    val turnCount: Int,
    val toolCallCount: Int,
    val createdAt: String
)
