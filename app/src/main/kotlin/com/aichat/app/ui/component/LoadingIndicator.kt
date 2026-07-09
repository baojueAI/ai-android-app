package com.aichat.app.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aichat.app.R

/**
 * “思考中”三点跳动指示，用于推理进行中（尚无任何字符产出时）。
 * 同时供 [MessageBubble] 复用。
 */
@Composable
fun ThinkingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val alpha = transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(8.dp).alpha(alpha.value)
            ) { }
        }
    }
}

/**
 * 全屏模型加载进度覆盖层。
 *
 * 在模型尚未就绪时覆盖整个聊天界面，显示环形进度与百分比，
 * 避免用户在模型加载期间误操作。
 *
 * @param progress 加载进度 0–100
 */
@Composable
fun ModelLoadingOverlay(
    progress: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(progress = progress.coerceIn(0, 100) / 100f)
            Text(
                text = stringResource(id = R.string.model_loading, progress.coerceIn(0, 100)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/** 预览：三点跳动指示。 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ThinkingDotsPreview() {
    MaterialTheme {
        ThinkingDots()
    }
}

