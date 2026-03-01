package com.forge.webide.repository

import com.forge.webide.entity.OrgInvitationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrgInvitationRepository : JpaRepository<OrgInvitationEntity, Long> {
    fun findByToken(token: String): Optional<OrgInvitationEntity>
    fun findByOrgId(orgId: String): List<OrgInvitationEntity>
}
