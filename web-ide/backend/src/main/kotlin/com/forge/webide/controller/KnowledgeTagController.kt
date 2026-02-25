package com.forge.webide.controller

import com.forge.webide.model.*
import com.forge.webide.service.KnowledgeTagService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/knowledge/tags")
class KnowledgeTagController(
    private val knowledgeTagService: KnowledgeTagService
) {

    @GetMapping
    fun listTags(@RequestParam(required = false) workspaceId: String?): ResponseEntity<List<KnowledgeTag>> {
        return ResponseEntity.ok(knowledgeTagService.listTags(workspaceId))
    }

    @GetMapping("/{tagId}")
    fun getTag(@PathVariable tagId: String): ResponseEntity<KnowledgeTag> {
        val tag = knowledgeTagService.getTag(tagId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(tag)
    }

    @PostMapping
    fun createTag(@RequestBody request: CreateKnowledgeTagRequest): ResponseEntity<KnowledgeTag> {
        val tag = knowledgeTagService.createTag(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(tag)
    }

    @PutMapping("/{tagId}")
    fun updateTag(
        @PathVariable tagId: String,
        @RequestBody request: UpdateKnowledgeTagRequest
    ): ResponseEntity<KnowledgeTag> {
        val tag = knowledgeTagService.updateTag(tagId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(tag)
    }

    @DeleteMapping("/{tagId}")
    fun deleteTag(@PathVariable tagId: String): ResponseEntity<Void> {
        val deleted = knowledgeTagService.deleteTag(tagId)
        return if (deleted) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }

    @PostMapping("/import")
    fun importBaseline(): ResponseEntity<Map<String, Any>> {
        val imported = knowledgeTagService.importFromBaseline()
        return ResponseEntity.ok(mapOf(
            "imported" to imported,
            "tags" to knowledgeTagService.listTags()
        ))
    }

    @PutMapping("/reorder")
    fun reorderTags(@RequestBody request: ReorderKnowledgeTagsRequest): ResponseEntity<List<KnowledgeTag>> {
        val tags = knowledgeTagService.reorderTags(request.tagIds)
        return ResponseEntity.ok(tags)
    }

    @GetMapping("/search")
    fun searchTags(
        @RequestParam q: String,
        @RequestParam(required = false) workspaceId: String?
    ): ResponseEntity<List<KnowledgeTag>> {
        return ResponseEntity.ok(knowledgeTagService.searchTags(q, workspaceId))
    }
}
