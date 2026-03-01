package com.forge.webide.repository

import com.forge.webide.entity.ChatMessageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, String> {

    fun findBySessionIdOrderByCreatedAt(sessionId: String): List<ChatMessageEntity>

    @Query("""
        SELECT COUNT(m) FROM ChatMessageEntity m, ChatSessionEntity s, WorkspaceEntity w
        WHERE m.sessionId = s.id AND s.workspaceId = w.id AND w.orgId = :orgId AND m.createdAt >= :since
    """)
    fun countByOrgSince(@Param("orgId") orgId: String, @Param("since") since: Instant): Long

    @Query("""
        SELECT m.createdAt FROM ChatMessageEntity m, ChatSessionEntity s, WorkspaceEntity w
        WHERE m.sessionId = s.id AND s.workspaceId = w.id AND w.orgId = :orgId AND m.createdAt >= :since
    """)
    fun findTimestampsByOrg(@Param("orgId") orgId: String, @Param("since") since: Instant): List<Instant>
}
