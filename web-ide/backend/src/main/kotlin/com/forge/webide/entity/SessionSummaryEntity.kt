package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "session_summaries")
class SessionSummaryEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "session_id", nullable = false, unique = true)
    val sessionId: String,

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: String,

    @Column(name = "profile", nullable = false)
    val profile: String,

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    var summary: String,

    @Column(name = "completed_work", columnDefinition = "TEXT")
    var completedWork: String = "[]",

    @Column(name = "artifacts", columnDefinition = "TEXT")
    var artifacts: String = "[]",

    @Column(name = "decisions", columnDefinition = "TEXT")
    var decisions: String = "[]",

    @Column(name = "unresolved", columnDefinition = "TEXT")
    var unresolved: String = "[]",

    @Column(name = "next_steps", columnDefinition = "TEXT")
    var nextSteps: String = "[]",

    @Column(name = "turn_count")
    val turnCount: Int = 0,

    @Column(name = "tool_call_count")
    val toolCallCount: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
