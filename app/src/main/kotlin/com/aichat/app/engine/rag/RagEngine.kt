package com.aichat.app.engine.rag

import com.aichat.app.domain.model.KnowledgeChunk

/**
 * 知识库检索引擎接口（RAG-lite）。
 */
interface RagEngine {

    /** 启动时加载知识库（构建内存倒排索引）。 */
    suspend fun load()

    /**
     * 检索与查询最相关的片段。
     * @param query 用户问题
     * @param topK 返回的最大片段数
     * @return 按 BM25 打分降序排列的 [KnowledgeChunk]
     */
    fun retrieve(query: String, topK: Int): List<KnowledgeChunk>
}
