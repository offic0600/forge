package com.forge.webide.repository

import com.forge.webide.entity.UserModelConfigEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserModelConfigRepository : JpaRepository<UserModelConfigEntity, String> {

    fun findByUserId(userId: String): List<UserModelConfigEntity>

    fun findByUserIdAndProvider(userId: String, provider: String): UserModelConfigEntity?

    fun deleteByUserIdAndProvider(userId: String, provider: String)
}
