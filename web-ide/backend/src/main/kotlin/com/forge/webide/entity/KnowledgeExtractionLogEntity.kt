package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "knowledge_extraction_logs")
class KnowledgeExtractionLogEntity(
    @Id
    val id: String,

    @Column(name = "job_id", nullable = false, length = 50)
    val jobId: String,

    @Column(name = "workspace_id", nullable = false, length = 100)
    val workspaceId: String = "",

    @Column(name = "tag_id", nullable = false, length = 50)
    val tagId: String,

    @Column(name = "tag_name", nullable = false, length = 200)
    val tagName: String,

    @Column(nullable = false, length = 20)
    val phase: String,

    @Column(nullable = false, length = 20)
    val status: String,

    @Column(nullable = false)
    val applicable: Boolean = true,

    @Column(columnDefinition = "TEXT")
    val reason: String? = null,

    @Column(name = "content_length", nullable = false)
    val contentLength: Int = 0,

    @Column(name = "tokens_used", nullable = false)
    val tokensUsed: Int = 0,

    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long = 0,

    @Column(name = "model_used", length = 100)
    val modelUsed: String? = null,

    @Column(name = "source_files", columnDefinition = "TEXT")
    val sourceFiles: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
