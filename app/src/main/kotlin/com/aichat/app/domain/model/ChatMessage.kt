package com.aichat.app.domain.model

import java.util.UUID

/**
 * 单条对话消息（领域模型，与持久化实体解耦）。
 *
 * @param id 唯一 id（默认随机生成）
 * @param sessionId 所属会话 id
 * @param role 角色（USER / ASSISTANT / SYSTEM）
 * @param content 文本内容（Markdown 友好）
 * @param createdAt 创建时间戳（毫秒）
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: Role,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
