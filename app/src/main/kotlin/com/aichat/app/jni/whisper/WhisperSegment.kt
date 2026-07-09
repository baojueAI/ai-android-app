package com.aichat.app.jni.whisper

/**
 * 单段语音识别结果。
 *
 * 字段顺序与 C++ 侧 `new_segment_callback` 构造对象时使用的
 * `(IFFLjava/lang/String;)V` 构造签名**逐字对应**：
 * (index:Int, startTime:Float, endTime:Float, text:String)。
 *
 * @param index 分段序号（从 0 开始）
 * @param startTime 起始时间（秒）
 * @param endTime 结束时间（秒）
 * @param text 该段识别文本
 */
data class WhisperSegment(
    val index: Int,
    val startTime: Float,
    val endTime: Float,
    val text: String
)
