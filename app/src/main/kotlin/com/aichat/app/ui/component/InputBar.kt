package com.aichat.app.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aichat.app.R

/**
 * 底部输入栏。
 *
 * - 🎙️ 麦克风按钮：切换录音态（[onMicClick]），录音中以红色高亮；
 * - 文本输入框：受控于 [text] / [onTextChange]，推理中禁用；
 * - 📤 发送按钮：空闲时发送（[onSend]），推理中变为 ⏹ 停止（[onStop]）。
 *
 * 所有可点击元素的触控区均不小于 48dp。
 */
@Composable
fun InputBar(
    text: String,
    isRecording: Boolean,
    isGenerating: Boolean,
    onTextChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sendEnabled = if (isGenerating) true else text.isNotBlank() && !isRecording

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 麦克风：切换录音态
        IconButton(
            onClick = onMicClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(id = R.string.cd_mic),
                tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 输入框
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            placeholder = { Text(text = stringResource(id = R.string.input_placeholder)) },
            enabled = !isGenerating,
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (sendEnabled) {
                        if (isGenerating) onStop() else onSend()
                    }
                }
            )
        )

        // 发送 / 停止
        IconButton(
            onClick = { if (isGenerating) onStop() else onSend() },
            enabled = sendEnabled,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isGenerating) Icons.Filled.Stop else Icons.Filled.Send,
                contentDescription = stringResource(
                    id = if (isGenerating) R.string.cd_stop else R.string.cd_send
                ),
                tint = if (isGenerating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** 预览：空闲态输入栏。 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun InputBarPreview() {
    MaterialTheme {
        InputBar(
            text = "",
            isRecording = false,
            isGenerating = false,
            onTextChange = {},
            onMicClick = {},
            onSend = {},
            onStop = {}
        )
    }
}
