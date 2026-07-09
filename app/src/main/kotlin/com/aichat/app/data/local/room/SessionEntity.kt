package com.aichat.app.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aichat.app.domain.model.Session

/**
 * 会话持久化实体。
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

/** 实体 -> 领域模型。 */
fun SessionEntity.toDomain(): Session = Session(id, title, createdAt, updatedAt)

/** 领域模型 -> 实体。 */
fun Session.toEntity(): SessionEntity = SessionEntity(id, title, createdAt, updatedAt)
