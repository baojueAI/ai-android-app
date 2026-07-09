package com.aichat.app.jni.llama

import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.Role

/**
 * 对话模板与停止序列管理。
 *
 * 负责把 [ChatMessage] 列表拼装为 **Phi-3 ChatML** 格式 prompt，
 * 并提供生成终止判定的停止序列集合。
 *
 * Phi-3 ChatML 示例：
 * ```
 * <|user|>\n问题内容<|end|>\n<|assistant|>\n
 * ```
 */
object Chat {

    /** 生成遇到这些子串即视为结束（与 Phi-3 的结束标记对应）。 */
    val STOP_SEQUENCES: List<String> = listOf("<|end|>", "</s>", "###")

    /** Phi-3 各角色对应的 ChatML 标记。 */
    private const val TAG_USER = "<|user|>"
    private const val TAG_ASSISTANT = "<|assistant|>"
    private const val TAG_SYSTEM = "<|system|>"
    private const val TAG_END = "<|end|>"

    /**
     * 拼装模型输入 prompt。
     *
     * @param messages 对话历史（已包含由上层注入的 system 提示，如有）
     * @param systemPrompt 可选的 system 提示；非空时置于最前
     * @return 可直接送入 [Model.generate] 的 prompt 文本（结尾已带 `<|assistant|>\n`）
     */
    fun buildPrompt(messages: List<ChatMessage>, systemPrompt: String? = null): String {
        val sb = StringBuilder()
        if (!systemPrompt.isNullOrBlank()) {
            sb.append(TAG_SYSTEM).append('\n').append(systemPrompt).append(TAG_END).append('\n')
        }
        for (m in messages) {
            val tag = when (m.role) {
                Role.USER -> TAG_USER
                Role.ASSISTANT -> TAG_ASSISTANT
                Role.SYSTEM -> TAG_SYSTEM
            }
            sb.append(tag).append('\n').append(m.content).append(TAG_END).append('\n')
        }
        // 以 assistant 起始标记收尾，引导模型续写
        sb.append(TAG_ASSISTANT).append('\n')
        return sb.toString()
    }

    /**
     * 判断一段已生成的文本是否命中停止序列。
     * 上层在流式拼接后可用它来截断多余内容。
     */
    fun shouldStop(generated: String): Boolean = STOP_SEQUENCES.any { generated.contains(it) }
}
