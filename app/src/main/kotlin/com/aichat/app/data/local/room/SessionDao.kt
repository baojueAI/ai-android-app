package com.aichat.app.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 会话 DAO。
 */
@Dao
interface SessionDao {

    /** 会话列表，按最近更新倒序。 */
    @Query("SELECT * FROM sessions ORDER BY updated_at DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    /** 插入或更新（按主键冲突替换）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    /** 更新某会话的最近活动时间（用于助手消息后刷新排序）。 */
    @Query("UPDATE sessions SET updated_at = :updatedAt WHERE id = :id")
    suspend fun touchSession(id: String, updatedAt: Long)

    /** 删除会话。 */
    @Delete
    suspend fun delete(session: SessionEntity)
}
