package com.forge.webide.repository

import com.forge.webide.entity.StageMemoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StageMemoryRepository : JpaRepository<StageMemoryEntity, String> {

    fun findByWorkspaceIdAndProfile(workspaceId: String, profile: String): StageMemoryEntity?

    fun findByWorkspaceId(workspaceId: String): List<StageMemoryEntity>
}
