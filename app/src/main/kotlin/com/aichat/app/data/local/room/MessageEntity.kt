package com.aichat.app.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.Role

/**
 * 消息持久化实体。
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["session_id"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val role: String,
    val content: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

/** 实体 -> 领域模型。 */
fun MessageEntity.toDomain(): ChatMessage =
    ChatMessage(id, sessionId, Role.valueOf(role), content, createdAt)

/** 领域模型 -> 实体（role 以 name 存储）。 */
fun ChatMessage.toEntity(): MessageEntity =
    MessageEntity(id, sessionId, role.name, content, createdAt)
