package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "execution_records")
class ExecutionRecordEntity(
    @Id
    val id: String,

    @Column(name = "session_id", nullable = false)
    val sessionId: String,

    @Column(nullable = false)
    val profile: String,

    @Column(name = "skills_loaded")
    val skillsLoaded: Int = 0,

    @Column(name = "ooda_durations", columnDefinition = "TEXT")
    val oodaDurations: String = "{}",

    @Column(name = "tool_calls", columnDefinition = "TEXT")
    val toolCalls: String = "[]",

    @Column(name = "baseline_results", columnDefinition = "TEXT")
    val baselineResults: String? = null,

    @Column(name = "hitl_result")
    val hitlResult: String? = null,

    @Column(name = "total_duration_ms")
    val totalDurationMs: Long = 0,

    @Column(name = "total_turns")
    val totalTurns: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
