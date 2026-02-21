package com.forge.webide.controller

import com.forge.adapter.model.ModelInfo
import com.forge.adapter.model.ModelRegistry
import com.forge.adapter.model.RegistrySummary
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for model discovery and management.
 *
 * Provides endpoints for:
 * - Listing all available models across providers
 * - Listing models by provider
 * - Querying provider health status
 */
@RestController
@RequestMapping("/api/models")
class ModelController(
    private val modelRegistry: ModelRegistry
) {

    @GetMapping
    fun listAllModels(): ResponseEntity<List<ModelInfo>> {
        return ResponseEntity.ok(modelRegistry.allModels())
    }

    @GetMapping("/providers")
    fun listProviders(): ResponseEntity<RegistrySummary> {
        return ResponseEntity.ok(modelRegistry.summary())
    }

    @GetMapping("/providers/{provider}")
    fun listModelsByProvider(@PathVariable provider: String): ResponseEntity<List<ModelInfo>> {
        val models = modelRegistry.modelsForProvider(provider)
        if (models.isEmpty()) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(models)
    }

    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Boolean>> {
        val results = runBlocking { modelRegistry.healthCheckAll() }
        return ResponseEntity.ok(results)
    }
}
