package com.aichat.app.domain

/**
 * 通用结果封装（仿 Rust Result）。
 *
 * 所有引擎/仓库的失败统一收敛到 [AppError]，便于上层统一处理与上报。
 */
sealed interface Result<out T> {
    /** 成功，携带值。 */
    data class Ok<out T>(val value: T) : Result<T>

    /** 失败，携带错误。 */
    data class Err(val error: AppError) : Result<Nothing>
}

/**
 * 应用错误层次。
 */
sealed interface AppError {
    /** 模型加载失败。 */
    data class ModelLoadFailed(val message: String) : AppError

    /** 资源（模型/知识库）拷贝失败。 */
    data class AssetCopyFailed(val message: String) : AppError

    /** 推理失败。 */
    data class InferenceFailed(val message: String) : AppError

    /** 语音转写失败。 */
    data class TranscriptionFailed(val message: String) : AppError

    /** 知识库加载失败。 */
    data class RagLoadFailed(val message: String) : AppError
}
