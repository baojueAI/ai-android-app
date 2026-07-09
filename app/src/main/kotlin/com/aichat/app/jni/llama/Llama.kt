package com.aichat.app.jni.llama

/**
 * llama.cpp 的 JNI 桥接门面类。
 *
 * **加载顺序（必须严格遵守）**：先加载底层 [llama]，再加载本桥 [llama-android]，
 * 最后加载 [whisper-android-bridge]。
 * 顺序与 [Model] / [Whisper] 中的 native 方法所依赖的 native 库一致。
 *
 * 所有 external fun 的方法名必须与
 * pp/src/main/cpp/llama-android-jni.cpp 中的
 * Java_com_aichat_app_jni_llama_Llama_* 函数**逐字一致**。
 */
class Llama private constructor() {

    /** 流式 token 回调接口，由 C++ 侧在生成过程中逐片调用。 */
    interface TokenCallback {
        /** @param piece 当前解码出的文本片段（UTF-8） */
        fun onToken(piece: String)
    }

    companion object {
        init {
            runCatching { System.loadLibrary("llama") }
            runCatching { System.loadLibrary("llama-android") }
            runCatching { System.loadLibrary("whisper-android-bridge") }
        }

        /**
         * 加载 GGUF 模型并构建推理上下文。
         * @return native 句柄指针（0 表示加载失败）
         */
        @JvmStatic
        external fun createModel(
            modelPath: String,
            nCtx: Int,
            nThreads: Int,
            temperature: Float,
            topP: Float
        ): Long

        /**
         * 基于已有上下文做流式生成。每个 token 通过 [callback] 回传文本片段。
         *
         * @param temperature 采样温度（>0），覆盖模型加载时的设置
         * @param topP top-p 截断阈值 (0..1)
         * @param maxTokens 单次生成最大 token 数
         */
        @JvmStatic
        external fun generate(
            modelPtr: Long,
            prompt: String,
            callback: TokenCallback,
            temperature: Float,
            topP: Float,
            maxTokens: Int
        )

        /** 请求中断当前生成（在下一个 token 后生效）。 */
        @JvmStatic
        external fun stop(modelPtr: Long)

        /** 释放模型与上下文。 */
        @JvmStatic
        external fun freeModel(modelPtr: Long)
    }
}
