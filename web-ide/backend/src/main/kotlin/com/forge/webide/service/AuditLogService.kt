package com.forge.webide.service

import com.forge.webide.entity.AuditLogEntity
import com.forge.webide.repository.AuditLogRepository
import org.springframework.stereotype.Service

@Service
class AuditLogService(private val repo: AuditLogRepository) {
    fun log(
        orgId: String?,
        actorId: String,
        action: String,
        targetType: String? = null,
        targetId: String? = null,
        detail: String? = null
    ) {
        try {
            repo.save(
                AuditLogEntity(
                    orgId = orgId,
                    actorId = actorId,
                    action = action,
                    targetType = targetType,
                    targetId = targetId,
                    detail = detail
                )
            )
        } catch (e: Exception) {
            // 审计失败不阻断主流程
        }
    }
}
