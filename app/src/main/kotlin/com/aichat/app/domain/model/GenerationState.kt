package com.aichat.app.domain.model

/**
 * 生成状态机（供 UI 观察，T05 接入）。
 */
sealed interface GenerationState {
    /** 空闲。 */
    data object Idle : GenerationState

    /** 生成中，携带已累计文本。 */
    data class Generating(val text: String) : GenerationState

    /** 生成完成。 */
    data class Done(val text: String) : GenerationState

    /** 出错。 */
    data class Error(val message: String) : GenerationState
}
