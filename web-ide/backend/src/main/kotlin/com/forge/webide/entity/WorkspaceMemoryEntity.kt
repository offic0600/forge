package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "workspace_memories")
class WorkspaceMemoryEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "workspace_id", nullable = false, unique = true)
    val workspaceId: String,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    var content: String = "",

    @Column(name = "version")
    var version: Int = 1,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
