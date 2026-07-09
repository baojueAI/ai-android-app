package com.aichat.app.jni.llama

import com.aichat.app.config.AppConfig
import com.aichat.app.domain.model.LlmParams

/**
 * 端侧 LLM 模型封装。
 *
 * 内部持有 [Llama] 返回的 native 指针（llama_model + llama_context 句柄），
 * 对外提供 [generate] / [stop] / [close] 等高层方法，屏蔽 JNI 细节。
 */
class Model private constructor(
    private val nativePtr: Long,
    val path: String,
    val params: Model.Params
) {

    /**
     * 模型加载参数（Bean）。
     *
     * @param nCtx 上下文窗口，默认 [AppConfig.DEFAULT_N_CTX]，上限 [AppConfig.MAX_N_CTX]
     * @param nThreads 推理线程数，默认 [AppConfig.defaultThreads]（min(核心数, 4)）
     * @param temperature 采样温度
     * @param topP top-p 截断阈值
     */
    data class Params(
        val nCtx: Int = AppConfig.DEFAULT_N_CTX,
        val nThreads: Int = AppConfig.defaultThreads(),
        val temperature: Float = AppConfig.DEFAULT_TEMPERATURE,
        val topP: Float = AppConfig.DEFAULT_TOP_P
    ) {
        init {
            require(nCtx in 1..AppConfig.MAX_N_CTX) { "nCtx 必须在 1..${AppConfig.MAX_N_CTX} 之间" }
            require(nThreads >= 1) { "nThreads 必须 >= 1" }
            require(temperature > 0f) { "temperature 必须 > 0" }
            require(topP in 0f..1f) { "topP 必须在 [0,1] 之间" }
        }
    }

    /**
     * 流式生成；每个片段通过 [callback] 回传。
     * 可选的 [llmParams] 覆盖此次生成的温度/topP/maxTokens（若不传则使用加载时的默认值）。
     */
    fun generate(prompt: String, callback: Llama.TokenCallback, llmParams: LlmParams? = null) {
        Llama.generate(
            nativePtr, prompt, callback,
            temperature = llmParams?.temperature ?: params.temperature,
            topP = llmParams?.topP ?: params.topP,
            maxTokens = llmParams?.maxTokens ?: AppConfig.DEFAULT_MAX_TOKENS
        )
    }

    /** 请求中断当前生成。 */
    fun stop() = Llama.stop(nativePtr)

    /** 释放 native 资源（重复调用安全）。 */
    fun close() {
        if (nativePtr != 0L) {
            Llama.freeModel(nativePtr)
        }
    }

    companion object {
        /**
         * 从文件路径加载 Phi-3 GGUF 模型并构建上下文。
         * @throws IllegalStateException 当 native 句柄为 0（模型加载失败）
         */
        fun loadFromFilePath(path: String, params: Params = Params()): Model {
            val ptr = Llama.createModel(
                path,
                params.nCtx,
                params.nThreads,
                params.temperature,
                params.topP
            )
            require(ptr != 0L) { "加载模型失败：$path（检查文件是否存在或格式是否正确）" }
            return Model(ptr, path, params)
        }
    }
}
