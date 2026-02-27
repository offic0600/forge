package com.forge.webide.service.skill

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class IntentSkillRouterTest {

    private lateinit var router: IntentSkillRouter

    @BeforeEach
    fun setUp() {
        router = IntentSkillRouter()
    }

    @Nested
    inner class TagAliases {

        @Test
        fun `@开发 tag routes to development skills with confidence 1_0`() {
            val result = router.analyze("@开发 实现订单服务")

            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.skills).contains("code-generation")
            assertThat(result.skills).contains("kotlin-conventions")
            assertThat(result.baselines).contains("code-style-baseline")
            assertThat(result.reason).contains("@开发")
            assertThat(result.profileHint).isEqualTo("development-profile")
        }

        @Test
        fun `@测试 tag routes to testing skills`() {
            val result = router.analyze("@测试 写测试用例")

            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.skills).contains("test-case-writing")
            assertThat(result.skills).contains("testing-standards")
            assertThat(result.profileHint).isEqualTo("testing-profile")
        }

        @Test
        fun `@设计 tag routes to design skills`() {
            val result = router.analyze("@设计 设计支付系统架构")

            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.skills).contains("architecture-design")
            assertThat(result.profileHint).isEqualTo("design-profile")
        }

        @Test
        fun `@规划 tag routes to planning skills`() {
            val result = router.analyze("@规划 分析用户需求")

            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.skills).contains("requirement-engineering")
            assertThat(result.profileHint).isEqualTo("planning-profile")
        }

        @Test
        fun `@运维 tag routes to ops skills`() {
            val result = router.analyze("@运维 部署到生产环境")

            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.skills).contains("deployment-ops")
            assertThat(result.profileHint).isEqualTo("ops-profile")
        }

        @Test
        fun `@评估 tag routes to evaluation skills in read-only mode`() {
            val result = router.analyze("@评估 分析项目进度")

            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.skills).contains("progress-evaluation")
            assertThat(result.mode).isEqualTo("read-only")
        }

        @Test
        fun `@git tag routes to git-workflow skill`() {
            val result = router.analyze("@git 帮我提交代码")

            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.skills).containsExactly("git-workflow")
            assertThat(result.reason).contains("@git")
        }

        @Test
        fun `tag routing has higher priority than keywords`() {
            // @测试 should win over "implement" keyword
            val result = router.analyze("@测试 帮我 implement 一个测试框架")

            assertThat(result.confidence).isEqualTo(1.0)
            assertThat(result.profileHint).isEqualTo("testing-profile")
        }
    }

    @Nested
    inner class KeywordRouting {

        @Test
        fun `git keywords route to git-workflow`() {
            val result = router.analyze("帮我提交代码并 push 到远程")

            assertThat(result.skills).contains("git-workflow")
            assertThat(result.confidence).isGreaterThan(0.5)
        }

        @Test
        fun `commit keyword routes to git-workflow`() {
            val result = router.analyze("帮我 commit 这些改动")

            assertThat(result.skills).contains("git-workflow")
        }

        @Test
        fun `test keywords route to test skills`() {
            val result = router.analyze("帮我写测试用例")

            assertThat(result.skills).contains("test-case-writing")
            assertThat(result.confidence).isGreaterThan(0.5)
        }

        @Test
        fun `bug keyword routes to bug-fix-workflow`() {
            val result = router.analyze("帮我修复这个 bug")

            assertThat(result.skills).contains("bug-fix-workflow")
            assertThat(result.confidence).isGreaterThan(0.5)
        }

        @Test
        fun `architecture keywords route to design skills`() {
            val result = router.analyze("帮我设计系统架构")

            assertThat(result.skills).contains("architecture-design")
        }

        @Test
        fun `deploy keywords route to ops skills`() {
            val result = router.analyze("帮我部署到 k8s")

            assertThat(result.skills).contains("deployment-ops")
        }

        @Test
        fun `multiple keyword matches give higher confidence`() {
            val singleMatch = router.analyze("帮我 commit")
            val multiMatch = router.analyze("帮我 git commit push 到远程 branch")

            assertThat(multiMatch.confidence).isGreaterThanOrEqualTo(singleMatch.confidence)
        }

        @Test
        fun `kotlin indicator appends kotlin-conventions skill`() {
            val result = router.analyze("帮我写一个 Kotlin 的 REST API")

            assertThat(result.skills).contains("kotlin-conventions")
        }

        @Test
        fun `spring indicator appends spring-boot-patterns skill`() {
            val result = router.analyze("帮我写 Spring @RestController")

            assertThat(result.skills).contains("spring-boot-patterns")
        }
    }

    @Nested
    inner class DefaultFallback {

        @Test
        fun `unknown message falls back to default skills`() {
            val result = router.analyze("你好，请介绍一下自己")

            assertThat(result.confidence).isEqualTo(0.3)
            assertThat(result.reason).contains("Default")
            assertThat(result.skills).contains("delivery-methodology")
            assertThat(result.skills).contains("code-generation")
        }

        @Test
        fun `empty message falls back to default`() {
            val result = router.analyze("")

            assertThat(result.confidence).isEqualTo(0.3)
            assertThat(result.mode).isEqualTo("default")
        }
    }

    @Nested
    inner class ReadOnlyMode {

        @Test
        fun `evaluation keywords enable read-only mode`() {
            val result = router.analyze("帮我评估项目进度状态")

            assertThat(result.mode).isEqualTo("read-only")
            assertThat(result.skills).contains("progress-evaluation")
        }

        @Test
        fun `@评估 tag sets read-only mode`() {
            val result = router.analyze("@评估 查看代码质量")

            assertThat(result.mode).isEqualTo("read-only")
        }
    }

    @Nested
    inner class BackwardCompatibility {

        @Test
        fun `@开发 backward compat returns development-profile hint`() {
            val result = router.analyze("@开发 写个 service")
            assertThat(result.profileHint).isEqualTo("development-profile")
        }

        @Test
        fun `@测试 backward compat returns testing-profile hint`() {
            val result = router.analyze("@测试 写测试")
            assertThat(result.profileHint).isEqualTo("testing-profile")
        }
    }
}
