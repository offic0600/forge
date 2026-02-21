package com.forge.webide.repository

import com.forge.webide.entity.ExecutionRecordEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ExecutionRecordRepository : JpaRepository<ExecutionRecordEntity, String> {

    fun findBySessionId(sessionId: String): List<ExecutionRecordEntity>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): List<ExecutionRecordEntity>

    fun findByCreatedAtAfter(since: Instant): List<ExecutionRecordEntity>

    @Query("SELECT COUNT(e) FROM ExecutionRecordEntity e WHERE e.createdAt >= :since")
    fun countSince(since: Instant): Long
}
