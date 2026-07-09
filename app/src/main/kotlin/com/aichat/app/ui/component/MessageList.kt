package com.aichat.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aichat.app.R
import com.aichat.app.domain.model.ChatMessage
import com.aichat.app.domain.model.Role

/**
 * 消息列表（竖直滚动）。
 *
 * - 按 [ChatMessage.createdAt] 升序展示；
 * - 新消息到达时自动滚动到底部；
 * - 无消息时显示空态提示。
 *
 * @param messages 消息列表
 * @param isGenerating 是否正在生成（用于驱动最后一条 AI 气泡的光标/思考指示）
 */
@Composable
fun MessageList(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 新消息时自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = R.string.empty_chat_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val sorted = messages.sortedBy { it.createdAt }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = sorted,
                    key = { it.id }
                ) { message ->
                    val isLast = sorted.last() == message
                    val isStreaming = isGenerating && message.role == Role.ASSISTANT && isLast
                    MessageBubble(message = message, isStreaming = isStreaming)
                }
            }
        }
    }
}
