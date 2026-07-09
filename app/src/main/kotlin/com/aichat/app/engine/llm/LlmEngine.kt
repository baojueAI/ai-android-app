package com.aichat.app.engine.llm

import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.LlmParams

/**
 * LLM 推理引擎接口（端侧）。
 */
interface LlmEngine {

    /**
     * 加载模型。
     * @param path 模型绝对路径（由 [com.aichat.app.engine.model.ModelManager] 提供）
     * @param params 推理参数
     */
    suspend fun load(path: String, params: LlmParams)

    /**
     * 基于对话历史生成回复，逐 token 通过 [onToken] 流式回传文本片段。
     *
     * @param messages 对话历史（调用方应已注入 RAG system 提示）
     * @param params 推理参数
     * @param onToken 每个文本片段的回调（在推理调度线程上触发）
     */
    suspend fun generate(
        messages: List<ChatMessage>,
        params: LlmParams,
        onToken: (String) -> Unit
    )

    /** 请求中断当前生成。 */
    fun stop()

    /** 卸载模型并释放 native 资源。 */
    fun unload()
}
