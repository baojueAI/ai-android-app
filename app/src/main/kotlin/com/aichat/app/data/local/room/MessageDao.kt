package com.aichat.app.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 消息 DAO。
 */
@Dao
interface MessageDao {

    /** 观察某会话消息，按创建时间倒序（最新在前，便于 UI 渲染）。 */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY created_at DESC")
    fun observeMessages(sessionId: String): Flow<List<MessageEntity>>

    /** 获取某会话全部消息，按创建时间正序（构建上下文用）。 */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY created_at ASC")
    suspend fun getMessages(sessionId: String): List<MessageEntity>

    /** 插入或更新。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    /** 删除某会话下全部消息。 */
    @Query("DELETE FROM messages WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
