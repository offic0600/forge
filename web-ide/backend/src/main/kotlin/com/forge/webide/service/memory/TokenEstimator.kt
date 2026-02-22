package com.forge.webide.service.memory

import com.forge.adapter.model.Message
import org.springframework.stereotype.Service

/**
 * Rough token estimator for context window management.
 * Uses heuristic: Chinese chars ~1.5 chars/token, English ~4 chars/token.
 */
@Service
class TokenEstimator {

    /**
     * Estimate token count for a text string.
     */
    fun estimate(text: String): Int {
        if (text.isBlank()) return 0
        val chineseChars = text.count { it.code in 0x4E00..0x9FFF }
        val otherChars = text.length - chineseChars
        return (chineseChars * 0.67 + otherChars * 0.25).toInt().coerceAtLeast(1)
    }

    /**
     * Estimate total tokens for a list of messages.
     */
    fun estimateMessages(messages: List<Message>): Int {
        return messages.sumOf { msg ->
            estimate(msg.content) +
                (msg.toolUses?.sumOf { estimate(it.name) + estimate(it.input.toString()) } ?: 0) +
                (msg.toolResults?.sumOf { estimate(it.content) } ?: 0)
        }
    }
}
