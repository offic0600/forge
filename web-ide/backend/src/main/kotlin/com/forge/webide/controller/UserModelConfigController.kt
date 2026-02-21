package com.forge.webide.controller

import com.forge.webide.service.UserModelConfigRequest
import com.forge.webide.service.UserModelConfigService
import com.forge.webide.service.UserModelConfigView
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

/**
 * 用户模型配置 REST API。
 *
 * 允许用户配置自己的模型提供商参数（API Key、Base URL 等），
 * 覆盖系统默认配置。API Key 加密存储，返回时脱敏。
 */
@RestController
@RequestMapping("/api/user/model-configs")
class UserModelConfigController(
    private val userModelConfigService: UserModelConfigService
) {

    /**
     * 获取当前用户的所有提供商配置。
     */
    @GetMapping
    fun getUserConfigs(principal: Principal?): List<UserModelConfigView> {
        val userId = principal?.name ?: "anonymous"
        return userModelConfigService.getUserConfigs(userId)
    }

    /**
     * 获取当前用户指定提供商的配置。
     */
    @GetMapping("/{provider}")
    fun getUserConfig(
        principal: Principal?,
        @PathVariable provider: String
    ): ResponseEntity<UserModelConfigView> {
        val userId = principal?.name ?: "anonymous"
        val config = userModelConfigService.getUserConfig(userId, provider)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(config)
    }

    /**
     * 保存或更新当前用户的提供商配置。
     */
    @PutMapping("/{provider}")
    fun saveUserConfig(
        principal: Principal?,
        @PathVariable provider: String,
        @RequestBody request: UserModelConfigRequest
    ): UserModelConfigView {
        val userId = principal?.name ?: "anonymous"
        val normalizedRequest = request.copy(provider = provider)
        return userModelConfigService.saveUserConfig(userId, normalizedRequest)
    }

    /**
     * 删除当前用户指定提供商的配置。
     */
    @DeleteMapping("/{provider}")
    fun deleteUserConfig(
        principal: Principal?,
        @PathVariable provider: String
    ): ResponseEntity<Void> {
        val userId = principal?.name ?: "anonymous"
        userModelConfigService.deleteUserConfig(userId, provider)
        return ResponseEntity.noContent().build()
    }
}
