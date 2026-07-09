package com.aichat.app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aichat.app.R

/**
 * 通用顶部应用栏。
 *
 * - 默认标题为应用名（[R.string.app_name]），可通过 [title] 覆盖（设置页用“设置”）。
 * - 可选返回按钮：当 [onBackClick] 非空时显示（设置页返回）。
 * - 可选“设置”入口：当 [onSettingsClick] 非空时显示（聊天页右上角）。
 *
 * @param title 标题文本；为空时回退到应用名
 * @param onBackClick 返回回调；为空则不显示返回箭头
 * @param onSettingsClick 设置回调；为空则不显示设置图标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    val resolvedTitle = title ?: stringResource(id = R.string.app_name)
    TopAppBar(
        modifier = modifier,
        title = { Text(text = resolvedTitle) },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back)
                    )
                }
            }
        },
        actions = {
            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(id = R.string.cd_settings)
                    )
                }
            }
        }
    )
}

/** 预览：聊天页顶栏（带设置入口）。 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun TopBarPreview() {
    TopBar(onSettingsClick = {})
}
