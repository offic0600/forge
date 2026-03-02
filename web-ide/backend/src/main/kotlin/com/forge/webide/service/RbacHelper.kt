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
        // Support both claim formats:
        // 1. realm_roles: ["admin", ...] — flat array (custom Keycloak mapper, claim.name=realm_roles)
        // 2. realm_access: { roles: ["admin", ...] } — standard Keycloak nested format
        val flatRoles = jwt?.getClaimAsStringList("realm_roles")
        if (!flatRoles.isNullOrEmpty()) return "admin" in flatRoles
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

    fun requireAuthenticated(jwt: Jwt?) {
        if (securityEnabled && jwt == null)
            throw AccessDeniedException("Authentication required")
    }

    fun requireOrgAdmin(jwt: Jwt?, orgId: String) {
        if (!isSystemAdmin(jwt) && !isOrgAdmin(jwt, orgId))
            throw AccessDeniedException("Requires OrgAdmin or SystemAdmin for org $orgId")
    }
}
