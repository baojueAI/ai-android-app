package com.aichat.app.ui.viewmodel

import com.aichat.app.config.ModelPaths
import com.aichat.app.data.repository.SettingsRepository
import com.aichat.app.domain.AppError
import com.aichat.app.domain.model.ChatMessage

/**
 * 聊天界面 UI 状态。所有可变字段均为不可变数据类成员，
 * 通过 [androidx.lifecycle.viewModelScope] + [kotlinx.coroutines.flow.StateFlow] 驱动重组。
 */
data class ChatUiState(
    /** 当前会话消息列表（按创建时间升序展示）。 */
    val messages: List<ChatMessage> = emptyList(),
    /** 是否正在生成（流式推理中）。 */
    val isGenerating: Boolean = false,
    /** 是否正在录音（语音输入中）。 */
    val isRecording: Boolean = false,
    /** 底部输入框的当前文本。 */
    val inputText: String = "",
    /** 当前需要提示的错误（消费后清空）。 */
    val error: AppError? = null,
    /** 模型是否加载就绪，未就绪时显示加载覆盖层。 */
    val modelReady: Boolean = false,
    /** 模型加载进度 0–100。 */
    val loadProgress: Int = 0
)

/**
 * 设置界面 UI 状态。
 *
 * 注意：[temperature] / [maxTokens] / [darkMode] / [networkFallback] 与 [SettingsRepository] 的
 * DataStore 键一一对应（[SettingsRepository.DEFAULT_TEMPERATURE] 等）。
 * [modelName] 为只读展示，暂未持久化。
 */
data class SettingsUiState(
    val temperature: Float = SettingsRepository.DEFAULT_TEMPERATURE,
    val maxTokens: Int = SettingsRepository.DEFAULT_MAX_TOKENS,
    val darkMode: Int = SettingsRepository.DARK_MODE_SYSTEM,
    val networkFallback: Boolean = false,
    val modelName: String = ModelPaths.LLAMA_MODEL_FILE
)
