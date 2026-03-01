package com.forge.webide.service

import com.forge.webide.repository.OrgMemberRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class RbacHelper(
    private val orgMemberRepository: OrgMemberRepository,
    @Value("\${forge.security.enabled:false}") private val securityEnabled: Boolean
) {
    fun isSystemAdmin(jwt: Jwt?): Boolean {
        if (!securityEnabled) return true
        @Suppress("UNCHECKED_CAST")
        val realmAccess = jwt?.getClaim<Map<String, Any>>("realm_access") ?: return false
        val roles = realmAccess["roles"] as? List<*> ?: return false
        return "admin" in roles
    }

    fun isOrgAdmin(jwt: Jwt?, orgId: String): Boolean {
        if (!securityEnabled) return true
        val userId = jwt?.subject ?: return false
        val member = orgMemberRepository.findByOrgIdAndUserId(orgId, userId).orElse(null)
        return member?.role in listOf("OWNER", "ADMIN")
    }

    fun requireSystemAdmin(jwt: Jwt?) {
        if (!isSystemAdmin(jwt)) throw AccessDeniedException("Requires SystemAdmin")
    }

    fun requireOrgAdmin(jwt: Jwt?, orgId: String) {
        if (!isOrgAdmin(jwt, orgId)) throw AccessDeniedException("Requires OrgAdmin")
    }
}
