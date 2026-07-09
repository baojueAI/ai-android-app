package com.aichat.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * 应用主题入口。
 *
 * @param darkTheme 是否使用深色方案
 * @param dynamicColor 是否启用 Android 12+ 动态取色（默认关闭，保证离线品牌一致性）
 * @param content Compose 内容
 */
@Composable
fun AIChatTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 让状态栏 / 导航栏跟随主题背景色，并实现深色图标切换
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(color = colorScheme.background, darkIcons = !darkTheme)
    systemUiController.setNavigationBarColor(color = colorScheme.background, darkIcons = !darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
