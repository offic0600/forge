package com.forge.webide.service.skill

/**
 * A loaded Skill definition parsed from a SKILL.md file.
 */
data class SkillDefinition(
    val name: String,
    val description: String,
    val trigger: String? = null,
    val tags: List<String> = emptyList(),
    val content: String,
    val sourcePath: String
)

/**
 * A loaded Profile definition parsed from a profile .md file.
 */
data class ProfileDefinition(
    val name: String,
    val description: String,
    val skills: List<String> = emptyList(),
    val baselines: List<String> = emptyList(),
    val hitlCheckpoint: String = "",
    val oodaGuidance: String,
    val sourcePath: String
)

/**
 * Result of routing a user message to a specific Profile.
 */
data class ProfileRoutingResult(
    val profile: ProfileDefinition,
    val confidence: Double,
    val reason: String
)
