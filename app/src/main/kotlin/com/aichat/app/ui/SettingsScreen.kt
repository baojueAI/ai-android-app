package com.aichat.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aichat.app.R
import com.aichat.app.config.ModelPaths
import com.aichat.app.data.repository.SettingsRepository
import com.aichat.app.di.AppModule
import com.aichat.app.di.SettingsViewModelFactory
import com.aichat.app.ui.component.TopBar
import com.aichat.app.ui.viewmodel.SettingsViewModel
import java.io.File

/**
 * 设置页：模型信息、温度、最大 Token、深色模式、联网兜底、模型文件管理。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(AppModule.getSettingsRepository())
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val modelManager = AppModule.getModelManager()

    // 检查模型是否已就绪
    val llamaReady = modelManager.isModelReady(ModelPaths.LLAMA_MODEL_FILE)
    val whisperReady = modelManager.isModelReady(ModelPaths.WHISPER_MODEL_FILE)
    val llamaSize = modelManager.getModelSize(ModelPaths.LLAMA_MODEL_FILE)
    val whisperSize = modelManager.getModelSize(ModelPaths.WHISPER_MODEL_FILE)

    // GGUF 文件选择器
    val ggufPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = modelManager.copyFromUri(context, uri, ModelPaths.LLAMA_MODEL_FILE)
            if (ok) {
                Toast.makeText(context, "Phi-3 模型文件已导入", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "导入失败，请重试", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Whisper 模型文件选择器
    val whisperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = modelManager.copyFromUri(context, uri, ModelPaths.WHISPER_MODEL_FILE)
            if (ok) {
                Toast.makeText(context, "Whisper 模型文件已导入", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "导入失败，请重试", Toast.LENGTH_LONG).show()
            }
        }
    }

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
            // ====== 模型文件管理 ======
            SettingsSection(title = "模型文件") {
                // Phi-3 模型状态
                Text("Phi-3 模型：" + if (llamaReady) "已就绪" else "未导入",
                    style = MaterialTheme.typography.bodyMedium)
                if (llamaSize > 0) {
                    Text("大小：" + String.format("%.1f GB", llamaSize / 1_000_000_000.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("文件名：" + ModelPaths.LLAMA_MODEL_FILE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { ggufPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("选择 Phi-3 模型文件（GGUF）")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Whisper 模型状态
                Text("Whisper 模型：" + if (whisperReady) "已就绪" else "未导入",
                    style = MaterialTheme.typography.bodyMedium)
                if (whisperSize > 0) {
                    Text("大小：" + String.format("%.0f MB", whisperSize / 1_000_000.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("文件名：" + ModelPaths.WHISPER_MODEL_FILE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { whisperPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("选择 Whisper 模型文件（bin）")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 存储路径提示
                Text("内部路径：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(modelManager.getModelsDir(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // 存储权限按钮（Android 11+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = Uri.parse("package:" + context.packageName)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("授予全部文件访问权限")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ====== 模型信息 ======
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

/** 把 GGUF 文件名美化为可读标签。 */
private fun prettyModelName(raw: String): String {
    val noExt = raw.substringBeforeLast(".gguf", missingDelimiterValue = raw)
    return noExt.replace("-4K-Instruct", "").replace("-", " · ")
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