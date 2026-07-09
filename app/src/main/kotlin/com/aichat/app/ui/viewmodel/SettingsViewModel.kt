package com.aichat.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 设置界面 ViewModel：持有 [SettingsRepository]，
 * 把 DataStore 中的键（temperature / maxTokens / darkMode / networkFallback）映射到 [SettingsUiState]，
 * 并通过 set* 方法写回。
 */
class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 将持久化键映射到 UI 状态
        settingsRepository.temperatureFlow
            .onEach { _uiState.update { s -> s.copy(temperature = it) } }
            .launchIn(viewModelScope)
        settingsRepository.maxTokensFlow
            .onEach { _uiState.update { s -> s.copy(maxTokens = it) } }
            .launchIn(viewModelScope)
        settingsRepository.darkModeFlow
            .onEach { _uiState.update { s -> s.copy(darkMode = it) } }
            .launchIn(viewModelScope)
        settingsRepository.networkFallbackFlow
            .onEach { _uiState.update { s -> s.copy(networkFallback = it) } }
            .launchIn(viewModelScope)
    }

    /** 设置温度（0–1）。 */
    fun setTemperature(value: Float) {
        viewModelScope.launch { settingsRepository.setTemperature(value.coerceIn(0f, 1f)) }
    }

    /** 设置最大生成 Token（持久化到 DataStore，影响下一次生成）。 */
    fun setMaxTokens(value: Int) {
        viewModelScope.launch { settingsRepository.setMaxTokens(value) }
    }

    /** 设置深色模式（0=跟随系统, 1=浅色, 2=深色）。 */
    fun setDarkMode(value: Int) {
        viewModelScope.launch { settingsRepository.setDarkMode(value) }
    }

    /** 设置联网兜底开关（默认关闭）。 */
    fun setNetworkFallback(value: Boolean) {
        viewModelScope.launch { settingsRepository.setNetworkFallback(value) }
    }
}
