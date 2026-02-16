package com.forge.webide.config

import com.forge.webide.websocket.ChatWebSocketHandler
import com.forge.webide.websocket.TerminalWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor

/**
 * WebSocket configuration for chat streaming and terminal sessions.
 *
 * Registers WebSocket handlers for:
 * - /ws/chat/{sessionId} - AI chat streaming
 * - /ws/terminal/{workspaceId} - Terminal sessions
 * - /ws/workflow/{workflowId} - Workflow execution streaming
 */
@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val chatWebSocketHandler: ChatWebSocketHandler,
    private val terminalWebSocketHandler: TerminalWebSocketHandler,
    @Value("\${forge.websocket.allowed-origins:http://localhost:3000}")
    private val allowedOrigins: String
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        val origins = allowedOrigins.split(",").map { it.trim() }.toTypedArray()

        registry
            .addHandler(chatWebSocketHandler, "/ws/chat/**")
            .addInterceptors(HttpSessionHandshakeInterceptor())
            .setAllowedOrigins(*origins)

        registry
            .addHandler(terminalWebSocketHandler, "/ws/terminal/**")
            .addInterceptors(HttpSessionHandshakeInterceptor())
            .setAllowedOrigins(*origins)
    }
}
