package com.forge.webide.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

/**
 * Authentication endpoints for the Web IDE.
 * Provides user information from the JWT token.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    @GetMapping("/me")
    fun me(principal: Principal?): Map<String, Any?> {
        if (principal == null) {
            return mapOf(
                "authenticated" to false,
                "username" to "anonymous",
                "email" to null,
                "roles" to emptyList<String>()
            )
        }

        return mapOf(
            "authenticated" to true,
            "username" to principal.name,
            "email" to null,
            "roles" to emptyList<String>()
        )
    }

    @GetMapping("/me/jwt")
    fun meFromJwt(@AuthenticationPrincipal jwt: Jwt?): Map<String, Any?> {
        if (jwt == null) {
            return mapOf(
                "authenticated" to false,
                "username" to "anonymous"
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
