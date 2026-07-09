package com.aichat.app.ui.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.aichat.app.R
import com.aichat.app.domain.AppError

/**
 * 错误提示桥接：消费 [AppError]，通过 Scaffold 的 [SnackbarHostState] 弹出 Snackbar，
 * 提供“重试”动作；消费后由调用方清空错误状态。
 *
 * 与 [com.aichat.app.ui.ChatScreen] 中 Scaffold 的 `snackbarHost` 共享同一个
 * [SnackbarHostState]，从而保证提示出现在界面底部。
 *
 * @param error 当前待提示的错误（为空则不展示）
 * @param snackbarHostState 与 Scaffold 共享的 SnackbarHost 状态
 * @param onRetry 点击“重试”时触发
 * @param onDismiss 错误被关闭（超时/滑走）时触发，通常清空错误
 */
@Composable
fun ErrorSnackbar(
    error: AppError?,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = errorMessage(error),
            actionLabel = context.getString(R.string.action_retry),
            duration = SnackbarDuration.Long
        )
        when (result) {
            SnackbarResult.ActionPerformed -> onRetry()
            SnackbarResult.Dismissed -> onDismiss()
        }
    }
}

/** 将 [AppError] 映射为用户可读的中文描述。 */
private fun errorMessage(error: AppError): String = when (error) {
    is AppError.ModelLoadFailed -> "模型加载失败：${error.message}"
    is AppError.AssetCopyFailed -> "资源拷贝失败：${error.message}"
    is AppError.InferenceFailed -> "生成失败：${error.message}"
    is AppError.TranscriptionFailed -> "语音识别失败：${error.message}"
    is AppError.RagLoadFailed -> "知识库加载失败：${error.message}"
}

