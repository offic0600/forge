package com.forge.webide.service.skill

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProfileRouterTest {

    private lateinit var skillLoader: SkillLoader
    private lateinit var router: ProfileRouter

    private val devProfile = ProfileDefinition(
        name = "development-profile",
        description = "Development",
        skills = listOf("code-generation"),
        baselines = listOf("code-style-baseline"),
        hitlCheckpoint = "Code review",
        oodaGuidance = "# Dev OODA",
        sourcePath = "test"
    )

    private val designProfile = ProfileDefinition(
        name = "design-profile",
        description = "Design",
        skills = listOf("architecture-design"),
        baselines = listOf("architecture-baseline"),
        hitlCheckpoint = "Architecture review",
        oodaGuidance = "# Design OODA",
        sourcePath = "test"
    )

    private val planningProfile = ProfileDefinition(
        name = "planning-profile",
        description = "Planning",
        skills = listOf("requirement-analysis"),
        baselines = emptyList(),
        hitlCheckpoint = "PRD confirmation",
        oodaGuidance = "# Planning OODA",
        sourcePath = "test"
    )

    private val testingProfile = ProfileDefinition(
        name = "testing-profile",
        description = "Testing",
        skills = listOf("test-case-writing"),
        baselines = listOf("test-coverage-baseline"),
        hitlCheckpoint = "Test report",
        oodaGuidance = "# Testing OODA",
        sourcePath = "test"
    )

    private val opsProfile = ProfileDefinition(
        name = "ops-profile",
        description = "Operations",
        skills = listOf("deployment-ops"),
        baselines = emptyList(),
        hitlCheckpoint = "Release approval",
        oodaGuidance = "# Ops OODA",
        sourcePath = "test"
    )

    @BeforeEach
    fun setUp() {
        skillLoader = mockk()
        every { skillLoader.loadProfile("development-profile") } returns devProfile
        every { skillLoader.loadProfile("design-profile") } returns designProfile
        every { skillLoader.loadProfile("planning-profile") } returns planningProfile
        every { skillLoader.loadProfile("testing-profile") } returns testingProfile
        every { skillLoader.loadProfile("ops-profile") } returns opsProfile

        router = ProfileRouter(skillLoader)
    }

    @Nested
    inner class ExplicitTags {

        @Test
        fun `should route to planning via @规划 tag`() {
            val result = router.route("@规划 创建一个新的需求文档")

            assertThat(result.profile.name).isEqualTo("planning-profile")
            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.reason).contains("@规划")
        }

        @Test
        fun `should route to design via @设计 tag`() {
            val result = router.route("@设计 设计一个支付系统架构")

            assertThat(result.profile.name).isEqualTo("design-profile")
            assertThat(result.confidence).isEqualTo(1.0)
        }

        @Test
        fun `should route to development via @开发 tag`() {
            val result = router.route("@开发 实现订单服务")

            assertThat(result.profile.name).isEqualTo("development-profile")
            assertThat(result.confidence).isEqualTo(1.0)
        }

        @Test
        fun `should route to testing via @测试 tag`() {
            val result = router.route("@测试 写测试用例")

            assertThat(result.profile.name).isEqualTo("testing-profile")
            assertThat(result.confidence).isEqualTo(1.0)
        }

        @Test
        fun `should route to ops via @运维 tag`() {
            val result = router.route("@运维 部署到生产环境")

            assertThat(result.profile.name).isEqualTo("ops-profile")
            assertThat(result.confidence).isEqualTo(1.0)
        }

        @Test
        fun `explicit tag should have highest priority over keywords`() {
            // Message contains both @设计 tag and "implement" keyword
            val result = router.route("@设计 implement the authentication module")

            assertThat(result.profile.name).isEqualTo("design-profile")
            assertThat(result.confidence).isEqualTo(1.0)
        }
    }

    @Nested
    inner class KeywordDetection {

        @Test
        fun `should detect development keywords in English`() {
            val result = router.route("implement a new REST API endpoint for orders")

            assertThat(result.profile.name).isEqualTo("development-profile")
            assertThat(result.confidence).isGreaterThan(0.5)
            assertThat(result.reason).contains("Keyword")
        }

        @Test
        fun `should detect development keywords in Chinese`() {
            val result = router.route("帮我实现一个订单服务，修复登录的bug")

            assertThat(result.profile.name).isEqualTo("development-profile")
            assertThat(result.confidence).isGreaterThan(0.5)
        }

        @Test
        fun `should detect design keywords`() {
            val result = router.route("design the architecture for the payment system")

            assertThat(result.profile.name).isEqualTo("design-profile")
        }

        @Test
        fun `should detect testing keywords`() {
            val result = router.route("write test cases for the order service with good coverage")

            assertThat(result.profile.name).isEqualTo("testing-profile")
        }

        @Test
        fun `should detect ops keywords`() {
            val result = router.route("deploy the latest release to kubernetes")

            assertThat(result.profile.name).isEqualTo("ops-profile")
        }

        @Test
        fun `should detect planning keywords`() {
            val result = router.route("write a PRD for the new user story")

            assertThat(result.profile.name).isEqualTo("planning-profile")
        }

        @Test
        fun `should prefer profile with more keyword matches`() {
            // "test" + "coverage" → 2 testing matches vs "code" → 1 dev match
            val result = router.route("analyze test coverage and find regression issues")

            assertThat(result.profile.name).isEqualTo("testing-profile")
        }

        @Test
        fun `higher keyword score should give higher confidence`() {
            val singleMatch = router.route("fix bug in the login module")
            val multiMatch = router.route("implement code and build a PR for the refactor")

            assertThat(multiMatch.confidence).isGreaterThan(singleMatch.confidence)
        }
    }

    @Nested
    inner class BranchContext {

        @Test
        fun `should route feature branch to development`() {
            val result = router.route("help me with this task", branchName = "feature/add-orders")

            assertThat(result.profile.name).isEqualTo("development-profile")
            assertThat(result.confidence).isEqualTo(0.6)
            assertThat(result.reason).contains("Branch context")
        }

        @Test
        fun `should route hotfix branch to ops`() {
            val result = router.route("help me with this task", branchName = "hotfix/fix-payment-crash")

            assertThat(result.profile.name).isEqualTo("ops-profile")
        }

        @Test
        fun `should route release branch to ops`() {
            val result = router.route("help me with this task", branchName = "release/v1.2.0")

            assertThat(result.profile.name).isEqualTo("ops-profile")
        }

        @Test
        fun `keywords should have higher priority than branch context`() {
            val result = router.route(
                "design the API schema for the new feature",
                branchName = "feature/add-orders"
            )

            // "design" + "schema" → design-profile should win over branch-based development-profile
            assertThat(result.profile.name).isEqualTo("design-profile")
        }
    }

    @Nested
    inner class DefaultFallback {

        @Test
        fun `should default to development when no match`() {
            val result = router.route("hello, can you help me?")

            assertThat(result.profile.name).isEqualTo("development-profile")
            assertThat(result.confidence).isEqualTo(0.3)
            assertThat(result.reason).contains("Default")
        }

        @Test
        fun `should provide built-in fallback when no profiles loaded`() {
            val emptyLoader = mockk<SkillLoader>()
            every { emptyLoader.loadProfile(any()) } returns null
            val fallbackRouter = ProfileRouter(emptyLoader)

            val result = fallbackRouter.route("hello")

            assertThat(result.profile.name).isEqualTo("development-profile")
            assertThat(result.confidence).isEqualTo(0.1)
            assertThat(result.reason).contains("Built-in fallback")
        }
    }
}
