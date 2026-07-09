package com.aichat.app.jni.whisper

/**
 * whisper.cpp 的 JNI 桥接门面类。
 *
 * 所有 `external fun` 方法名必须与
 * `app/src/main/cpp/whisper-android-jni.cpp` 中的
 * `Java_com_aichat_app_jni_whisper_Whisper_*` 函数**逐字一致**。
 *
 * 注意：native 库 [whisper] 由 [com.aichat.app.jni.llama.Llama] 伴侣对象统一加载，
 * 本类不再重复加载，避免加载顺序问题。
 */
class Whisper private constructor(
    private val nativePtr: Long,
    val path: String
) {

    /** 分段识别回调接口，由 C++ 侧在转写过程中逐段调用。 */
    interface SegmentCallback {
        fun onSegment(segment: WhisperSegment)
    }

    /**
     * 转写给定的 16kHz 单声道 PCM Float 数据。
     *
     * @param pcmFloat16k 采样率为 16000Hz、单声道的 Float PCM（值域约 [-1, 1]）
     * @param callback 可选的分段回调；无论是否提供，返回结果都会包含全部分段
     * @return 完整分段列表（按时间顺序）
     */
    fun transcribe(pcmFloat16k: FloatArray, callback: SegmentCallback? = null): List<WhisperSegment> {
        val collected = mutableListOf<WhisperSegment>()
        val sink = object : SegmentCallback {
            override fun onSegment(segment: WhisperSegment) {
                collected.add(segment)
                callback?.onSegment(segment)
            }
        }
        transcribeNative(nativePtr, pcmFloat16k, sink)
        return collected
    }

    /** 释放 native 资源（重复调用安全）。 */
    fun close() {
        if (nativePtr != 0L) free(nativePtr)
    }

    companion object {
        init {
            // 与应用统一的加载顺序（幂等）。
            // 即使 Whisper 先于 Llama 被使用，也保证 llama / llama-android / whisper 就绪，
            // 避免单独调用 Whisper 时出现 UnsatisfiedLinkError。
            System.loadLibrary("llama")
            System.loadLibrary("llama-android")
            System.loadLibrary("whisper")
        }

        /**
         * 从文件路径加载 whisper 模型（ggml-base.bin）。
         * @return native 句柄指针（0 表示加载失败）
         */
        @JvmStatic
        external fun create(path: String): Long

        /** 转写（对应 C++ 中的 transcribeNative）。 */
        @JvmStatic
        external fun transcribeNative(ptr: Long, samples: FloatArray, callback: SegmentCallback)

        /** 释放模型。 */
        @JvmStatic
        external fun free(ptr: Long)

        /** 便捷加载：失败抛异常。 */
        fun load(path: String): Whisper {
            val ptr = create(path)
            require(ptr != 0L) { "加载 Whisper 模型失败：$path" }
            return Whisper(ptr, path)
        }
    }
}
