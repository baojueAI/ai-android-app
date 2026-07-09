package com.aichat.app.engine.rag

import android.content.Context
import com.aichat.app.config.ModelPaths
import com.aichat.app.domain.model.KnowledgeChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RAG 引擎实现：从 assets/knowledge/*.md 读取语料，构建 [InvertedIndex]，
 * 检索时返回 BM25 排序的 [KnowledgeChunk]。
 *
 * 全部在端侧内存完成，无 embedding 模型、无网络。
 *
 * @param context 应用上下文（用于读取 assets）
 * @param index 倒排索引实现（可注入，便于测试）
 */
class RagEngineImpl(
    private val context: Context,
    private val index: InvertedIndex = InvertedIndex()
) : RagEngine {

    @Volatile private var loaded = false

    override suspend fun load() = withContext(Dispatchers.IO) {
        val dir = ModelPaths.ASSETS_KNOWLEDGE_DIR
        val files = context.assets.list(dir)?.filter { it.endsWith(".md") } ?: emptyList()
        for (file in files) {
            val text = context.assets.open("$dir/$file").bufferedReader().use { it.readText() }
            index.addDocument(text, file)
        }
        loaded = true
    }

    override fun retrieve(query: String, topK: Int): List<KnowledgeChunk> {
        require(loaded) { "RAG 尚未加载，请先调用 load()" }
        return index.search(query, topK)
    }
}
