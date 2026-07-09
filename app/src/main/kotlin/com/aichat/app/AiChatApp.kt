package com.aichat.app

import android.app.Application
import com.aichat.app.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口：完成依赖装配（[AppModule.init]）与模型资产预热（异步、不阻塞启动）。
 *
 * 预热仅负责把 assets 中的模型拷贝到 `filesDir/models/` 并记录版本号；
 * 真正的模型加载（llama / whisper 解码器构建、RAG 索引）延迟到进入聊天页时由
 * [com.aichat.app.ui.viewmodel.ChatViewModel.loadModel] 触发。
 */
class AiChatApp : Application() {

    /** 应用级协程作用域（跟随进程生命周期；预热失败不影响 UI）。 */
    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 1) 装配依赖（加载原生库 + 构建引擎/仓库单例）
        AppModule.init(applicationContext)
        // 2) 异步预热：拷贝 assets 中的模型到 filesDir 并记录版本，不阻塞启动
        warmupScope.launch {
            runCatching {
                AppModule.getModelManager().ensureModels(MODEL_VERSION) { /* 进度忽略，ChatViewModel 会再次展示 */ }
            }
        }
    }

    companion object {
        /** 当前内置模型版本号（需与 [com.aichat.app.ui.viewmodel.ChatViewModel] 的 EXPECTED_MODEL_VERSION 保持一致）。 */
        const val MODEL_VERSION = 1
    }
}
