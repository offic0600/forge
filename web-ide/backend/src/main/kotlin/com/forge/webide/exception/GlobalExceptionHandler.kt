package com.forge.webide.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(mapOf("error" to "forbidden", "message" to (ex.message ?: "Access denied")))

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(ex.statusCode)
            .body(mapOf("error" to ex.reason.orEmpty(), "message" to ex.message))
}
