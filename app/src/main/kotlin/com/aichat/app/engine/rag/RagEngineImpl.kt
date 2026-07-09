package com.aichat.app.engine.rag

import android.content.Context
import com.aichat.app.domain.model.KnowledgeChunk
import com.aichat.app.engine.model.AssetExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RagEngineImpl(
    private val context: Context,
    private val index: InvertedIndex = InvertedIndex(),
    private val extractor: AssetExtractor = AssetExtractor(context.assets)
) : RagEngine {

    private var loaded = false

    override suspend fun load() = withContext(Dispatchers.IO) {
        val dir = "knowledge"
        val files = context.assets.list(dir)?.filter { it.endsWith(".md") } ?: emptyList()
        for (file in files) {
            val text = context.assets.open("$dir/$file").bufferedReader().use { it.readText() }
            index.addDocument(text, file)
        }
        loaded = true
    }

    override fun retrieve(query: String, topK: Int): List<KnowledgeChunk> {
        require(loaded) { "RAG not loaded" }
        return index.search(query, topK)
    }
}


