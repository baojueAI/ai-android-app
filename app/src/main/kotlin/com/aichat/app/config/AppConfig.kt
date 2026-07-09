package com.aichat.app.config

import kotlin.math.min

/**
 * 默认推理与系统配置。
 */
object AppConfig {
    /** 默认上下文窗口。 */
    const val DEFAULT_N_CTX = 4096

    /** 上下文窗口硬性上限。 */
    const val MAX_N_CTX = 8192

    /** 默认采样温度。 */
    const val DEFAULT_TEMPERATURE = 0.7f

    /** 默认 top-p 阈值。 */
    const val DEFAULT_TOP_P = 0.95f

    /** 默认最大生成 token 数。 */
    const val DEFAULT_MAX_TOKENS = 2048

    /** 推理线程数：min(CPU 核心数, 4)，兼顾性能与发热。 */
    fun defaultThreads(): Int = min(Runtime.getRuntime().availableProcessors(), 4).coerceAtLeast(1)
}
