package com.forge.webide.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "org_invitations")
class OrgInvitationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 128)
    val token: String,

    @Column(name = "org_id", nullable = false, length = 36)
    val orgId: String,

    @Column(nullable = false, length = 32)
    val role: String = "MEMBER",

    @Column(name = "created_by", nullable = false, length = 255)
    val createdBy: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "used_by", length = 255)
    var usedBy: String? = null,

    @Column(name = "used_at")
    var usedAt: Instant? = null
)
