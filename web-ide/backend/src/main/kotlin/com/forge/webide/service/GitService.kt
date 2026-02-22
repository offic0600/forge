package com.forge.webide.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.concurrent.TimeUnit

data class GitStatus(
    val branch: String,
    val clean: Boolean,
    val modifiedFiles: List<String> = emptyList()
)

@Service
class GitService {

    private val logger = LoggerFactory.getLogger(GitService::class.java)

    fun cloneRepository(url: String, branch: String?, targetDir: Path): Path {
        val cmd = mutableListOf("git", "clone", "--depth", "1")
        if (!branch.isNullOrBlank()) {
            cmd.addAll(listOf("-b", branch))
        }
        cmd.addAll(listOf(url, targetDir.toString()))

        logger.info("Cloning repository: {} -> {}", url, targetDir)
        val result = runGitCommand(cmd, targetDir.parent)

        if (result.exitCode != 0) {
            throw GitOperationException("git clone failed (exit=${result.exitCode}): ${result.stderr}")
        }

        logger.info("Repository cloned successfully: {}", url)
        return targetDir
    }

    fun pull(workspaceDir: Path): String {
        val result = runGitCommand(listOf("git", "pull", "--ff-only"), workspaceDir)
        if (result.exitCode != 0) {
            throw GitOperationException("git pull failed: ${result.stderr}")
        }
        return result.stdout
    }

    fun status(workspaceDir: Path): GitStatus {
        val branchResult = runGitCommand(listOf("git", "rev-parse", "--abbrev-ref", "HEAD"), workspaceDir)
        val branch = branchResult.stdout.trim().ifBlank { "unknown" }

        val statusResult = runGitCommand(listOf("git", "status", "--porcelain"), workspaceDir)
        val modifiedFiles = statusResult.stdout.lines()
            .filter { it.isNotBlank() }
            .map { it.substring(3).trim() }

        return GitStatus(
            branch = branch,
            clean = modifiedFiles.isEmpty(),
            modifiedFiles = modifiedFiles
        )
    }

    private fun runGitCommand(cmd: List<String>, workDir: Path): GitResult {
        val process = ProcessBuilder(cmd)
            .directory(workDir.toFile())
            .redirectErrorStream(false)
            .start()

        val finished = process.waitFor(120, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw GitOperationException("Git command timed out after 120s: ${cmd.joinToString(" ")}")
        }

        return GitResult(
            exitCode = process.exitValue(),
            stdout = process.inputStream.bufferedReader().readText(),
            stderr = process.errorStream.bufferedReader().readText()
        )
    }

    private data class GitResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )
}

class GitOperationException(message: String) : RuntimeException(message)
