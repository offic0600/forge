package com.forge.webide.service.skill

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Routes user messages to the appropriate Skill Profile.
 *
 * Priority chain (from CLAUDE.md):
 * 1. Explicit user tags (@规划, @设计, @开发, @测试, @运维)
 * 2. Keyword auto-detection (Chinese and English)
 * 3. Workspace context (branch name)
 * 4. Default to development-profile
 */
@Service
class ProfileRouter(
    private val skillLoader: SkillLoader
) {
    private val logger = LoggerFactory.getLogger(ProfileRouter::class.java)

    companion object {
        private const val DEFAULT_PROFILE = "development-profile"

        // Priority 1: Explicit tag → profile name mapping
        private val EXPLICIT_TAGS = mapOf(
            "@规划" to "planning-profile",
            "@设计" to "design-profile",
            "@开发" to "development-profile",
            "@测试" to "testing-profile",
            "@运维" to "ops-profile"
        )

        // Priority 2: Keyword → profile name mapping
        private val KEYWORD_ROUTES: List<Pair<List<String>, String>> = listOf(
            listOf(
                "requirement", "prd", "user story", "feature request", "scope", "stakeholder",
                "需求", "规划", "产品文档"
            ) to "planning-profile",
            listOf(
                "architecture", "design", "api spec", "schema", "adr", "c4", "sequence diagram",
                "架构", "设计", "接口"
            ) to "design-profile",
            listOf(
                "implement", "code", "build", "fix bug", "refactor", "pr",
                "开发", "编码", "实现", "修复"
            ) to "development-profile",
            listOf(
                "test", "coverage", "qa", "regression", "boundary",
                "测试", "覆盖率", "用例"
            ) to "testing-profile",
            listOf(
                "deploy", "release", "rollback", "monitor", "incident", "kubernetes",
                "部署", "发布", "运维"
            ) to "ops-profile"
        )

        // Priority 3: Branch name patterns
        private val BRANCH_PATTERNS = mapOf(
            Regex("^feature/.*") to "development-profile",
            Regex("^bugfix/.*") to "development-profile",
            Regex("^hotfix/.*") to "ops-profile",
            Regex("^release/.*") to "ops-profile",
            Regex("^test/.*") to "testing-profile",
            Regex("^design/.*") to "design-profile"
        )
    }

    /**
     * Route a user message to the most appropriate Profile.
     *
     * @param message The user's message text
     * @param branchName Optional current git branch name for context-based routing
     */
    fun route(message: String, branchName: String? = null): ProfileRoutingResult {
        // Priority 1: Explicit tags
        val tagResult = routeByExplicitTag(message)
        if (tagResult != null) return tagResult

        // Priority 2: Keyword detection
        val keywordResult = routeByKeyword(message)
        if (keywordResult != null) return keywordResult

        // Priority 3: Branch name context
        if (branchName != null) {
            val branchResult = routeByBranch(branchName)
            if (branchResult != null) return branchResult
        }

        // Priority 4: Default
        return routeToDefault()
    }

    private fun routeByExplicitTag(message: String): ProfileRoutingResult? {
        for ((tag, profileName) in EXPLICIT_TAGS) {
            if (message.contains(tag)) {
                val profile = skillLoader.loadProfile(profileName)
                if (profile != null) {
                    logger.info("Routed to {} via explicit tag '{}'", profileName, tag)
                    return ProfileRoutingResult(
                        profile = profile,
                        confidence = 1.0,
                        reason = "Explicit tag: $tag"
                    )
                }
            }
        }
        return null
    }

    private fun routeByKeyword(message: String): ProfileRoutingResult? {
        val lowerMessage = message.lowercase()

        // Score each profile by keyword matches
        var bestProfile: String? = null
        var bestScore = 0
        var bestKeyword = ""

        for ((keywords, profileName) in KEYWORD_ROUTES) {
            var score = 0
            var matchedKeyword = ""
            for (keyword in keywords) {
                if (lowerMessage.contains(keyword.lowercase())) {
                    score++
                    if (matchedKeyword.isEmpty()) matchedKeyword = keyword
                }
            }
            if (score > bestScore) {
                bestScore = score
                bestProfile = profileName
                bestKeyword = matchedKeyword
            }
        }

        if (bestProfile != null && bestScore > 0) {
            val profile = skillLoader.loadProfile(bestProfile)
            if (profile != null) {
                val confidence = (0.5 + 0.1 * bestScore).coerceAtMost(0.95)
                logger.info(
                    "Routed to {} via keyword '{}' (score={}, confidence={})",
                    bestProfile, bestKeyword, bestScore, confidence
                )
                return ProfileRoutingResult(
                    profile = profile,
                    confidence = confidence,
                    reason = "Keyword detected: $bestKeyword (score=$bestScore)"
                )
            }
        }

        return null
    }

    private fun routeByBranch(branchName: String): ProfileRoutingResult? {
        for ((pattern, profileName) in BRANCH_PATTERNS) {
            if (pattern.matches(branchName)) {
                val profile = skillLoader.loadProfile(profileName)
                if (profile != null) {
                    logger.info("Routed to {} via branch name '{}'", profileName, branchName)
                    return ProfileRoutingResult(
                        profile = profile,
                        confidence = 0.6,
                        reason = "Branch context: $branchName"
                    )
                }
            }
        }
        return null
    }

    private fun routeToDefault(): ProfileRoutingResult {
        val profile = skillLoader.loadProfile(DEFAULT_PROFILE)
        if (profile != null) {
            logger.debug("Routed to default profile: {}", DEFAULT_PROFILE)
            return ProfileRoutingResult(
                profile = profile,
                confidence = 0.3,
                reason = "Default fallback"
            )
        }

        // Ultimate fallback: create a minimal development profile
        logger.warn("Default profile '{}' not found, using built-in fallback", DEFAULT_PROFILE)
        return ProfileRoutingResult(
            profile = ProfileDefinition(
                name = DEFAULT_PROFILE,
                description = "Default development profile (built-in fallback)",
                skills = emptyList(),
                baselines = emptyList(),
                hitlCheckpoint = "",
                oodaGuidance = "",
                sourcePath = "built-in"
            ),
            confidence = 0.1,
            reason = "Built-in fallback (no profiles loaded)"
        )
    }
}
