package com.forge.webide.repository

import com.forge.webide.entity.AuditLogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLogEntity, Long> {
    fun findByOrgIdOrderByCreatedAtDesc(orgId: String, pageable: Pageable): Page<AuditLogEntity>
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<AuditLogEntity>
}
