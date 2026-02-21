package com.forge.webide.repository

import com.forge.webide.entity.HitlCheckpointEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HitlCheckpointRepository : JpaRepository<HitlCheckpointEntity, String> {

    fun findBySessionIdAndStatus(sessionId: String, status: String): List<HitlCheckpointEntity>

    fun findBySessionId(sessionId: String): List<HitlCheckpointEntity>

    fun findByStatus(status: String): List<HitlCheckpointEntity>
}
