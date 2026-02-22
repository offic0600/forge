package com.forge.webide.service

import com.forge.webide.entity.HitlCheckpointEntity
import com.forge.webide.repository.HitlCheckpointRepository
import com.forge.webide.service.skill.HitlAction
import com.forge.webide.service.skill.HitlDecision
import com.forge.webide.service.skill.ProfileDefinition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages Human-In-The-Loop (HITL) checkpoints.
 *
 * Handles checkpoint creation, persistence, waiting for approval,
 * and resolution from the WebSocket handler.
 */
@Service
class HitlCheckpointManager(
    private val hitlCheckpointRepository: HitlCheckpointRepository,
    private val metricsService: MetricsService
) {
    private val logger = LoggerFactory.getLogger(HitlCheckpointManager::class.java)

    // HITL checkpoint futures: sessionId -> CompletableFuture<HitlDecision>
    private val pendingCheckpoints = ConcurrentHashMap<String, CompletableFuture<HitlDecision>>()

    companion object {
        const val HITL_TIMEOUT_SECONDS = 300L
    }

    /**
     * Resolve a pending HITL checkpoint (called from WebSocket handler).
     */
    fun resolveCheckpoint(sessionId: String, decision: HitlDecision) {
        val future = pendingCheckpoints[sessionId]
        if (future != null) {
            future.complete(decision)
            logger.info("HITL checkpoint resolved for session $sessionId: ${decision.action}")
        } else {
            logger.warn("No pending HITL checkpoint for session $sessionId")
        }
    }

    /**
     * Check if a session has a pending HITL checkpoint (for reconnection).
     */
    fun getPendingCheckpoint(sessionId: String): HitlCheckpointEntity? {
        return hitlCheckpointRepository.findBySessionIdAndStatus(sessionId, "PENDING").firstOrNull()
    }

    /**
     * Trigger HITL checkpoint: emit event, persist state, wait for approval.
     * Returns the decision (APPROVE/REJECT/MODIFY) or TIMEOUT after 5 minutes.
     */
    fun awaitHitlCheckpoint(
        sessionId: String,
        profile: ProfileDefinition,
        deliverables: List<String>,
        baselineResults: List<Map<String, String>>?,
        onEvent: (Map<String, Any?>) -> Unit
    ): HitlDecision {
        val checkpointId = UUID.randomUUID().toString()

        // Persist to DB
        val entity = HitlCheckpointEntity(
            id = checkpointId,
            sessionId = sessionId,
            profile = profile.name,
            checkpoint = profile.hitlCheckpoint,
            deliverables = com.google.gson.Gson().toJson(deliverables),
            baselineResults = baselineResults?.let { com.google.gson.Gson().toJson(it) },
            status = "PENDING"
        )
        hitlCheckpointRepository.save(entity)

        // Emit checkpoint event to frontend
        onEvent(mapOf(
            "type" to "hitl_checkpoint",
            "status" to "awaiting_approval",
            "profile" to profile.name,
            "checkpoint" to profile.hitlCheckpoint,
            "deliverables" to deliverables,
            "baselineResults" to (baselineResults ?: emptyList<Map<String, String>>()),
            "timeoutSeconds" to HITL_TIMEOUT_SECONDS
        ))
        emitSubStep(onEvent, "等待用户审批: ${profile.hitlCheckpoint}")

        // Create and register future
        val future = CompletableFuture<HitlDecision>()
        pendingCheckpoints[sessionId] = future

        return try {
            val decision = future.get(HITL_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            // Update DB
            entity.status = decision.action.name
            entity.feedback = decision.feedback
            entity.resolvedAt = Instant.now()
            hitlCheckpointRepository.save(entity)

            metricsService.recordHitlResult(profile.name, decision.action.name)
            emitSubStep(onEvent, "审批结果: ${decision.action}${if (decision.feedback != null) " — ${decision.feedback}" else ""}")

            onEvent(mapOf(
                "type" to "hitl_checkpoint",
                "status" to decision.action.name.lowercase(),
                "profile" to profile.name,
                "hitlFeedback" to (decision.feedback ?: "")
            ))

            decision
        } catch (e: java.util.concurrent.TimeoutException) {
            entity.status = "TIMEOUT"
            entity.resolvedAt = Instant.now()
            hitlCheckpointRepository.save(entity)

            metricsService.recordHitlResult(profile.name, "TIMEOUT")
            emitSubStep(onEvent, "审批超时 (${HITL_TIMEOUT_SECONDS}s)，自动继续")

            onEvent(mapOf(
                "type" to "hitl_checkpoint",
                "status" to "timeout",
                "profile" to profile.name
            ))

            HitlDecision(action = HitlAction.APPROVE, feedback = "Auto-approved due to timeout")
        } finally {
            pendingCheckpoints.remove(sessionId)
        }
    }
}
