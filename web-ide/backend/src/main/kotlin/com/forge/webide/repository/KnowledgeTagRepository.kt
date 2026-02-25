package com.forge.webide.repository

import com.forge.webide.entity.KnowledgeTagEntity
import org.springframework.data.jpa.repository.JpaRepository

interface KnowledgeTagRepository : JpaRepository<KnowledgeTagEntity, String> {
    fun findAllByOrderBySortOrderAsc(): List<KnowledgeTagEntity>
    fun findByStatus(status: String): List<KnowledgeTagEntity>
    fun findByNameContainingIgnoreCaseOrContentContainingIgnoreCase(
        name: String,
        content: String
    ): List<KnowledgeTagEntity>
    fun findByWorkspaceIdOrderBySortOrderAsc(workspaceId: String): List<KnowledgeTagEntity>
    fun findByWorkspaceIdIsNullOrderBySortOrderAsc(): List<KnowledgeTagEntity>
    fun countByWorkspaceId(workspaceId: String): Long
    fun deleteByWorkspaceId(workspaceId: String)
}
