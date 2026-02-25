package com.forge.webide.controller

import com.forge.webide.entity.WorkspaceEntity
import com.forge.webide.model.CreateWorkspaceRequest
import com.forge.webide.model.McpContent
import com.forge.webide.model.McpToolCallResponse
import com.forge.webide.repository.WorkspaceRepository
import com.forge.webide.service.GitService
import com.forge.webide.service.KnowledgeTagService
import com.forge.webide.service.McpProxyService
import com.forge.webide.service.WorkspaceService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Optional

class ContextControllerTest {

    private val workspaceRepository = mockk<WorkspaceRepository>(relaxed = true)
    private val gitService = mockk<GitService>(relaxed = true)
    private val mcpProxyService = mockk<McpProxyService>(relaxed = true)

    private lateinit var workspaceService: WorkspaceService
    private lateinit var controller: ContextController

    @TempDir
    lateinit var tempDir: Path

    private val entityStore = mutableMapOf<String, WorkspaceEntity>()

    @BeforeEach
    fun setup() {
        entityStore.clear()

        val entitySlot = slot<WorkspaceEntity>()
        every { workspaceRepository.save(capture(entitySlot)) } answers {
            val entity = entitySlot.captured
            entityStore[entity.id] = entity
            entity
        }
        every { workspaceRepository.findById(any()) } answers {
            val id = firstArg<String>()
            Optional.ofNullable(entityStore[id])
        }
        every { workspaceRepository.count() } answers { entityStore.size.toLong() }

        val knowledgeTagService = mockk<KnowledgeTagService>(relaxed = true)
        workspaceService = WorkspaceService(workspaceRepository, gitService, knowledgeTagService, tempDir.toString())
        workspaceService.init()

        controller = ContextController(workspaceService, mcpProxyService)
    }

    @Test
    fun `search files returns workspace files`() {
        val ws = workspaceService.createWorkspace(
            CreateWorkspaceRequest(name = "ctx-test"), "testuser"
        )

        val results = controller.search("files", null, ws.id)

        assertThat(results).isNotEmpty()
        assertThat(results.all { it.type == "file" }).isTrue()
    }

    @Test
    fun `search files filters by query`() {
        val ws = workspaceService.createWorkspace(
            CreateWorkspaceRequest(name = "ctx-filter"), "testuser"
        )

        val results = controller.search("files", "index", ws.id)

        assertThat(results).isNotEmpty()
        assertThat(results.all { it.label.contains("index", ignoreCase = true) || it.description?.contains("index", ignoreCase = true) == true }).isTrue()
    }

    @Test
    fun `search files returns empty when no workspaceId`() {
        val results = controller.search("files", null, null)

        assertThat(results).isEmpty()
    }

    @Test
    fun `search knowledge delegates to McpProxyService`() {
        every {
            mcpProxyService.callTool("search_knowledge", mapOf("query" to "architecture"))
        } returns McpToolCallResponse(
            content = listOf(McpContent(
                type = "text",
                text = """
                    Found 1 document matching 'architecture':

                    1. **Forge Architecture**
                       Path: conventions/forge-architecture.md
                       Overview of the platform design
                """.trimIndent()
            )),
            isError = false
        )

        val results = controller.search("knowledge", "architecture", null)

        assertThat(results).isNotEmpty()
        assertThat(results[0].type).isEqualTo("knowledge")
        assertThat(results[0].label).isEqualTo("Forge Architecture")
    }

    @Test
    fun `search knowledge returns empty on error`() {
        every {
            mcpProxyService.callTool("search_knowledge", any())
        } returns McpToolCallResponse(
            content = listOf(McpContent(type = "text", text = "Error occurred")),
            isError = true
        )

        val results = controller.search("knowledge", "test", null)

        assertThat(results).isEmpty()
    }

    @Test
    fun `search services delegates to McpProxyService`() {
        every {
            mcpProxyService.callTool("get_service_info", mapOf("service" to ""))
        } returns McpToolCallResponse(
            content = listOf(McpContent(
                type = "text",
                text = "=== backend ===\nSpring Boot 3 on port 8080\n=== frontend ===\nNext.js 15 on port 3000"
            )),
            isError = false
        )

        val results = controller.search("services", null, null)

        assertThat(results).hasSize(2)
        assertThat(results.map { it.label }).containsExactly("backend", "frontend")
    }

    @Test
    fun `search unknown category returns empty`() {
        val results = controller.search("unknown", null, null)

        assertThat(results).isEmpty()
    }
}
