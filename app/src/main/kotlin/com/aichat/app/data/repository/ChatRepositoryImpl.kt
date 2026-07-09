package com.aichat.app.data.repository

import com.aichat.app.data.local.room.ChatDatabase
import com.aichat.app.data.local.room.MessageEntity
import com.aichat.app.data.local.room.SessionEntity
import com.aichat.app.data.local.room.toDomain
import com.aichat.app.data.local.room.toEntity
import com.aichat.app.domain.AppError
import com.aichat.app.domain.Result
import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.KnowledgeChunk
import com.aichat.app.domain.model.LlmParams
import com.aichat.app.domain.model.Role
import com.aichat.app.domain.model.Session
import com.aichat.app.engine.llm.LlmEngine
import com.aichat.app.engine.rag.RagEngine
import com.aichat.app.engine.speech.SpeechEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 聊天仓库实现：编排 RAG 检索 -> 拼装 system 提示 -> LLM 流式生成 -> 落库。
 *
 * 系统提示词严格按照架构约定：
 * “你是一名完全离线运行的智能助手，仅依据【知识库】内容简洁专业回答；无相关信息如实说明”
 */
class ChatRepositoryImpl(
    private val llmEngine: LlmEngine,
    private val speechEngine: SpeechEngine,
    private val ragEngine: RagEngine,
    private val database: ChatDatabase
) : ChatRepository {

    override suspend fun sendMessage(
        sessionId: String,
        history: List<ChatMessage>,
        params: LlmParams,
        onToken: (String) -> Unit
    ): Result<String> = try {
        // 1) 取用户最新问题做检索
        val query = history.lastOrNull { it.role == Role.USER }?.content
            ?: throw IllegalArgumentException("对话历史中缺少用户消息")
        val chunks: List<KnowledgeChunk> = ragEngine.retrieve(query, RAG_TOP_K)
        val knowledge = chunks.joinToString("\n---\n") { it.text }

        // 2) 拼装 system 提示（含知识库上下文）
        val systemPrompt = buildSystemPrompt(knowledge)
        val fullHistory = listOf(
            ChatMessage(sessionId = sessionId, role = Role.SYSTEM, content = systemPrompt)
        ) + history

        // 3) 流式生成
        val acc = StringBuilder()
        llmEngine.generate(fullHistory, params) { piece ->
            acc.append(piece)
            onToken(piece)
        }
        val answer = acc.toString()

        // 4) 落库
        saveMessage(
            ChatMessage(sessionId = sessionId, role = Role.ASSISTANT, content = answer)
        )
        Result.Ok(answer)
    } catch (e: Exception) {
        Result.Err(AppError.InferenceFailed(e.message ?: "推理失败"))
    }

    override suspend fun transcribe(audio: ByteArray): Result<String> = try {
        Result.Ok(speechEngine.transcribe(audio))
    } catch (e: Exception) {
        Result.Err(AppError.TranscriptionFailed(e.message ?: "语音转写失败"))
    }

    override fun retrieve(query: String, topK: Int): List<KnowledgeChunk> =
        ragEngine.retrieve(query, topK)

    override suspend fun saveMessage(message: ChatMessage) {
        database.messageDao().upsert(message.toEntity())
        if (message.role == Role.USER) {
            database.sessionDao().upsert(
                SessionEntity(
                    id = message.sessionId,
                    title = message.content.take(24).trim(),
                    createdAt = message.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            database.sessionDao().touchSession(message.sessionId, System.currentTimeMillis())
        }
    }

    override suspend fun getMessages(sessionId: String): List<ChatMessage> =
        database.messageDao().getMessages(sessionId).map { it.toDomain() }

    override fun getSessions(): Flow<List<Session>> =
        database.sessionDao().observeSessions().map { list -> list.map { it.toDomain() } }

    /** 按架构约定的系统提示词模板。 */
    private fun buildSystemPrompt(knowledge: String): String =
        "你是一名完全离线运行的智能助手，仅依据【知识库】内容简洁专业回答；无相关信息如实说明。" +
            "\n\n【知识库】\n$knowledge"

    private companion object {
        const val RAG_TOP_K = 3
    }
}
