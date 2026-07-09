package com.aichat.app.engine.model

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
 * 1. 应用内部目录 filesDir/models/（已经 copy 过的）
 * 2. APK assets/models/（开发者打包在 APK 中）
 * 3. 手机下载目录 Download/（用户自行下载）
 *
 * 从 Download 找到后会自动复制到内部目录供后续使用。
 */
class ModelManager(context: Context) {

    data class ModelLocations(
        val llamaPath: String,
        val whisperPath: String
    )

    private val appContext = context.applicationContext
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
                copyFromDownload(appContext, ModelPaths.LLAMA_MODEL_FILE, llamaFile)
                onProgress(60)
            }
            if (!whisperFile.exists()) {
                copyFromDownload(appContext, ModelPaths.WHISPER_MODEL_FILE, whisperFile)
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
                "请把下载好的模型文件放到手机「下载 (Download)」文件夹，\n" +
                "然后在系统设置中授予本应用「所有文件管理权限」，\n" +
                "最后重新打开 App。\n" +
                "预期路径：Download/${ModelPaths.LLAMA_MODEL_FILE}\n" +
                "         Download/${ModelPaths.WHISPER_MODEL_FILE}"
            )
        }
        return ModelLocations(llamaFile.absolutePath, whisperFile.absolutePath)
    }

    /**
     * 从手机 Download 目录复制模型文件到内部目录。
     * 使用多种策略兼容不同 Android 版本。
     */
    private fun copyFromDownload(context: Context, fileName: String, destFile: File) {
        // 策略1：通过 Environment API 获取 Download 目录（API < 30，或 >=30 且有 MANAGE_EXTERNAL_STORAGE）
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val sourceFile = File(downloadDir, fileName)
            if (sourceFile.exists() && sourceFile.length() > 0) {
                copyFile(sourceFile, destFile)
                return
            }
        } catch (_: Exception) {
            // 静默降级
        }

        // 策略2：通过 MediaStore Downloads API 查找（API 29+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val downloadedFile = findFileViaMediaStore(context, fileName)
                if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0) {
                    copyFile(downloadedFile, destFile)
                    return
                }
            } catch (_: Exception) {
                // 静默降级
            }
        }

        // 策略3：尝试常见下载目录的几种变体
        val altPaths = listOf(
            "/sdcard/Download/$fileName",
            "/sdcard/download/$fileName",
            "/storage/emulated/0/Download/$fileName",
            "/storage/emulated/0/download/$fileName"
        )
        for (altPath in altPaths) {
            try {
                val altFile = File(altPath)
                if (altFile.exists() && altFile.length() > 0) {
                    copyFile(altFile, destFile)
                    return
                }
            } catch (_: Exception) {
                // 继续尝试下一个
            }
        }
    }

    /** 通过 MediaStore Downloads 集合查找文件（API 29+）。 */
    private fun findFileViaMediaStore(context: Context, fileName: String): File? {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.DATA
        )
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val dataIndex = cursor.getColumnIndex(MediaStore.Downloads.DATA)
                if (dataIndex >= 0) {
                    val filePath = cursor.getString(dataIndex)
                    if (filePath != null) {
                        val file = File(filePath)
                        if (file.exists() && file.length() > 0) {
                            return file
                        }
                    }
                }
            }
        }
        return null
    }

    private fun copyFile(source: File, dest: File) {
        dest.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    private companion object {
        val MODEL_VERSION = intPreferencesKey("model_version")
    }
}
