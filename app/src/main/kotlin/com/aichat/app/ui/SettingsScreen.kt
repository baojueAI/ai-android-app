package com.aichat.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aichat.app.R
import com.aichat.app.data.repository.SettingsRepository
import com.aichat.app.di.AppModule
import com.aichat.app.di.SettingsViewModelFactory
import com.aichat.app.ui.component.TopBar
import com.aichat.app.ui.viewmodel.SettingsViewModel

/**
 * 设置页：模型信息、温度、最大 Token、深色模式、联网兜底、知识库版本。
 *
 * 全部读写通过 [SettingsViewModel] 回写到 DataStore：
 * 温度 / 深色模式 / 联网兜底持久化；最大 Token 仅 UI 态。
 *
 * @param onBack 返回聊天页的回调
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(AppModule.getSettingsRepository())
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(id = R.string.title_settings),
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 模型信息
            SettingsSection(title = stringResource(id = R.string.label_model_info)) {
                Text(
                    text = prettyModelName(state.modelName),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(id = R.string.model_info_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 温度
            SettingsSection(title = stringResource(id = R.string.label_temperature)) {
                Text(
                    text = "%.2f".format(state.temperature),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = state.temperature,
                    onValueChange = viewModel::setTemperature,
                    valueRange = 0f..1f
                )
            }

            // 最大 Token
            SettingsSection(title = stringResource(id = R.string.label_max_tokens)) {
                val options = listOf(256, 512, 1024)
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = state.maxTokens == option,
                                onClick = { viewModel.setMaxTokens(option) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.maxTokens == option,
                            onClick = { viewModel.setMaxTokens(option) }
                        )
                        Text(text = option.toString())
                    }
                }
            }

            // 深色模式
            SettingsSection(title = stringResource(id = R.string.label_dark_mode)) {
                val modes = listOf(
                    SettingsRepository.DARK_MODE_SYSTEM to stringResource(id = R.string.dark_system),
                    SettingsRepository.DARK_MODE_LIGHT to stringResource(id = R.string.dark_light),
                    SettingsRepository.DARK_MODE_DARK to stringResource(id = R.string.dark_dark)
                )
                modes.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = state.darkMode == mode,
                                onClick = { viewModel.setDarkMode(mode) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.darkMode == mode,
                            onClick = { viewModel.setDarkMode(mode) }
                        )
                        Text(text = label)
                    }
                }
            }

            // 联网兜底
            SettingsSection(title = stringResource(id = R.string.label_network_fallback)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.network_fallback_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.networkFallback,
                        onCheckedChange = viewModel::setNetworkFallback
                    )
                }
            }

            // 知识库版本
            SettingsSection(title = stringResource(id = R.string.label_knowledge)) {
                Text(
                    text = stringResource(id = R.string.knowledge_version_value),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 设置分组容器：标题 + 内容。
 */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

/**
 * 把 GGUF 文件名美化为可读标签。
 * 例：“Phi-3-mini-4K-Instruct-Q4_K_M.gguf” → “Phi-3-mini · Q4_K_M”
 */
private fun prettyModelName(raw: String): String {
    val noExt = raw.substringBeforeLast(".gguf", missingDelimiterValue = raw)
    return noExt.replace("-4K-Instruct", "").replace("-", " · ")
}
