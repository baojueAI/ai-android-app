package com.aichat.app.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.Role
import dev.jeziellago.compose.markdowntext.MarkdownText

/**
 * 单条消息气泡。
 *
 * - 用户消息：右对齐，使用主题 [MaterialTheme.colorScheme.primary]；
 * - AI 消息：左对齐，使用 [MaterialTheme.colorScheme.surface]。
 * - AI 正文使用 Markdown 渲染（兼容链接/代码/列表等富文本）。
 * - 当 [isStreaming] 为真且内容为空时显示“思考中”三点跳动指示；
 *   否则在正文末尾显示闪烁光标，体现流式生成。
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == Role.USER
    val horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        val containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser && isStreaming && message.content.isBlank()) {
                    // 模型尚未吐出任何字符：显示“思考中”三点跳动
                    ThinkingDots(modifier = Modifier.padding(4.dp))
                } else {
                    if (message.content.isNotBlank()) {
                        MarkdownText(
                            markdown = message.content,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (isStreaming) {
                        BlinkingCursor(modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

/** 闪烁光标：体现流式输出进行中。 */
@Composable
private fun BlinkingCursor(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    Text(
        text = "▍",
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.alpha(alpha.value)
    )
}

/** 预览：用户气泡。 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun UserBubblePreview() {
    MaterialTheme {
        MessageBubble(
            message = com.aichat.app.domain.model.ChatMessage(
                sessionId = "s",
                role = Role.USER,
                content = "你好，离线助手"
            ),
            isStreaming = false
        )
    }
}
