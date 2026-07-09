package com.aichat.app.domain.model

import com.aichat.app.config.AppConfig

/**
 * LLM 推理参数（领域层 Bean）。
 *
 * @param nCtx 上下文窗口
 * @param nThreads 推理线程数
 * @param temperature 采样温度
 * @param topP top-p 阈值
 * @param maxTokens 单次最大生成 token 数
 */
data class LlmParams(
    val nCtx: Int = AppConfig.DEFAULT_N_CTX,
    val nThreads: Int = AppConfig.defaultThreads(),
    val temperature: Float = AppConfig.DEFAULT_TEMPERATURE,
    val topP: Float = AppConfig.DEFAULT_TOP_P,
    val maxTokens: Int = AppConfig.DEFAULT_MAX_TOKENS
)
