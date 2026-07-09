package com.aichat.app.jni.whisper

/**
 * 一次完整转写的结果聚合。
 *
 * @param segments 各分段结果（按时间顺序）
 * @param text 拼接后的完整文本（各段文本直接相连）
 */
data class WhisperResult(
    val segments: List<WhisperSegment>,
    val text: String
) {
    companion object {
        /** 由分段列表构建结果（自动拼接全文）。 */
        fun from(segments: List<WhisperSegment>): WhisperResult =
            WhisperResult(segments, segments.joinToString("") { it.text })
    }
}
