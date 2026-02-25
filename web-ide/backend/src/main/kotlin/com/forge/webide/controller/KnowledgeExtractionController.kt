package com.forge.webide.controller

import com.forge.webide.model.ExtractionTriggerRequest
import com.forge.webide.service.KnowledgeExtractionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/knowledge/extraction")
class KnowledgeExtractionController(
    private val extractionService: KnowledgeExtractionService
) {

    @PostMapping("/trigger")
    fun triggerExtraction(@RequestBody request: ExtractionTriggerRequest): ResponseEntity<Map<String, String>> {
        val jobId = extractionService.triggerExtraction(request)
        return ResponseEntity.ok(mapOf("jobId" to jobId))
    }

    @GetMapping("/jobs/{jobId}")
    fun getJobStatus(@PathVariable jobId: String): ResponseEntity<Any> {
        val status = extractionService.getJobStatus(jobId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(status)
    }

    @GetMapping("/logs")
    fun getLogs(
        @RequestParam(required = false) tagId: String?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<Any> {
        val logs = extractionService.getLogs(tagId, limit)
        return ResponseEntity.ok(logs)
    }
}
