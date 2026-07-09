package com.aichat.app.engine.speech

/**
 * 语音引擎接口（端侧 ASR）。
 */
interface SpeechEngine {

    /** 加载 ASR 模型。 */
    suspend fun load(path: String)

    /**
     * 开始录音并实时转写；每识别出一段文本即通过 [onSegment] 回传。
     * 与 [stopRecording] 配对使用。
     */
    fun startRecording(onSegment: (String) -> Unit)

    /**
     * 停止录音并返回完整录音的 WAV 字节（含 44 字节头 + 16bit PCM）。
     */
    fun stopRecording(): ByteArray

    /**
     * 对给定的音频（WAV 字节）做一次性转写。
     * @return 识别出的完整文本
     */
    suspend fun transcribe(audio: ByteArray): String
}
