package com.aichat.app.engine.model

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.aichat.app.config.ModelPaths
import com.aichat.app.data.repository.dataStore
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 模型管理器：按以下顺序寻找模型文件——
 * 1. 应用内部目录 filesDir/models/
 * 2. 手机下载目录 Download/
 *
 * 从 Download 找到后会自动复制到内部目录供后续使用。
 */
class ModelManager(context: Context) {

    data class ModelLocations(
        val llamaPath: String,
        val whisperPath: String
    )

    private val assetExtractor = AssetExtractor(context.assets)
    private val dataStore = context.dataStore
    private val filesDir = context.filesDir

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
            var progress = 0

            // 方案1：从 APK assets 中提取（开发者打包模型进 APK 时使用）
            if (!llamaFile.exists()) {
                if (assetExtractor.assetExists(ModelPaths.ASSETS_MODELS_DIR, ModelPaths.LLAMA_MODEL_FILE)) {
                    assetExtractor.extract(
                        "${ModelPaths.ASSETS_MODELS_DIR}/${ModelPaths.LLAMA_MODEL_FILE}",
                        llamaFile
                    ).collect { onProgress(it / 2) }
                    progress = 50
                }
            } else { progress = 50 }

            if (!whisperFile.exists()) {
                if (assetExtractor.assetExists(ModelPaths.ASSETS_MODELS_DIR, ModelPaths.WHISPER_MODEL_FILE)) {
                    assetExtractor.extract(
                        "${ModelPaths.ASSETS_MODELS_DIR}/${ModelPaths.WHISPER_MODEL_FILE}",
                        whisperFile
                    ).collect { onProgress(50 + it / 2) }
                }
            }

            // 方案2：从手机 Download 目录复制（用户自行下载时使用）
            if (!llamaFile.exists()) {
                copyFromDownload(ModelPaths.LLAMA_MODEL_FILE, llamaFile)
                onProgress(60)
            }
            if (!whisperFile.exists()) {
                copyFromDownload(ModelPaths.WHISPER_MODEL_FILE, whisperFile)
                onProgress(100)
            }

            dataStore.edit { it[MODEL_VERSION] = version }
        } else {
            onProgress(100)
        }

        if (!llamaFile.exists() || !whisperFile.exists()) {
            val missing = mutableListOf<String>()
            if (!llamaFile.exists()) missing.add(ModelPaths.LLAMA_MODEL_FILE)
            if (!whisperFile.exists()) missing.add(ModelPaths.WHISPER_MODEL_FILE)
            throw IllegalStateException(
                "模型文件缺失：${missing.joinToString("、")}。\n" +
                "请把下载好的模型文件放到手机「下载」文件夹，然后重新打开 App。"
            )
        }
        return ModelLocations(llamaFile.absolutePath, whisperFile.absolutePath)
    }

    /** 从手机 Download 目录复制模型文件到内部目录 */
    private fun copyFromDownload(fileName: String, destFile: File) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val sourceFile = File(downloadDir, fileName)
        if (!sourceFile.exists()) return

        destFile.parentFile?.mkdirs()
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private companion object {
        val MODEL_VERSION = intPreferencesKey("model_version")
    }
}
