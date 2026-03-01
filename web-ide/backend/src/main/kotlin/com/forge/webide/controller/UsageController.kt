package com.forge.webide.controller

import com.forge.webide.entity.AuditLogEntity
import com.forge.webide.entity.OrganizationEntity
import com.forge.webide.model.AuditLogEntry
import com.forge.webide.model.Organization
import com.forge.webide.model.OrgUsageSummary
import com.forge.webide.model.UpdateQuotaRequest
import com.forge.webide.repository.AuditLogRepository
import com.forge.webide.repository.OrganizationRepository
import com.forge.webide.service.AuditLogService
import com.forge.webide.service.RbacHelper
import com.forge.webide.service.UsageService
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/admin")
class UsageController(
    private val usageService: UsageService,
    private val auditLogRepo: AuditLogRepository,
    private val orgRepo: OrganizationRepository,
    private val rbacHelper: RbacHelper,
    private val auditLogService: AuditLogService
) {

    @GetMapping("/orgs/{orgId}/usage")
    fun getOrgUsage(
        @PathVariable orgId: String,
        @RequestParam(defaultValue = "7") days: Int,
        @AuthenticationPrincipal jwt: Jwt? = null
    ): ResponseEntity<OrgUsageSummary> {
        rbacHelper.requireOrgAdmin(jwt, orgId)
        return ResponseEntity.ok(usageService.getOrgUsage(orgId, days))
    }

    @GetMapping("/orgs/{orgId}/audit-logs")
    fun listOrgAuditLogs(
        @PathVariable orgId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @AuthenticationPrincipal jwt: Jwt? = null
    ): ResponseEntity<Map<String, Any>> {
        rbacHelper.requireOrgAdmin(jwt, orgId)
        val pageable = PageRequest.of(page, size)
        val result = auditLogRepo.findByOrgIdOrderByCreatedAtDesc(orgId, pageable)
        return ResponseEntity.ok(
            mapOf(
                "content" to result.content.map { it.toEntry() },
                "totalPages" to result.totalPages,
                "totalElements" to result.totalElements
            )
        )
    }

    @GetMapping("/audit-logs")
    fun listAllAuditLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int,
        @AuthenticationPrincipal jwt: Jwt? = null
    ): ResponseEntity<Map<String, Any>> {
        rbacHelper.requireSystemAdmin(jwt)
        val pageable = PageRequest.of(page, size)
        val result = auditLogRepo.findAllByOrderByCreatedAtDesc(pageable)
        return ResponseEntity.ok(
            mapOf(
                "content" to result.content.map { it.toEntry() },
                "totalPages" to result.totalPages,
                "totalElements" to result.totalElements
            )
        )
    }

    @PutMapping("/orgs/{orgId}/quota")
    fun updateQuota(
        @PathVariable orgId: String,
        @RequestBody req: UpdateQuotaRequest,
        @AuthenticationPrincipal jwt: Jwt? = null
    ): ResponseEntity<Organization> {
        rbacHelper.requireSystemAdmin(jwt)
        val org = orgRepo.findById(orgId).orElse(null) ?: return ResponseEntity.notFound().build()
        org.monthlyMessageQuota = req.monthlyMessageQuota
        org.monthlyExecQuota = req.monthlyExecQuota
        orgRepo.save(org)
        val actorId = jwt?.subject ?: "system"
        auditLogService.log(
            orgId, actorId, "QUOTA_UPDATED", "ORGANIZATION", orgId,
            "msgQuota=${req.monthlyMessageQuota}, execQuota=${req.monthlyExecQuota}"
        )
        return ResponseEntity.ok(org.toModel())
    }

    private fun AuditLogEntity.toEntry() = AuditLogEntry(
        id = id, orgId = orgId, actorId = actorId,
        action = action, targetType = targetType, targetId = targetId,
        detail = detail, createdAt = createdAt
    )

    private fun OrganizationEntity.toModel() = Organization(
        id = id, name = name, slug = slug, description = description,
        status = status, createdAt = createdAt,
        monthlyMessageQuota = monthlyMessageQuota, monthlyExecQuota = monthlyExecQuota
    )
}
