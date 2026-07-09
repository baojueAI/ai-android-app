package com.aichat.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.config.AppConfig
import com.aichat.app.data.repository.ChatRepository
import com.aichat.app.data.repository.SettingsRepository
import com.aichat.app.domain.AppError
import com.aichat.app.domain.Result
import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.LlmParams
import com.aichat.app.domain.model.Role
import com.aichat.app.engine.llm.LlmEngine
import com.aichat.app.engine.model.ModelManager
import com.aichat.app.engine.rag.RagEngine
import com.aichat.app.engine.speech.SpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 聊天界面 ViewModel：持有 [ChatRepository]、各推理引擎与 [ModelManager]，
 * 负责把仓库层的流式结果逐片写入 [ChatUiState]，驱动 Compose 重组。
 *
 * 调度约定：
 * - 推理 / 转写使用 [Dispatchers.Default.limitedParallelism] 单并发，避免阻塞主线程；
 * - 资产拷贝由 [ModelManager] 内部走 IO 调度；
 * - 状态更新在主线程安全的 [MutableStateFlow] 上进行。
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val llmEngine: LlmEngine,
    private val speechEngine: SpeechEngine,
    private val ragEngine: RagEngine,
    private val modelManager: ModelManager
) : ViewModel() {

    /** 单会话：当前版本为单对话，使用固定 sessionId 以跨重启保留历史。 */
    private val sessionId = "main"

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** 推理/转写使用的单并发调度器。 */
    private val genDispatcher = Dispatchers.Default.limitedParallelism(1)

    /** 当前温度（来自设置），影响生成效果。 */
    private val _temperature = MutableStateFlow(AppConfig.DEFAULT_TEMPERATURE)

    /** 当前最大 token（来自设置），影响生成长度。 */
    private val _maxTokens = MutableStateFlow(AppConfig.DEFAULT_MAX_TOKENS)

    init {
        // 同步设置中的温度
        viewModelScope.launch {
            settingsRepository.temperatureFlow.collect { _temperature.value = it }
        }
        // 同步设置中的最大 token
        viewModelScope.launch {
            settingsRepository.maxTokensFlow.collect { _maxTokens.value = it }
        }
        // 进入时恢复历史消息
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.getMessages(sessionId) }
                .onSuccess { msgs -> _uiState.update { it.copy(messages = msgs) } }
        }
    }

    /** 输入框文本变化。 */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * 发送文本消息：
     * 1) 存用户消息 2) 乐观追加用户消息 + 占位 assistant 消息
     * 3) [ChatRepository.sendMessage] 流式 [onToken] 逐片追加到 assistant 消息 4) 落库
     */
    fun sendText() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isGenerating || !_uiState.value.modelReady) return

        val userMsg = ChatMessage(sessionId = sessionId, role = Role.USER, content = text)
        val history = _uiState.value.messages + userMsg
        val assistantMsg = ChatMessage(sessionId = sessionId, role = Role.ASSISTANT, content = "")

        viewModelScope.launch(genDispatcher) {
            // 落库用户消息
            runCatching { chatRepository.saveMessage(userMsg) }
            // 乐观更新：显示用户消息 + 占位 assistant 消息，并进入生成态
            _uiState.update {
                it.copy(
                    inputText = "",
                    messages = it.messages + userMsg + assistantMsg,
                    isGenerating = true
                )
            }

            val result = chatRepository.sendMessage(
                sessionId = sessionId,
                history = history,
                params = buildParams(),
                onToken = { piece ->
                    _uiState.update { st ->
                        val list = st.messages.toMutableList()
                        val idx = list.lastIndex
                        if (idx >= 0) {
                            list[idx] = list[idx].copy(content = list[idx].content + piece)
                        }
                        st.copy(messages = list)
                    }
                }
            )
            _uiState.update { it.copy(isGenerating = false) }
            if (result is Result.Err) {
                _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    /** 开始语音输入：启动录音（whisper 模型需已加载）。 */
    fun startVoiceInput() {
        if (_uiState.value.isRecording || !_uiState.value.modelReady) return
        try {
            // 实时音频片段暂未用于中间显示，传空回调
            speechEngine.startRecording { /* 实时片段未使用 */ }
            _uiState.update { it.copy(isRecording = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = AppError.TranscriptionFailed(e.message ?: "无法启动录音")) }
        }
    }

    /** 停止语音输入：结束录音并转写，回填到输入框。 */
    fun stopVoiceInput() {
        if (!_uiState.value.isRecording) return
        viewModelScope.launch(genDispatcher) {
            try {
                val wav = speechEngine.stopRecording()
                _uiState.update { it.copy(isRecording = false) }
                when (val res = chatRepository.transcribe(wav)) {
                    is Result.Ok -> _uiState.update { it.copy(inputText = res.value) }
                    is Result.Err -> _uiState.update { it.copy(error = res.error) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        error = AppError.TranscriptionFailed(e.message ?: "语音转写失败")
                    )
                }
            }
        }
    }

    /** 停止当前生成（请求 LLM 中断）。 */
    fun stopGeneration() {
        if (!_uiState.value.isGenerating) return
        llmEngine.stop()
        _uiState.update { it.copy(isGenerating = false) }
    }

    /**
     * 加载模型：拷贝资产 -> 加载 llama / whisper -> 加载 RAG 索引，
     * 过程中回报进度与就绪状态。幂等：已就绪则直接返回。
     */
    fun loadModel() {
        if (_uiState.value.modelReady) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(loadProgress = 0) }
                val locations = modelManager.ensureModels(EXPECTED_MODEL_VERSION) { progress ->
                    _uiState.update { it.copy(loadProgress = progress) }
                }
                llmEngine.load(locations.llamaPath, buildParams())
                speechEngine.load(locations.whisperPath)
                ragEngine.load()
                _uiState.update { it.copy(modelReady = true, loadProgress = 100) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = AppError.ModelLoadFailed(e.message ?: "模型加载失败"),
                        modelReady = false,
                        loadProgress = 0
                    )
                }
            }
        }
    }

    /** 清空错误提示。 */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** 依据当前设置构造推理参数。 */
    private fun buildParams(): LlmParams = LlmParams(
        nCtx = AppConfig.DEFAULT_N_CTX,
        nThreads = AppConfig.defaultThreads(),
        temperature = _temperature.value,
        topP = AppConfig.DEFAULT_TOP_P,
        maxTokens = _maxTokens.value
    )

    private companion object {
        /** 期望的模型版本号：与 [com.aichat.app.AiChatApp] 预热保持一致。 */
        const val EXPECTED_MODEL_VERSION = 1
    }
}
