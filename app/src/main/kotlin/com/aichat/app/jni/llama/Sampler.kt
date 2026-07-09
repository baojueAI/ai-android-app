package com.aichat.app.jni.llama

import com.aichat.app.config.AppConfig

/**
 * 采样参数（Bean），对应 llama.cpp 的采样配置子集。
 *
 * @param temperature 温度，越高越随机；默认 [AppConfig.DEFAULT_TEMPERATURE]
 * @param topP nucleus 采样阈值，默认 [AppConfig.DEFAULT_TOP_P]
 * @param topK 仅保留概率最高的 K 个候选（用于 C++ 端扩展）
 * @param repeatPenalty 重复惩罚，抑制重复生成
 */
data class Sampler(
    val temperature: Float = AppConfig.DEFAULT_TEMPERATURE,
    val topP: Float = AppConfig.DEFAULT_TOP_P,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f
) {
    init {
        require(temperature > 0f) { "temperature 必须 > 0" }
        require(topP in 0f..1f) { "topP 必须在 [0,1]" }
        require(topK >= 0) { "topK 必须 >= 0" }
        require(repeatPenalty > 0f) { "repeatPenalty 必须 > 0" }
    }
}
