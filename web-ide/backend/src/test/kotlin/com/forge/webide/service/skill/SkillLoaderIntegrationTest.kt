package com.forge.webide.service.skill

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Integration test that loads skills and profiles from the real plugins/ directory.
 * Only runs when the plugins directory is available (not in CI without the full repo).
 */
@EnabledIf("pluginsDirectoryExists")
class SkillLoaderIntegrationTest {

    private lateinit var skillLoader: SkillLoader

    companion object {
        private fun resolvePluginsPath(): String {
            // Try relative to project root (Gradle runs from module dir)
            val candidates = listOf(
                "../../plugins",        // from web-ide/backend/
                "../plugins",           // from web-ide/
                "plugins"               // from project root
            )
            for (candidate in candidates) {
                val path = Paths.get(candidate).toAbsolutePath().normalize()
                if (Files.isDirectory(path.resolve("forge-foundation/skills"))) {
                    return path.toString()
                }
            }
            return "plugins"
        }

        @JvmStatic
        fun pluginsDirectoryExists(): Boolean {
            val path = Paths.get(resolvePluginsPath())
            return Files.isDirectory(path.resolve("forge-foundation/skills"))
        }
    }

    @BeforeEach
    fun setUp() {
        skillLoader = SkillLoader(resolvePluginsPath())
        skillLoader.reloadAll()
    }

    @Test
    fun `should load foundation skills`() {
        val foundationSkills = skillLoader.loadAllFoundationSkills()

        assertThat(foundationSkills).isNotEmpty
        assertThat(foundationSkills.map { it.name }).contains(
            "kotlin-conventions",
            "spring-boot-patterns",
            "api-design",
            "testing-standards"
        )

        // Each skill should have non-empty content
        for (skill in foundationSkills) {
            assertThat(skill.content).isNotBlank()
            assertThat(skill.name).isNotBlank()
            assertThat(skill.sourcePath).contains("SKILL.md")
        }
    }

    @Test
    fun `should load delivery skills`() {
        val allSkills = skillLoader.loadAllSkills()
        val deliverySkills = allSkills.filter { it.sourcePath.contains("forge-superagent") }

        assertThat(deliverySkills).isNotEmpty
        assertThat(deliverySkills.map { it.name }).contains(
            "code-generation",
            "architecture-design"
        )
    }

    @Test
    fun `should load all five profiles`() {
        val profiles = skillLoader.loadAllProfiles()

        assertThat(profiles).hasSizeGreaterThanOrEqualTo(5)
        assertThat(profiles.map { it.name }).containsAll(
            listOf(
                "planning-profile",
                "design-profile",
                "development-profile",
                "testing-profile",
                "ops-profile"
            )
        )

        // Each profile should have OODA guidance
        for (profile in profiles) {
            assertThat(profile.oodaGuidance).isNotBlank()
            assertThat(profile.name).isNotBlank()
        }
    }

    @Test
    fun `development profile should reference code-generation skill`() {
        val devProfile = skillLoader.loadProfile("development-profile")

        assertThat(devProfile).isNotNull
        assertThat(devProfile!!.skills).contains("code-generation")
        assertThat(devProfile.baselines).isNotEmpty
    }

    @Test
    fun `should load skills for development profile`() {
        val devProfile = skillLoader.loadProfile("development-profile")!!
        val skills = skillLoader.loadSkillsForProfile(devProfile)

        // Should include code-generation + all foundation skills
        assertThat(skills).isNotEmpty
        assertThat(skills.map { it.name }).contains("code-generation")
        // foundation-skills-all should expand to include foundation skills
        assertThat(skills.map { it.name }).contains("kotlin-conventions")
    }

    @Test
    fun `should load SuperAgent instructions`() {
        assertThat(skillLoader.superAgentInstructions).isNotBlank()
        assertThat(skillLoader.superAgentInstructions).contains("Forge SuperAgent")
        assertThat(skillLoader.superAgentInstructions).contains("OODA")
    }

    @Test
    fun `total loaded items should match expected counts`() {
        val skills = skillLoader.loadAllSkills()
        val profiles = skillLoader.loadAllProfiles()

        // Foundation (16-18) + SuperAgent (8) + Deployment (2) + Knowledge (3) ≈ 29+
        assertThat(skills.size).isGreaterThanOrEqualTo(20)
        assertThat(profiles.size).isGreaterThanOrEqualTo(5)

        println("Integration test: Loaded ${skills.size} skills, ${profiles.size} profiles")
        println("Skills: ${skills.map { it.name }.sorted().joinToString(", ")}")
        println("Profiles: ${profiles.map { it.name }.sorted().joinToString(", ")}")
    }
}
