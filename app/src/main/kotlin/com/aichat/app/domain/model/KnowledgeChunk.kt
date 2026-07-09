package com.aichat.app.domain.model

/**
 * 知识库检索出的单条片段。
 *
 * @param text 片段正文
 * @param score BM25 打分（越高越相关）
 * @param source 来源文件名（如 doc1_多节点对讲系统.md）
 */
data class KnowledgeChunk(
    val text: String,
    val score: Float,
    val source: String
)
