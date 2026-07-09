package com.aichat.app.jni.llama

/**
 * 表示单个 token 的轻量数据类。
 *
 * @param id llama.cpp 中的 token id
 * @param text 该 token 对应的文本片段（可能为空或非完整字）
 * @param isEnd 是否为结束 token（如 `<|end|>`、`</s>`）
 */
data class Token(
    val id: Int,
    val text: String,
    val isEnd: Boolean
)
