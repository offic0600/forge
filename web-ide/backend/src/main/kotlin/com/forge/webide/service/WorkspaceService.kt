package com.forge.webide.service

import com.forge.webide.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages workspace lifecycle including creation, activation (starting
 * code-server pods), suspension, and deletion.
 *
 * In a production deployment, this service would interact with Kubernetes
 * to manage workspace pods. For now, it uses in-memory storage.
 */
@Service
class WorkspaceService {

    private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)

    private val workspaces = ConcurrentHashMap<String, Workspace>()
    private val fileTrees = ConcurrentHashMap<String, List<FileNode>>()
    private val fileContents = ConcurrentHashMap<String, MutableMap<String, String>>()

    fun createWorkspace(request: CreateWorkspaceRequest, userId: String): Workspace {
        val workspace = Workspace(
            name = request.name,
            description = request.description ?: "",
            status = WorkspaceStatus.ACTIVE,
            owner = userId,
            repository = request.repository,
            branch = request.branch ?: "main"
        )

        workspaces[workspace.id] = workspace
        initializeDefaultFiles(workspace.id)

        logger.info("Created workspace '${workspace.name}' (${workspace.id}) for user $userId")
        return workspace
    }

    fun listWorkspaces(userId: String): List<Workspace> {
        return workspaces.values
            .filter { it.owner == userId || it.owner.isEmpty() || it.owner == "anonymous" }
            .sortedByDescending { it.updatedAt }
    }

    fun getWorkspace(id: String): Workspace? {
        return workspaces[id]
    }

    fun deleteWorkspace(id: String, userId: String) {
        val workspace = workspaces[id] ?: return
        if (workspace.owner != userId && workspace.owner.isNotEmpty() && workspace.owner != "anonymous") {
            throw IllegalAccessException("User $userId cannot delete workspace $id")
        }

        // In production: tear down K8s pod
        workspaces.remove(id)
        fileTrees.remove(id)
        fileContents.remove(id)

        logger.info("Deleted workspace $id")
    }

    fun activateWorkspace(id: String): Workspace? {
        val workspace = workspaces[id] ?: return null

        // In production: create/start K8s pod for code-server
        val activated = workspace.copy(
            status = WorkspaceStatus.ACTIVE,
            updatedAt = Instant.now()
        )
        workspaces[id] = activated

        logger.info("Activated workspace $id")
        return activated
    }

    fun suspendWorkspace(id: String): Workspace? {
        val workspace = workspaces[id] ?: return null

        // In production: scale down K8s pod
        val suspended = workspace.copy(
            status = WorkspaceStatus.SUSPENDED,
            updatedAt = Instant.now()
        )
        workspaces[id] = suspended

        logger.info("Suspended workspace $id")
        return suspended
    }

    fun getFileTree(workspaceId: String): List<FileNode> {
        return fileTrees[workspaceId] ?: emptyList()
    }

    fun getFileContent(workspaceId: String, path: String): String? {
        return fileContents[workspaceId]?.get(path)
    }

    fun saveFileContent(workspaceId: String, path: String, content: String) {
        fileContents.getOrPut(workspaceId) { mutableMapOf() }[path] = content

        // Update the workspace timestamp
        workspaces[workspaceId]?.let {
            workspaces[workspaceId] = it.copy(updatedAt = Instant.now())
        }
    }

    fun createFile(workspaceId: String, path: String, content: String) {
        fileContents.getOrPut(workspaceId) { mutableMapOf() }[path] = content
        rebuildFileTree(workspaceId)
    }

    fun deleteFile(workspaceId: String, path: String) {
        val files = fileContents[workspaceId] ?: return
        // Remove exact match (single file)
        files.remove(path)
        // Also remove all files under this path (folder deletion)
        val prefix = "$path/"
        files.keys.removeIf { it.startsWith(prefix) }
        rebuildFileTree(workspaceId)
    }

    private fun initializeDefaultFiles(workspaceId: String) {
        val files = mutableMapOf<String, String>()

        files["src/index.ts"] = """
            |// Forge Workspace - Entry Point
            |
            |import { Application } from './app';
            |
            |async function main() {
            |    const app = new Application();
            |    await app.start();
            |    console.log('Application started successfully');
            |}
            |
            |main().catch(console.error);
        """.trimMargin()

        files["src/app.ts"] = """
            |export class Application {
            |    private name: string;
            |
            |    constructor() {
            |        this.name = 'forge-app';
            |    }
            |
            |    async start(): Promise<void> {
            |        console.log(`Starting ${'$'}{this.name}...`);
            |    }
            |}
        """.trimMargin()

        files["package.json"] = """
            |{
            |    "name": "forge-workspace",
            |    "version": "1.0.0",
            |    "main": "dist/index.js",
            |    "scripts": {
            |        "build": "tsc",
            |        "start": "node dist/index.js",
            |        "dev": "ts-node src/index.ts"
            |    }
            |}
        """.trimMargin()

        files["tsconfig.json"] = """
            |{
            |    "compilerOptions": {
            |        "target": "ES2022",
            |        "module": "commonjs",
            |        "outDir": "./dist",
            |        "strict": true
            |    },
            |    "include": ["src/**/*"]
            |}
        """.trimMargin()

        files["README.md"] = """
            |# Forge Workspace
            |
            |This is a new Forge workspace.
            |
            |## Getting Started
            |
            |```bash
            |npm install
            |npm run dev
            |```
        """.trimMargin()

        fileContents[workspaceId] = files
        rebuildFileTree(workspaceId)
    }

    private fun rebuildFileTree(workspaceId: String) {
        val files = fileContents[workspaceId] ?: return

        // Mutable intermediate node for building nested tree
        class MutableNode(
            val name: String,
            val path: String,
            var type: FileType,
            var size: Long? = null,
            val childMap: MutableMap<String, MutableNode> = mutableMapOf()
        )

        val root = MutableNode("", "", FileType.DIRECTORY)

        for (filePath in files.keys.sorted()) {
            val parts = filePath.split("/")
            var current = root
            for (i in parts.indices) {
                val part = parts[i]
                val isLast = i == parts.size - 1
                val childPath = parts.take(i + 1).joinToString("/")

                if (isLast) {
                    // Leaf file node
                    current.childMap[part] = MutableNode(
                        name = part,
                        path = childPath,
                        type = FileType.FILE,
                        size = files[filePath]?.length?.toLong()
                    )
                } else {
                    // Intermediate directory node
                    current = current.childMap.getOrPut(part) {
                        MutableNode(name = part, path = childPath, type = FileType.DIRECTORY)
                    }
                }
            }
        }

        fun toFileNode(node: MutableNode): FileNode {
            return if (node.childMap.isNotEmpty()) {
                FileNode(
                    name = node.name,
                    path = node.path,
                    type = FileType.DIRECTORY,
                    children = node.childMap.values
                        .map { toFileNode(it) }
                        .sortedWith(compareBy<FileNode> { it.type != FileType.DIRECTORY }.thenBy { it.name })
                )
            } else if (node.type == FileType.DIRECTORY) {
                FileNode(name = node.name, path = node.path, type = FileType.DIRECTORY, children = emptyList())
            } else {
                FileNode(name = node.name, path = node.path, type = FileType.FILE, size = node.size)
            }
        }

        fileTrees[workspaceId] = root.childMap.values
            .map { toFileNode(it) }
            .sortedWith(compareBy<FileNode> { it.type != FileType.DIRECTORY }.thenBy { it.name })
    }
}
