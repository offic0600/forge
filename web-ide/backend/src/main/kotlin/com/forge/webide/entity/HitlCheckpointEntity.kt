package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "hitl_checkpoints")
class HitlCheckpointEntity(
    @Id
    val id: String,

    @Column(name = "session_id", nullable = false)
    val sessionId: String,

    @Column(nullable = false)
    val profile: String,

    @Column(nullable = false)
    val checkpoint: String,

    @Column(columnDefinition = "TEXT")
    val deliverables: String = "[]",

    @Column(name = "baseline_results", columnDefinition = "TEXT")
    val baselineResults: String? = null,

    @Column(nullable = false)
    var status: String = "PENDING",

    @Column(columnDefinition = "TEXT")
    var feedback: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null
)
