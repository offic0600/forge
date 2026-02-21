package com.forge.webide.service.learning

import com.forge.webide.entity.ExecutionRecordEntity
import com.forge.webide.repository.ExecutionRecordRepository
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Spring-managed execution logger for the learning loop.
 * Persists execution records to both DB and file system for analysis.
 */
@Service
class ExecutionLoggerService(
    private val executionRecordRepository: ExecutionRecordRepository
) {
    private val logger = LoggerFactory.getLogger(ExecutionLoggerService::class.java)
    private val gson = Gson()
    private val logBaseDir = "logs"

    /**
     * Log an execution record to DB and file system.
     */
    fun logExecution(record: ExecutionRecordEntity): String {
        // Save to DB
        executionRecordRepository.save(record)

        // Write to file system for learning loop analysis
        return try {
            val date = record.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
            val dateDir = File(logBaseDir).resolve(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            dateDir.mkdirs()

            val logFile = dateDir.resolve("exec-${record.id}.json")
            val json = gson.toJson(mapOf(
                "id" to record.id,
                "sessionId" to record.sessionId,
                "profile" to record.profile,
                "skillsLoaded" to record.skillsLoaded,
                "toolCalls" to record.toolCalls,
                "baselineResults" to record.baselineResults,
                "hitlResult" to record.hitlResult,
                "totalDurationMs" to record.totalDurationMs,
                "totalTurns" to record.totalTurns,
                "createdAt" to record.createdAt.toString()
            ))
            logFile.writeText(json)
            logger.debug("Execution logged to {}", logFile.absolutePath)
            logFile.absolutePath
        } catch (e: Exception) {
            logger.warn("Failed to write execution log file: {}", e.message)
            ""
        }
    }

    /**
     * Get recent execution records from DB.
     */
    fun getRecentExecutions(days: Int): List<ExecutionRecordEntity> {
        val since = Instant.now().minus(java.time.Duration.ofDays(days.toLong()))
        return executionRecordRepository.findByCreatedAtAfter(since)
    }

    /**
     * Get execution count since a given time.
     */
    fun countSince(since: Instant): Long {
        return executionRecordRepository.countSince(since)
    }
}
