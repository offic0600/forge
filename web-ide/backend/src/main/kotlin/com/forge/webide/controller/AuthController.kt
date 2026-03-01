package com.forge.webide.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Authentication endpoints for the Web IDE.
 *
 * Uses @AuthenticationPrincipal Jwt? instead of Principal? because Spring
 * Security injects a non-null anonymous Principal for unauthenticated requests,
 * making Principal? checks unreliable. Jwt? is null for anonymous users and
 * non-null only when a valid Bearer token is present.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt?): Map<String, Any?> {
        if (jwt == null) {
            return mapOf(
                "authenticated" to false,
                "username" to "anonymous",
                "email" to null,
                "roles" to emptyList<String>()
            )
        }

        val roles = jwt.getClaimAsStringList("realm_roles") ?: emptyList()

        return mapOf(
            "authenticated" to true,
            "username" to (jwt.getClaimAsString("preferred_username") ?: jwt.subject),
            "email" to jwt.getClaimAsString("email"),
            "name" to jwt.getClaimAsString("name"),
            "roles" to roles,
            "sub" to jwt.subject
        )
    }
}
