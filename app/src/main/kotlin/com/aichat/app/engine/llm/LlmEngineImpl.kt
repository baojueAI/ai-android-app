package com.aichat.app.engine.llm

import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.LlmParams
import com.aichat.app.jni.llama.Chat
import com.aichat.app.jni.llama.Llama
import com.aichat.app.jni.llama.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LLM 引擎实现：基于 [Model] 在单一线程上执行解码循环，
 * 通过 [Llama] 的 JNI 桥逐 token 流式回调。
 *
 * 推理与转写均使用 [Dispatchers.Default.limitedParallelism] 限为 1 个并发，
 * 避免移动端多实例推理导致的内存/性能问题。
 */
class LlmEngineImpl : LlmEngine {

    private val stopped = AtomicBoolean(false)
    @Volatile private var model: Model? = null

    // 单线程推理调度器
    private val inferDispatcher = Dispatchers.Default.limitedParallelism(1)

    override suspend fun load(path: String, params: LlmParams) =
        withContext(inferDispatcher) {
            // 先释放旧模型
            model?.close()
            model = Model.loadFromFilePath(
                path,
                Model.Params(
                    nCtx = params.nCtx,
                    nThreads = params.nThreads,
                    temperature = params.temperature,
                    topP = params.topP
                )
            )
        }

    override suspend fun generate(
        messages: List<ChatMessage>,
        params: LlmParams,
        onToken: (String) -> Unit
    ) = withContext(inferDispatcher) {
        val m = model ?: throw IllegalStateException("LLM 模型尚未加载，请先调用 load()")
        stopped.set(false)

        val prompt = Chat.buildPrompt(messages)
        m.generate(prompt, object : Llama.TokenCallback {
            override fun onToken(piece: String) {
                if (stopped.get()) return
                onToken(piece)
            }
        }, params) // 传入 params，让温度/topP/maxTokens 实时生效
    }

    override fun stop() {
        stopped.set(true)
        model?.stop()
    }

    override fun unload() {
        stopped.set(true)
        model?.close()
        model = null
    }
}
