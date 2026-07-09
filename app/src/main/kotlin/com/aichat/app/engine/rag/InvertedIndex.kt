package com.aichat.app.engine.rag

import com.aichat.app.domain.model.KnowledgeChunk
import kotlin.math.ln

/**
 * 内存倒排索引 + BM25 打分。
 *
 * 中文按**字符 2-gram** 分词（无需分词器/词典），ASCII 词按空白与标点切分并小写化。
 * 适合端侧小语料、零依赖的 RAG-lite 场景。
 *
 * @property k1 BM25 饱和度参数（默认 1.5）
 * @property b BM25 长度归一化参数（默认 0.75）
 */
class InvertedIndex(
    private val k1: Float = 1.5f,
    private val b: Float = 0.75f
) {

    /** 单篇文档（一个 .md 文件即一个 chunk）。 */
    private data class Doc(
        val id: Int,
        val text: String,
        val source: String,
        val tokens: List<String>
    )

    /** 倒排记录：某 term 出现在哪些文档、位置（位置用于后续扩展，这里仅计数）。 */
    private data class Posting(val docId: Int, val count: Int)

    private val index = mutableMapOf<String, MutableList<Posting>>()
    private val docs = mutableListOf<Doc>()
    private var totalTokens = 0

    /** 平均文档长度（token 数）。 */
    private val avgLen: Float
        get() = if (docs.isEmpty()) 1f else (totalTokens.toFloat() / docs.size)

    /**
     * 加入一篇文档。
     * @param text 正文
     * @param source 来源文件名
     */
    fun addDocument(text: String, source: String) {
        val tokens = tokenize(text)
        val docId = docs.size
        docs.add(Doc(docId, text, source, tokens))
        totalTokens += tokens.size

        val termCounts = tokens.groupingBy { it }.eachCount()
        for ((term, count) in termCounts) {
            val list = index.getOrPut(term) { mutableListOf() }
            list.add(Posting(docId, count))
        }
    }

    /**
     * BM25 检索。
     * @param query 查询文本
     * @param topK 返回的最大片段数
     * @return 按打分降序的 [KnowledgeChunk]
     */
    fun search(query: String, topK: Int): List<KnowledgeChunk> {
        val qTerms = tokenize(query).distinct()
        if (qTerms.isEmpty() || docs.isEmpty()) return emptyList()

        val scores = mutableMapOf<Int, Float>()
        val avg = avgLen
        val n = docs.size.toFloat()

        for (term in qTerms) {
            val postings = index[term] ?: continue
            val df = postings.size
            // IDF（加 1 防止 log(0)）
            val idf = ln((n - df + 0.5f) / (df + 0.5f) + 1f)
            for (p in postings) {
                val doc = docs[p.docId]
                val tf = p.count
                val len = doc.tokens.size
                val denom = tf + k1 * (1 - b + b * len / avg)
                val score = idf * (tf * (k1 + 1) / denom)
                scores[p.docId] = (scores[p.docId] ?: 0f) + score
            }
        }

        return scores.entries
            .sortedByDescending { it.value }
            .take(topK.coerceAtLeast(0))
            .map { (docId, score) ->
                KnowledgeChunk(docs[docId].text, score, docs[docId].source)
            }
    }

    /**
     * 分词：
     * - ASCII 词（字母/数字）：按空白与标点切分、小写化
     * - 连续汉字：按字符 2-gram 切分；单字则整体作为 term
     */
    private fun tokenize(text: String): List<String> {
        val result = mutableListOf<String>()

        // 1) ASCII / 数字词
        text.split(Regex("[\\s\\p{P}]+"))
            .filter { it.isNotBlank() && it.any { c -> c.isLetterOrDigit() } }
            .forEach { result.add(it.lowercase()) }

        // 2) 中文按字符 2-gram
        val cjk = text.filter { it.isChinese() }
        if (cjk.length == 1) {
            result.add(cjk)
        } else {
            for (i in 0 until cjk.length - 1) {
                result.add(cjk.substring(i, i + 2))
            }
        }
        return result
    }

    /** 判断字符是否为 CJK 统一表意文字（含常用扩展区）。 */
    private fun Char.isChinese(): Boolean {
        val cp = code
        return cp in 0x4E00..0x9FFF ||
            cp in 0x3400..0x4DBF ||
            cp in 0x30000..0x3134F
    }
}
