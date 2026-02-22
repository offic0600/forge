package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "stage_memories",
    uniqueConstraints = [UniqueConstraint(columnNames = ["workspace_id", "profile"])]
)
class StageMemoryEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "workspace_id", nullable = false)
    val workspaceId: String,

    @Column(name = "profile", nullable = false)
    val profile: String,

    @Column(name = "completed_work", columnDefinition = "TEXT")
    var completedWork: String = "[]",

    @Column(name = "key_decisions", columnDefinition = "TEXT")
    var keyDecisions: String = "[]",

    @Column(name = "unresolved_issues", columnDefinition = "TEXT")
    var unresolvedIssues: String = "[]",

    @Column(name = "next_steps", columnDefinition = "TEXT")
    var nextSteps: String = "[]",

    @Column(name = "session_count")
    var sessionCount: Int = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
