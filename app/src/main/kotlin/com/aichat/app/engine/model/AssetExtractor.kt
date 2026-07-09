package com.aichat.app.engine.model

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream

/**
 * 资源（模型）抽取器：把 [AssetManager] 中的大文件以流式方式拷贝到应用私有目录，
 * 并在拷贝过程中回报进度（0..100）。
 *
 * 模型文件（.gguf / .bin）已在 Gradle 中标记为 `noCompress`，可通过
 * [AssetManager.openFd] 获取准确长度用于计算进度。
 *
 * @param assetManager 由 [android.content.Context.getAssets] 获取
 */
class AssetExtractor(private val assetManager: AssetManager) {

    /**
     * 将 assets 中的 [assetRelativePath] 流式拷贝到 [destFile]，回报进度。
     *
     * @param assetRelativePath assets 内相对路径，如 "models/Phi-3-mini-4K-Instruct-Q4_K_M.gguf"
     * @param destFile 目标文件（父目录会被自动创建）
     */
    fun extract(assetRelativePath: String, destFile: File): Flow<Int> = flow {
        emit(0)
        val total = runCatching { assetManager.openFd(assetRelativePath).length }.getOrDefault(0L)
        assetManager.open(assetRelativePath).use { input ->
            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { out ->
                val buffer = ByteArray(BUFFER_SIZE)
                var copied: Long = 0
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    copied += read
                    if (total > 0) {
                        emit(((copied * 100 / total).toInt()).coerceIn(0, 100))
                    }
                }
            }
        }
        emit(100)
    }.flowOn(Dispatchers.IO)

    /** 判断 assets 的 [dir] 目录下是否包含 [fileName]。 */
    fun assetExists(dir: String, fileName: String): Boolean =
        assetManager.list(dir)?.contains(fileName) ?: false

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
