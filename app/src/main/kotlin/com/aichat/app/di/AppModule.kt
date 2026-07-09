package com.aichat.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aichat.app.data.local.room.ChatDatabase
import com.aichat.app.data.repository.ChatRepository
import com.aichat.app.data.repository.ChatRepositoryImpl
import com.aichat.app.data.repository.SettingsRepository
import com.aichat.app.engine.llm.LlmEngine
import com.aichat.app.engine.llm.LlmEngineImpl
import com.aichat.app.engine.model.AssetExtractor
import com.aichat.app.engine.model.ModelManager
import com.aichat.app.engine.rag.RagEngine
import com.aichat.app.engine.rag.RagEngineImpl
import com.aichat.app.engine.speech.SpeechEngine
import com.aichat.app.engine.speech.SpeechEngineImpl
import com.aichat.app.ui.viewmodel.ChatViewModel
import com.aichat.app.ui.viewmodel.SettingsViewModel

/**
 * 手动依赖注入容器（Service Locator）。
 *
 * 在 [android.app.Application.onCreate] 中调用 [init] 完成：
 * 1) 按统一顺序加载原生库（llama → llama-android → whisper）；
 * 2) 装配仓库 / 引擎 / 模型管理器等单例。
 *
 * 各界面通过 [getChatRepository] / [getSettingsRepository] / [getModelManager]（及引擎）
 * 获取单例，并借助 *ViewModelFactory 构造 ViewModel。
 */
object AppModule {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var assetExtractor: AssetExtractor
    private lateinit var modelManager: ModelManager
    private lateinit var database: ChatDatabase
    private lateinit var llmEngine: LlmEngine
    private lateinit var speechEngine: SpeechEngine
    private lateinit var ragEngine: RagEngine
    private lateinit var chatRepository: ChatRepository

    @Volatile
    private var initialized = false

    /** 初始化容器：按统一顺序加载原生库，再装配引擎与仓库（幂等）。 */
    fun init(context: Context) {
        if (initialized) return
        val ctx = context.applicationContext

        // 原生库加载顺序：llama -> llama-android -> whisper
        // 原生库加载：库不存在时静默跳过，App 正常运行（纯 Kotlin 模式）
        runCatching { System.loadLibrary("llama") }
        runCatching { System.loadLibrary("llama-android") }
        runCatching { System.loadLibrary("whisper") }
        runCatching { System.loadLibrary("whisper-android-bridge") }

        settingsRepository = SettingsRepository(ctx)
        assetExtractor = AssetExtractor(ctx.assets)
        modelManager = ModelManager(ctx)
        database = ChatDatabase.getInstance(ctx)
        llmEngine = LlmEngineImpl()
        speechEngine = SpeechEngineImpl()
        ragEngine = RagEngineImpl(ctx)
        chatRepository = ChatRepositoryImpl(llmEngine, speechEngine, ragEngine, database)

        initialized = true
    }

    private fun checkInit() {
        require(initialized) {
            "AppModule 尚未初始化，请先在 Application.onCreate 调用 AppModule.init(context)"
        }
    }

    fun getChatRepository(): ChatRepository {
        checkInit()
        return chatRepository
    }

    fun getSettingsRepository(): SettingsRepository {
        checkInit()
        return settingsRepository
    }

    fun getModelManager(): ModelManager {
        checkInit()
        return modelManager
    }

    fun getAssetExtractor(): AssetExtractor {
        checkInit()
        return assetExtractor
    }

    fun getLlmEngine(): LlmEngine {
        checkInit()
        return llmEngine
    }

    fun getSpeechEngine(): SpeechEngine {
        checkInit()
        return speechEngine
    }

    fun getRagEngine(): RagEngine {
        checkInit()
        return ragEngine
    }
}

/**
 * [ChatViewModel] 工厂：持有所需的单例依赖，由 [androidx.lifecycle.viewmodel.compose.viewModel]
 * 在需要时装配。
 */
internal class ChatViewModelFactory(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val llmEngine: LlmEngine,
    private val speechEngine: SpeechEngine,
    private val ragEngine: RagEngine,
    private val modelManager: ModelManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == ChatViewModel::class.java) { "不支持的 ViewModel：$modelClass" }
        return ChatViewModel(
            chatRepository = chatRepository,
            settingsRepository = settingsRepository,
            llmEngine = llmEngine,
            speechEngine = speechEngine,
            ragEngine = ragEngine,
            modelManager = modelManager
        ) as T
    }
}

/**
 * [SettingsViewModel] 工厂。
 */
internal class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == SettingsViewModel::class.java) { "不支持的 ViewModel：$modelClass" }
        return SettingsViewModel(settingsRepository = settingsRepository) as T
    }
}


