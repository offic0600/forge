package com.forge.webide.controller

import com.forge.webide.model.CreateInvitationRequest
import com.forge.webide.model.OrgInvitation
import com.forge.webide.model.OrgInvitationInfo
import com.forge.webide.model.OrgMember
import com.forge.webide.service.InvitationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class InvitationController(
    private val invitationService: InvitationService
) {
    @PostMapping("/orgs/{orgId}/invitations")
    fun createInvitation(
        @PathVariable orgId: String,
        @RequestBody req: CreateInvitationRequest,
        @AuthenticationPrincipal jwt: Jwt? = null
    ): ResponseEntity<OrgInvitation> {
        val invitation = invitationService.createInvitation(orgId, req.role, jwt)
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation)
    }

    @GetMapping("/invitations/{token}")
    fun getInvitation(@PathVariable token: String): ResponseEntity<OrgInvitationInfo> {
        val info = invitationService.getInvitation(token)
        return ResponseEntity.ok(info)
    }

    @PostMapping("/invitations/{token}/accept")
    fun acceptInvitation(
        @PathVariable token: String,
        @AuthenticationPrincipal jwt: Jwt? = null
    ): ResponseEntity<OrgMember> {
        val member = invitationService.acceptInvitation(token, jwt)
        return ResponseEntity.ok(member)
    }
}
