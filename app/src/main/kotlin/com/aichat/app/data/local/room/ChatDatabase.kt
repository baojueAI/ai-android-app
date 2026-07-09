package com.aichat.app.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 应用数据库（Room）。
 *
 * 包含 [SessionEntity] 与 [MessageEntity]，并通过 [sessionDao] / [messageDao] 暴露访问。
 * 采用单例（双重检查锁）避免重复构建。
 */
@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao

    companion object {
        private const val DB_NAME = "aichat.db"

        @Volatile
        private var INSTANCE: ChatDatabase? = null

        /** 获取单例数据库实例。 */
        fun getInstance(context: Context): ChatDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
