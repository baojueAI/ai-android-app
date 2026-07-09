package com.aichat.app.engine.model

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.aichat.app.config.ModelPaths
import com.aichat.app.data.repository.dataStore
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * 模型管理器：负责在首启时将 assets 中的模型文件拷贝到
 * `filesDir/models/`，并通过 DataStore 记录模型版本号，仅在版本变化或文件缺失时重拷。
 *
 * 若模型文件已直接存在于 `filesDir/models/`（例如用户自行放置或首次联网拉取），
 * 则跳过拷贝步骤直接返回其绝对路径。
 *
 * @param context 应用上下文
 */
class ModelManager(context: Context) {

    /** LLM 与 ASR 模型的绝对路径。 */
    data class ModelLocations(
        val llamaPath: String,
        val whisperPath: String
    )

    private val assetExtractor = AssetExtractor(context.assets)
    private val dataStore = context.dataStore
    private val filesDir = context.filesDir

    /**
     * 确保模型就绪并返回其绝对路径。
     *
     * @param version 当前期望的模型版本号；与 DataStore 中记录不一致则触发拷贝
     * @param onProgress 进度回调（0..100），来自两次拷贝进度的归并
     * @throws IllegalStateException 当模型文件最终仍不存在（未下载/未放入 assets）
     */
    suspend fun ensureModels(
        version: Int,
        onProgress: suspend (Int) -> Unit = {}
    ): ModelLocations {
        val modelsDir = File(filesDir, ModelPaths.ASSETS_MODELS_DIR).apply { mkdirs() }
        val llamaFile = File(modelsDir, ModelPaths.LLAMA_MODEL_FILE)
        val whisperFile = File(modelsDir, ModelPaths.WHISPER_MODEL_FILE)

        val storedVersion = dataStore.data.first()[MODEL_VERSION] ?: -1
        val needCopy = storedVersion != version || !llamaFile.exists() || !whisperFile.exists()

        if (needCopy) {
            if (assetExtractor.assetExists(ModelPaths.ASSETS_MODELS_DIR, ModelPaths.LLAMA_MODEL_FILE)) {
                assetExtractor.extract(
                    "${ModelPaths.ASSETS_MODELS_DIR}/${ModelPaths.LLAMA_MODEL_FILE}",
                    llamaFile
                ).collect { onProgress((it * 50) / 100) }
            }
            if (assetExtractor.assetExists(ModelPaths.ASSETS_MODELS_DIR, ModelPaths.WHISPER_MODEL_FILE)) {
                assetExtractor.extract(
                    "${ModelPaths.ASSETS_MODELS_DIR}/${ModelPaths.WHISPER_MODEL_FILE}",
                    whisperFile
                ).collect { onProgress(50 + (it * 50) / 100) }
            }
            dataStore.edit { it[MODEL_VERSION] = version }
        } else {
            onProgress(100)
        }

        if (!llamaFile.exists() || !whisperFile.exists()) {
            throw IllegalStateException(
                "模型文件缺失：${llamaFile.absolutePath} / ${whisperFile.absolutePath}。" +
                    "请将 ${ModelPaths.LLAMA_MODEL_FILE} 与 ${ModelPaths.WHISPER_MODEL_FILE} " +
                    "放入 assets/models/ 或应用的 filesDir/models/ 目录（见 assets/models/README.md）。"
            )
        }
        return ModelLocations(llamaFile.absolutePath, whisperFile.absolutePath)
    }

    private companion object {
        val MODEL_VERSION = intPreferencesKey("model_version")
    }
}
