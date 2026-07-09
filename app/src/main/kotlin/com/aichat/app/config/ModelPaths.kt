package com.aichat.app.config

/**
 * 模型与知识库资源路径约定。
 *
 * 这些常量必须与 [android.content.res.AssetManager] 中的目录结构、
 * [app.src.main.cpp.CMakeLists] 的 aaptOptions 配置（noCompress "gguf","bin"）
 * 以及 `assets/models/README.md` 中的文件名说明**完全一致**。
 */
object ModelPaths {
    /** assets 下模型目录名（首启拷贝到 filesDir/models）。 */
    const val ASSETS_MODELS_DIR = "models"

    /** assets 下知识库目录名。 */
    const val ASSETS_KNOWLEDGE_DIR = "knowledge"

    /** LLM 模型文件名：Phi-3-mini 4K 指令版 Q4_K_M 量化。 */
    const val LLAMA_MODEL_FILE = "Phi-3-mini-4K-Instruct-Q4_K_M.gguf"

    /** ASR 模型文件名：whisper base。 */
    const val WHISPER_MODEL_FILE = "ggml-base.bin"

    /** 知识库文件匹配模式。 */
    const val KNOWLEDGE_GLOB = "*.md"
}
