path = r"C:\Users\ASUS\Documents\Codex\2026-07-09\new-chat\ai-android-app\app\src\main\kotlin\com\aichat\app\AiChatApp.kt"
content = """package com.aichat.app

import android.app.Application
import com.aichat.app.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口：完成依赖装配（[AppModule.init]）与模型资产预热（异步、不阻塞启动）。
 */
class AiChatApp : Application() {

    /** 应用级协程作用域（跟随进程生命周期；预热失败不影响 UI）。 */
    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 设置全局未捕获异常处理器，闪退时输出日志
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("AiChatApp", "闪退：" + throwable.message, throwable)
        }
        // 1) 装配依赖（加载原生库 + 构建引擎/仓库单例）
        AppModule.init(applicationContext)
        // 2) 异步预热：拷贝 assets 中的模型到 filesDir 并记录版本，不阻塞启动
        warmupScope.launch {
            runCatching {
                AppModule.getModelManager().ensureModels(MODEL_VERSION) { }
            }
        }
    }

    companion object {
        const val MODEL_VERSION = 1
    }
}
"""
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("AiChatApp.kt rewritten OK")
