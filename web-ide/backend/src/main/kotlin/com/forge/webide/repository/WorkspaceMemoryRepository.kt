package com.forge.webide.repository

import com.forge.webide.entity.WorkspaceMemoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceMemoryRepository : JpaRepository<WorkspaceMemoryEntity, String> {

    fun findByWorkspaceId(workspaceId: String): WorkspaceMemoryEntity?
}
