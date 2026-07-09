package com.aichat.app.data.repository

import android.content.Context
import android.net.Uri
import com.aichat.app.config.ModelPaths
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 设置持久化（DataStore preferences）。
 *
 * 提供 temperature / 深色模式 / 联网兜底开关 / 模型版本号 的读写。
 * 默认值：联网兜底关闭、深色模式跟随系统。
 */
class SettingsRepository(private val context: Context) {

    private val ds = context.dataStore

    /** 采样温度。 */
    val temperatureFlow: Flow<Float> = ds.data.map { it[TEMPERATURE] ?: DEFAULT_TEMPERATURE }

    /** 最大生成 token 数。 */
    val maxTokensFlow: Flow<Int> = ds.data.map { it[MAX_TOKENS] ?: DEFAULT_MAX_TOKENS }

    /** 深色模式：0=跟随系统, 1=浅色, 2=深色。 */
    val darkModeFlow: Flow<Int> = ds.data.map { it[DARK_MODE] ?: DARK_MODE_SYSTEM }

    /** 联网兜底开关（默认关闭，保持离线优先）。 */
    val networkFallbackFlow: Flow<Boolean> = ds.data.map { it[NETWORK_FALLBACK] ?: false }

    /** 模型版本号（-1 表示未拷贝/未设置）。 */
    val modelVersionFlow: Flow<Int> = ds.data.map { it[MODEL_VERSION] ?: -1 }

    suspend fun setTemperature(value: Float) = ds.edit { it[TEMPERATURE] = value }
    suspend fun setMaxTokens(value: Int) = ds.edit { it[MAX_TOKENS] = value.coerceIn(64, 2048) }
    suspend fun setDarkMode(mode: Int) = ds.edit { it[DARK_MODE] = mode }
    suspend fun setNetworkFallback(enabled: Boolean) = ds.edit { it[NETWORK_FALLBACK] = enabled }
    suspend fun setModelVersion(version: Int) = ds.edit { it[MODEL_VERSION] = version }

    /**
     * 从系统文件选择器返回的 [Uri] 拷贝模型文件到应用私有目录 models/，
     * 并以 [targetName]（如 Phi-3-mini-4K-Instruct-Q4_K_M.gguf）命名，供 ModelManager 自动发现。
     * 无需申请存储权限（SAF 已授权该 Uri 的读取）。
     */
    suspend fun importModelFromUri(uri: Uri, targetName: String) {
        val destDir = File(context.filesDir, ModelPaths.ASSETS_MODELS_DIR).apply { mkdirs() }
        val dest = File(destDir, targetName)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("无法打开所选文件：$uri")
        input.use { src ->
            FileOutputStream(dest).use { out ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                while (src.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                }
            }
        }
    }

    companion object {
        private val TEMPERATURE = floatPreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val DARK_MODE = intPreferencesKey("dark_mode")
        private val NETWORK_FALLBACK = booleanPreferencesKey("network_fallback")
        private val MODEL_VERSION = intPreferencesKey("model_version")

        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MAX_TOKENS = 512
        const val DARK_MODE_SYSTEM = 0
        const val DARK_MODE_LIGHT = 1
        const val DARK_MODE_DARK = 2
    }
}

/**
 * 模块级 DataStore 实例（preferences，文件名 "settings"）。
 * 供 [com.aichat.app.engine.model.ModelManager] 等共享使用。
 */
internal val Context.dataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(name = "settings")
