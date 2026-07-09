package com.aichat.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aichat.app.di.AppModule
import com.aichat.app.di.ChatViewModelFactory
import com.aichat.app.domain.AppError
import com.aichat.app.ui.component.ErrorSnackbar
import com.aichat.app.ui.component.InputBar
import com.aichat.app.ui.component.MessageList
import com.aichat.app.ui.component.ModelLoadingOverlay
import com.aichat.app.ui.component.TopBar
import com.aichat.app.ui.viewmodel.ChatViewModel

/**
 * 聊天主界面：组合 TopBar / MessageList / InputBar / 加载覆盖层 / 错误 Snackbar。
 *
 * 进入界面时通过 [ChatViewModel.loadModel] 触发模型加载（幂等：已就绪直接返回）。
 *
 * @param onSettingsClick 点击设置入口的回调（导航到设置页）
 */
@Composable
fun ChatScreen(
    onSettingsClick: () -> Unit,
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            chatRepository = AppModule.getChatRepository(),
            settingsRepository = AppModule.getSettingsRepository(),
            llmEngine = AppModule.getLlmEngine(),
            speechEngine = AppModule.getSpeechEngine(),
            ragEngine = AppModule.getRagEngine(),
            modelManager = AppModule.getModelManager()
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 进入聊天页即加载模型（幂等）
    LaunchedEffect(Unit) {
        viewModel.loadModel()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopBar(onSettingsClick = onSettingsClick)
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                InputBar(
                    text = state.inputText,
                    isRecording = state.isRecording,
                    isGenerating = state.isGenerating,
                    onTextChange = viewModel::onInputChange,
                    onMicClick = {
                        if (state.isRecording) viewModel.stopVoiceInput()
                        else viewModel.startVoiceInput()
                    },
                    onSend = viewModel::sendText,
                    onStop = viewModel::stopGeneration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        ) { innerPadding ->
            MessageList(
                messages = state.messages,
                isGenerating = state.isGenerating,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }

        // 模型未就绪时显示全屏加载覆盖层
        if (!state.modelReady) {
            ModelLoadingOverlay(
                progress = state.loadProgress,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 错误提示（与 Scaffold 的 SnackbarHost 协调）
        ErrorSnackbar(
            error = state.error,
            snackbarHostState = snackbarHostState,
            onRetry = {
                when (state.error) {
                    is AppError.ModelLoadFailed -> viewModel.loadModel()
                    else -> viewModel.clearError()
                }
            },
            onDismiss = viewModel::clearError
        )
    }
}
