package com.forge.webide.service.skill

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SkillLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var skillLoader: SkillLoader

    @BeforeEach
    fun setUp() {
        skillLoader = SkillLoader(tempDir.toString())
    }

    @Nested
    inner class SplitFrontmatter {

        @Test
        fun `should split frontmatter and body correctly`() {
            val content = """
                |---
                |name: test-skill
                |description: A test skill
                |---
                |
                |# Body Content
                |Some markdown here.
            """.trimMargin()

            val (frontmatter, body) = skillLoader.splitFrontmatter(content)

            assertThat(frontmatter).contains("name: test-skill")
            assertThat(frontmatter).contains("description: A test skill")
            assertThat(body).contains("# Body Content")
            assertThat(body).contains("Some markdown here.")
        }

        @Test
        fun `should return empty frontmatter when no delimiters`() {
            val content = "# Just a markdown file\nNo frontmatter here."

            val (frontmatter, body) = skillLoader.splitFrontmatter(content)

            assertThat(frontmatter).isEmpty()
            assertThat(body).isEqualTo(content)
        }

        @Test
        fun `should handle frontmatter with no closing delimiter`() {
            val content = "---\nname: broken\nNo closing delimiter"

            val (frontmatter, body) = skillLoader.splitFrontmatter(content)

            assertThat(frontmatter).isEmpty()
            assertThat(body).isEqualTo(content)
        }
    }

    @Nested
    inner class ParseSkillFile {

        @Test
        fun `should parse skill with full frontmatter`() {
            val skillDir = tempDir.resolve("forge-foundation/skills/kotlin-conventions")
            Files.createDirectories(skillDir)
            val skillFile = skillDir.resolve("SKILL.md")
            Files.writeString(skillFile, """
                |---
                |name: kotlin-conventions
                |description: Kotlin coding conventions
                |trigger: when writing .kt files
                |tags: [kotlin, conventions]
                |---
                |
                |# Kotlin Conventions
                |
                |Use data classes for DTOs.
            """.trimMargin())

            val skill = skillLoader.parseSkillFile(skillFile)

            assertThat(skill.name).isEqualTo("kotlin-conventions")
            assertThat(skill.description).isEqualTo("Kotlin coding conventions")
            assertThat(skill.trigger).isEqualTo("when writing .kt files")
            assertThat(skill.tags).containsExactly("kotlin", "conventions")
            assertThat(skill.content).contains("# Kotlin Conventions")
            assertThat(skill.content).contains("Use data classes for DTOs.")
        }

        @Test
        fun `should use directory name when name not in frontmatter`() {
            val skillDir = tempDir.resolve("forge-foundation/skills/my-skill")
            Files.createDirectories(skillDir)
            val skillFile = skillDir.resolve("SKILL.md")
            Files.writeString(skillFile, """
                |---
                |description: A skill without a name
                |---
                |
                |# Content
            """.trimMargin())

            val skill = skillLoader.parseSkillFile(skillFile)

            assertThat(skill.name).isEqualTo("my-skill")
        }

        @Test
        fun `should handle multiline description`() {
            val skillDir = tempDir.resolve("forge-foundation/skills/test")
            Files.createDirectories(skillDir)
            val skillFile = skillDir.resolve("SKILL.md")
            Files.writeString(skillFile, """
                |---
                |name: test-skill
                |description: >
                |  A long description that
                |  spans multiple lines
                |tags: [test]
                |---
                |
                |# Test
            """.trimMargin())

            val skill = skillLoader.parseSkillFile(skillFile)

            assertThat(skill.name).isEqualTo("test-skill")
            assertThat(skill.description).contains("A long description")
        }
    }

    @Nested
    inner class ParseProfileFile {

        @Test
        fun `should parse profile with YAML lists`() {
            val profileDir = tempDir.resolve("forge-superagent/skill-profiles")
            Files.createDirectories(profileDir)
            val profileFile = profileDir.resolve("development-profile.md")
            Files.writeString(profileFile, """
                |---
                |name: development-profile
                |description: "Development stage profile"
                |skills:
                |  - code-generation
                |  - foundation-skills-all
                |baselines:
                |  - code-style-baseline
                |  - security-baseline
                |hitl-checkpoint: "Code review checkpoint"
                |---
                |
                |# Development Profile OODA Guidance
                |
                |## Observe
                |Read the design documents.
            """.trimMargin())

            val profile = skillLoader.parseProfileFile(profileFile)

            assertThat(profile.name).isEqualTo("development-profile")
            assertThat(profile.description).isEqualTo("Development stage profile")
            assertThat(profile.skills).containsExactly("code-generation", "foundation-skills-all")
            assertThat(profile.baselines).containsExactly("code-style-baseline", "security-baseline")
            assertThat(profile.hitlCheckpoint).isEqualTo("Code review checkpoint")
            assertThat(profile.oodaGuidance).contains("# Development Profile OODA Guidance")
            assertThat(profile.oodaGuidance).contains("## Observe")
        }

        @Test
        fun `should handle profile with empty baselines`() {
            val profileDir = tempDir.resolve("forge-superagent/skill-profiles")
            Files.createDirectories(profileDir)
            val profileFile = profileDir.resolve("planning-profile.md")
            Files.writeString(profileFile, """
                |---
                |name: planning-profile
                |description: "Planning profile"
                |skills:
                |  - requirement-analysis
                |baselines: []
                |hitl-checkpoint: "PRD confirmation"
                |---
                |
                |# Planning Profile
            """.trimMargin())

            val profile = skillLoader.parseProfileFile(profileFile)

            assertThat(profile.baselines).isEmpty()
            assertThat(profile.skills).containsExactly("requirement-analysis")
        }
    }

    @Nested
    inner class LoadAndCache {

        @Test
        fun `should load skills from plugins directory`() {
            setupTestPlugins()
            skillLoader.reloadAll()

            assertThat(skillLoader.loadAllSkills()).hasSize(2)
            assertThat(skillLoader.loadSkill("kotlin-conventions")).isNotNull
            assertThat(skillLoader.loadSkill("code-generation")).isNotNull
        }

        @Test
        fun `should load profiles from plugins directory`() {
            setupTestPlugins()
            skillLoader.reloadAll()

            assertThat(skillLoader.loadAllProfiles()).hasSize(1)
            assertThat(skillLoader.loadProfile("development-profile")).isNotNull
        }

        @Test
        fun `should load foundation skills separately`() {
            setupTestPlugins()
            skillLoader.reloadAll()

            val foundationSkills = skillLoader.loadAllFoundationSkills()
            assertThat(foundationSkills).hasSize(1)
            assertThat(foundationSkills[0].name).isEqualTo("kotlin-conventions")
        }

        @Test
        fun `should load skills for profile with foundation-skills-all token`() {
            setupTestPlugins()
            skillLoader.reloadAll()

            val profile = skillLoader.loadProfile("development-profile")!!
            val skills = skillLoader.loadSkillsForProfile(profile)

            assertThat(skills.map { it.name }).contains("code-generation")
            assertThat(skills.map { it.name }).contains("kotlin-conventions")
        }

        @Test
        fun `should handle missing plugins directory gracefully`() {
            val loader = SkillLoader("/nonexistent/path")
            loader.reloadAll()

            assertThat(loader.loadAllSkills()).isEmpty()
            assertThat(loader.loadAllProfiles()).isEmpty()
        }

        @Test
        fun `should load superagent instructions`() {
            setupTestPlugins()
            skillLoader.reloadAll()

            assertThat(skillLoader.superAgentInstructions).contains("Forge SuperAgent")
        }

        @Test
        fun `reloadAll should clear and reload cache`() {
            setupTestPlugins()
            skillLoader.reloadAll()
            assertThat(skillLoader.loadAllSkills()).hasSize(2)

            // Add another skill
            val newSkillDir = tempDir.resolve("forge-foundation/skills/new-skill")
            Files.createDirectories(newSkillDir)
            Files.writeString(newSkillDir.resolve("SKILL.md"), """
                |---
                |name: new-skill
                |description: A new skill
                |---
                |# New Skill
            """.trimMargin())

            skillLoader.reloadAll()
            assertThat(skillLoader.loadAllSkills()).hasSize(3)
        }

        private fun setupTestPlugins() {
            // Foundation skill
            val foundationDir = tempDir.resolve("forge-foundation/skills/kotlin-conventions")
            Files.createDirectories(foundationDir)
            Files.writeString(foundationDir.resolve("SKILL.md"), """
                |---
                |name: kotlin-conventions
                |description: Kotlin conventions
                |tags: [kotlin]
                |---
                |# Kotlin Conventions
                |Use data classes.
            """.trimMargin())

            // Delivery skill
            val deliveryDir = tempDir.resolve("forge-superagent/skills/code-generation")
            Files.createDirectories(deliveryDir)
            Files.writeString(deliveryDir.resolve("SKILL.md"), """
                |---
                |name: code-generation
                |description: Code generation skill
                |tags: [code]
                |---
                |# Code Generation
                |Generate code from design.
            """.trimMargin())

            // Profile
            val profileDir = tempDir.resolve("forge-superagent/skill-profiles")
            Files.createDirectories(profileDir)
            Files.writeString(profileDir.resolve("development-profile.md"), """
                |---
                |name: development-profile
                |description: Development profile
                |skills:
                |  - code-generation
                |  - foundation-skills-all
                |baselines:
                |  - code-style-baseline
                |hitl-checkpoint: Code review
                |---
                |# Development OODA
            """.trimMargin())

            // CLAUDE.md
            Files.writeString(tempDir.resolve("forge-superagent/CLAUDE.md"),
                "# Forge SuperAgent — System Instructions\n\nYou are Forge SuperAgent.")
        }
    }
}
