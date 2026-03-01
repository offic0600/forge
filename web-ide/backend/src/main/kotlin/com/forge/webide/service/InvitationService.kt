package com.forge.webide.service

import com.forge.webide.entity.OrgInvitationEntity
import com.forge.webide.entity.OrgMemberEntity
import com.forge.webide.model.OrgInvitation
import com.forge.webide.model.OrgInvitationInfo
import com.forge.webide.model.OrgMember
import com.forge.webide.repository.OrgInvitationRepository
import com.forge.webide.repository.OrgMemberRepository
import com.forge.webide.repository.OrganizationRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class InvitationService(
    private val invitationRepository: OrgInvitationRepository,
    private val orgMemberRepository: OrgMemberRepository,
    private val organizationRepository: OrganizationRepository,
    private val rbacHelper: RbacHelper
) {
    fun createInvitation(orgId: String, role: String, jwt: Jwt?): OrgInvitation {
        if (!rbacHelper.isSystemAdmin(jwt) && !rbacHelper.isOrgAdmin(jwt, orgId)) {
            throw AccessDeniedException("Only SystemAdmin or OrgAdmin can create invitations")
        }
        val token = generateToken()
        val expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
        val createdBy = jwt?.subject ?: "anonymous"
        val entity = OrgInvitationEntity(
            token = token,
            orgId = orgId,
            role = role,
            createdBy = createdBy,
            expiresAt = expiresAt
        )
        invitationRepository.save(entity)
        return entity.toModel()
    }

    fun getInvitation(token: String): OrgInvitationInfo {
        val entity = invitationRepository.findByToken(token)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found") }
        if (entity.usedBy != null) {
            throw ResponseStatusException(HttpStatus.GONE, "Invitation already used")
        }
        if (entity.expiresAt.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.GONE, "Invitation expired")
        }
        val org = organizationRepository.findById(entity.orgId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found") }
        return OrgInvitationInfo(
            token = entity.token,
            orgId = entity.orgId,
            orgName = org.name,
            role = entity.role,
            expiresAt = entity.expiresAt
        )
    }

    @Transactional
    fun acceptInvitation(token: String, jwt: Jwt?): OrgMember {
        val userId = jwt?.subject
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")
        val entity = invitationRepository.findByToken(token)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found") }
        if (entity.usedBy != null) {
            throw ResponseStatusException(HttpStatus.GONE, "Invitation already used")
        }
        if (entity.expiresAt.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.GONE, "Invitation expired")
        }

        // Idempotent: if already a member, return existing
        val existing = orgMemberRepository.findByOrgIdAndUserId(entity.orgId, userId)
        if (existing.isPresent) return existing.get().toModel()

        // Create new member
        val member = OrgMemberEntity(
            orgId = entity.orgId,
            userId = userId,
            role = entity.role
        )
        orgMemberRepository.save(member)

        // Mark token as used
        entity.usedBy = userId
        entity.usedAt = Instant.now()
        invitationRepository.save(entity)

        return member.toModel()
    }

    private fun generateToken(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun OrgInvitationEntity.toModel() = OrgInvitation(
        id = id,
        token = token,
        orgId = orgId,
        role = role,
        createdBy = createdBy,
        expiresAt = expiresAt,
        usedBy = usedBy,
        usedAt = usedAt
    )

    private fun OrgMemberEntity.toModel() = OrgMember(
        orgId = orgId,
        userId = userId,
        role = role,
        joinedAt = joinedAt
    )
}
