package com.aichat.app.config

/**
 * 应用级全局常量。
 */
object Constants {
    const val APP_NAME = "AI 对话"

    /** DataStore 中存放“已拷贝模型版本号”的键。 */
    const val MODEL_VERSION_KEY = "model_version"

    /** DataStore 中存放“知识库版本号”的键（预留）。 */
    const val KNOWLEDGE_VERSION_KEY = "knowledge_version"

    /** 联网兜底开关键（预留，默认关闭）。 */
    const val NETWORK_FALLBACK_KEY = "network_fallback"

    /** 深色模式偏好键（0=跟随系统,1=浅色,2=深色）。 */
    const val DARK_MODE_KEY = "dark_mode"
}
