package com.forge.webide.repository

import com.forge.webide.entity.SessionSummaryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SessionSummaryRepository : JpaRepository<SessionSummaryEntity, String> {

    fun findBySessionId(sessionId: String): SessionSummaryEntity?

    fun findByWorkspaceIdOrderByCreatedAtDesc(workspaceId: String): List<SessionSummaryEntity>

    fun findByWorkspaceIdAndProfileOrderByCreatedAtDesc(
        workspaceId: String,
        profile: String
    ): List<SessionSummaryEntity>
}
