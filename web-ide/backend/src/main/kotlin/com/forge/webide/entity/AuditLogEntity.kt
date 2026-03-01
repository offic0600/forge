package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "audit_logs")
data class AuditLogEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    val orgId: String? = null,
    val actorId: String,
    val action: String,
    val targetType: String? = null,
    val targetId: String? = null,
    val detail: String? = null,
    val createdAt: Instant = Instant.now()
)
