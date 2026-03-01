package com.forge.webide.service

import com.forge.webide.model.OrgUsageSummary
import com.forge.webide.repository.ChatMessageRepository
import com.forge.webide.repository.ExecutionRecordRepository
import com.forge.webide.repository.OrganizationRepository
import com.forge.webide.repository.WorkspaceRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
class UsageService(
    private val chatMsgRepo: ChatMessageRepository,
    private val execRepo: ExecutionRecordRepository,
    private val workspaceRepo: WorkspaceRepository,
    private val orgRepo: OrganizationRepository
) {
    fun getOrgUsage(orgId: String, days: Int = 7): OrgUsageSummary {
        val since = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)

        val msgTotal = chatMsgRepo.countByOrgSince(orgId, since)
        val execTotal = execRepo.countByOrgSince(orgId, since)
        val activeWorkspaces = workspaceRepo.findByOrgId(orgId).size.toLong()

        val msgByDay = chatMsgRepo.findTimestampsByOrg(orgId, since)
            .groupBy { LocalDate.ofInstant(it, ZoneOffset.UTC).toString() }
            .mapValues { it.value.size.toLong() }

        val execByDay = execRepo.findTimestampsByOrg(orgId, since)
            .groupBy { LocalDate.ofInstant(it, ZoneOffset.UTC).toString() }
            .mapValues { it.value.size.toLong() }

        val org = orgRepo.findById(orgId)
            .orElseThrow { NoSuchElementException("Organization not found: $orgId") }

        return OrgUsageSummary(
            orgId = orgId,
            days = days,
            totalMessages = msgTotal,
            totalExecutions = execTotal,
            activeWorkspaces = activeWorkspaces,
            monthlyMessageQuota = org.monthlyMessageQuota,
            monthlyExecQuota = org.monthlyExecQuota,
            messagesByDay = msgByDay,
            executionsByDay = execByDay
        )
    }
}
