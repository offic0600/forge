package com.forge.webide.service

import com.forge.webide.entity.UserModelConfigEntity
import com.forge.webide.repository.UserModelConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 用户模型配置服务。
 *
 * 管理用户级别的模型提供商配置（API Key、Base URL 等），
 * 支持覆盖系统默认配置。API Key 通过 [EncryptionService] 加密存储。
 */
@Service
class UserModelConfigService(
    private val repository: UserModelConfigRepository,
    private val encryptionService: EncryptionService
) {

    private val logger = LoggerFactory.getLogger(UserModelConfigService::class.java)

    /**
     * 获取用户的所有提供商配置（API Key 脱敏返回）。
     */
    fun getUserConfigs(userId: String): List<UserModelConfigView> {
        return repository.findByUserId(userId).map { it.toView() }
    }

    /**
     * 获取用户指定提供商的配置。
     */
    fun getUserConfig(userId: String, provider: String): UserModelConfigView? {
        return repository.findByUserIdAndProvider(userId, provider)?.toView()
    }

    /**
     * 保存或更新用户的提供商配置。
     */
    @Transactional
    fun saveUserConfig(userId: String, request: UserModelConfigRequest): UserModelConfigView {
        val existing = repository.findByUserIdAndProvider(userId, request.provider)

        val entity = if (existing != null) {
            existing.apply {
                apiKeyEncrypted = if (request.apiKey.isNotBlank()) {
                    encryptionService.encrypt(request.apiKey)
                } else {
                    apiKeyEncrypted // 不更新空 key
                }
                baseUrl = request.baseUrl
                region = request.region
                enabled = request.enabled
                updatedAt = Instant.now()
            }
        } else {
            UserModelConfigEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                provider = request.provider,
                apiKeyEncrypted = encryptionService.encrypt(request.apiKey),
                baseUrl = request.baseUrl,
                region = request.region,
                enabled = request.enabled
            )
        }

        val saved = repository.save(entity)
        logger.info("用户 {} 保存模型配置: provider={}", userId, request.provider)
        return saved.toView()
    }

    /**
     * 删除用户指定提供商的配置。
     */
    @Transactional
    fun deleteUserConfig(userId: String, provider: String) {
        repository.deleteByUserIdAndProvider(userId, provider)
        logger.info("用户 {} 删除模型配置: provider={}", userId, provider)
    }

    /**
     * 获取用户指定提供商的解密 API Key（内部调用，不暴露给前端）。
     */
    fun getDecryptedApiKey(userId: String, provider: String): String? {
        val entity = repository.findByUserIdAndProvider(userId, provider) ?: return null
        if (!entity.enabled || entity.apiKeyEncrypted.isBlank()) return null
        return encryptionService.decrypt(entity.apiKeyEncrypted)
    }

    private fun UserModelConfigEntity.toView() = UserModelConfigView(
        provider = provider,
        hasApiKey = apiKeyEncrypted.isNotBlank(),
        apiKeyMasked = maskApiKey(apiKeyEncrypted),
        baseUrl = baseUrl,
        region = region,
        enabled = enabled,
        updatedAt = updatedAt.toString()
    )

    private fun maskApiKey(encrypted: String): String {
        if (encrypted.isBlank()) return ""
        return try {
            val decrypted = encryptionService.decrypt(encrypted)
            if (decrypted.length <= 8) "****"
            else "${decrypted.take(4)}****${decrypted.takeLast(4)}"
        } catch (e: Exception) {
            "****"
        }
    }
}

/**
 * 用户模型配置请求
 */
data class UserModelConfigRequest(
    val provider: String,
    val apiKey: String = "",
    val baseUrl: String = "",
    val region: String = "",
    val enabled: Boolean = true
)

/**
 * 用户模型配置视图（API Key 脱敏）
 */
data class UserModelConfigView(
    val provider: String,
    val hasApiKey: Boolean,
    val apiKeyMasked: String,
    val baseUrl: String,
    val region: String,
    val enabled: Boolean,
    val updatedAt: String
)
