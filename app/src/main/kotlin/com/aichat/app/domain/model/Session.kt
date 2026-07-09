package com.aichat.app.domain.model

import java.util.UUID

/**
 * 对话会话（领域模型）。
 *
 * @param id 唯一 id
 * @param title 会话标题（取首条用户消息前若干字）
 * @param createdAt 创建时间戳
 * @param updatedAt 最近更新时间戳
 */
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
