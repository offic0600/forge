package com.forge.webide.controller

import com.forge.webide.repository.ExecutionRecordRepository
import com.forge.webide.repository.HitlCheckpointRepository
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val executionRecordRepository: ExecutionRecordRepository,
    private val hitlCheckpointRepository: HitlCheckpointRepository
) {

    /**
     * GET /api/dashboard/metrics — aggregated statistics.
     */
    @GetMapping("/metrics")
    fun getMetrics(): Map<String, Any> {
        val since7d = Instant.now().minus(7, ChronoUnit.DAYS)
        val records = executionRecordRepository.findByCreatedAtAfter(since7d)
        val hitlRecords = hitlCheckpointRepository.findAll()

        // Profile stats
        val profileStats = records.groupBy { it.profile }.map { (name, recs) ->
            mapOf(
                "name" to name,
                "count" to recs.size,
                "avgDurationMs" to if (recs.isNotEmpty()) recs.map { it.totalDurationMs }.average().toLong() else 0L,
                "avgTurns" to if (recs.isNotEmpty()) recs.map { it.totalTurns }.average() else 0.0
            )
        }

        // Tool call stats (from JSON in records)
        val toolCounts = mutableMapOf<String, Int>()
        for (record in records) {
            try {
                val tools = com.google.gson.Gson().fromJson(record.toolCalls, List::class.java)
                @Suppress("UNCHECKED_CAST")
                for (tool in tools as List<Map<String, Any>>) {
                    val name = tool["name"] as? String ?: continue
                    toolCounts[name] = (toolCounts[name] ?: 0) + 1
                }
            } catch (_: Exception) { /* skip malformed JSON */ }
        }
        val toolCallStats = toolCounts.entries.sortedByDescending { it.value }
            .take(10)
            .map { mapOf("name" to it.key, "count" to it.value) }

        // HITL stats
        val hitlStats = mapOf(
            "total" to hitlRecords.size,
            "approved" to hitlRecords.count { it.status == "APPROVED" || it.status == "APPROVE" },
            "rejected" to hitlRecords.count { it.status == "REJECTED" || it.status == "REJECT" },
            "timeout" to hitlRecords.count { it.status == "TIMEOUT" },
            "modified" to hitlRecords.count { it.status == "MODIFY" || it.status == "MODIFIED" },
            "pending" to hitlRecords.count { it.status == "PENDING" }
        )

        return mapOf(
            "profileStats" to profileStats,
            "toolCallStats" to toolCallStats,
            "hitlStats" to hitlStats,
            "totalSessions" to records.size,
            "avgDurationMs" to if (records.isNotEmpty()) records.map { it.totalDurationMs }.average().toLong() else 0L
        )
    }

    /**
     * GET /api/dashboard/executions?limit=20 — recent execution records.
     */
    @GetMapping("/executions")
    fun getExecutions(@RequestParam(defaultValue = "20") limit: Int): List<Map<String, Any?>> {
        val records = executionRecordRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(0, limit.coerceIn(1, 100))
        )
        return records.map { r ->
            mapOf(
                "id" to r.id,
                "sessionId" to r.sessionId,
                "profile" to r.profile,
                "skillsLoaded" to r.skillsLoaded,
                "totalDurationMs" to r.totalDurationMs,
                "totalTurns" to r.totalTurns,
                "hitlResult" to r.hitlResult,
                "baselineResults" to r.baselineResults,
                "createdAt" to r.createdAt.toString()
            )
        }
    }

    /**
     * GET /api/dashboard/trends?days=7 — daily trends.
     */
    @GetMapping("/trends")
    fun getTrends(@RequestParam(defaultValue = "7") days: Int): List<Map<String, Any>> {
        val now = Instant.now()
        val since = now.minus(days.coerceIn(1, 30).toLong(), ChronoUnit.DAYS)
        val records = executionRecordRepository.findByCreatedAtAfter(since)

        // Group by date
        val byDate = records.groupBy { r ->
            r.createdAt.truncatedTo(ChronoUnit.DAYS).toString().substringBefore("T")
        }

        return (0 until days).map { d ->
            val date = now.minus(d.toLong(), ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS).toString().substringBefore("T")
            val dayRecords = byDate[date] ?: emptyList()
            mapOf(
                "date" to date,
                "sessions" to dayRecords.size,
                "avgDurationMs" to if (dayRecords.isNotEmpty()) dayRecords.map { it.totalDurationMs }.average().toLong() else 0L
            )
        }.reversed()
    }
}
