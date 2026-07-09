package com.aichat.app.data.repository

import com.aichat.app.domain.Result
import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.KnowledgeChunk
import com.aichat.app.domain.model.LlmParams
import com.aichat.app.domain.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓库接口：编排 RAG 检索、LLM 生成与本地持久化，对 UI 层屏蔽引擎细节。
 */
interface ChatRepository {

    /**
     * 发送一条消息并流式生成回复。
     *
     * @param sessionId 会话 id
     * @param history 当前会话历史（不含本次系统提示；系统提示由仓库依据 RAG 注入）
     * @param params 推理参数
     * @param onToken 每个生成文本片段的回调
     * @return [Result.Ok] 携带完整回复；[Result.Err] 携带 [com.aichat.app.domain.AppError]
     */
    suspend fun sendMessage(
        sessionId: String,
        history: List<ChatMessage>,
        params: LlmParams,
        onToken: (String) -> Unit
    ): Result<String>

    /**
     * 语音转写。
     * @return [Result.Ok] 携带识别文本
     */
    suspend fun transcribe(audio: ByteArray): Result<String>

    /**
     * 知识库检索。
     * @param query 查询
     * @param topK 返回片段数
     */
    fun retrieve(query: String, topK: Int): List<KnowledgeChunk>

    /** 保存一条消息（同时维护会话记录）。 */
    suspend fun saveMessage(message: ChatMessage)

    /** 获取某会话全部消息（按时间正序）。 */
    suspend fun getMessages(sessionId: String): List<ChatMessage>

    /** 观察会话列表（按最近更新倒序）。 */
    fun getSessions(): Flow<List<Session>>
}
