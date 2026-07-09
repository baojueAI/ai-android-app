package com.aichat.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 应用配色（Material 3）。主色采用紫蓝系（brand purple / indigo）。
 * 同时导出 [LightColorScheme] 与 [DarkColorScheme] 两套方案供 [AIChatTheme] 选择。
 */

// 品牌色（紫蓝系）
val BrandPurple = Color(0xFF6C5CE7)
val BrandPurpleLight = Color(0xFF9A8CFF)
val BrandIndigo = Color(0xFF4834D4)

// 浅色方案各角色色
private val LightPrimary = BrandPurple
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFE8E4FF)
private val LightOnPrimaryContainer = Color(0xFF21005D)
private val LightSecondary = Color(0xFF625B71)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightBackground = Color(0xFFFBFAFF)
private val LightOnBackground = Color(0xFF1B1B21)
private val LightSurface = Color(0xFFFBFAFF)
private val LightOnSurface = Color(0xFF1B1B21)
private val LightSurfaceVariant = Color(0xFFE7E0F9)
private val LightOnSurfaceVariant = Color(0xFF49454F)
private val LightError = Color(0xFFB3261E)

// 深色方案各角色色
private val DarkPrimary = BrandPurpleLight
private val DarkOnPrimary = Color(0xFF381E72)
private val DarkPrimaryContainer = Color(0xFF4F378B)
private val DarkOnPrimaryContainer = Color(0xFFEADDFF)
private val DarkSecondary = Color(0xFFCBC2DB)
private val DarkOnSecondary = Color(0xFF332D41)
private val DarkBackground = Color(0xFF1B1B21)
private val DarkOnBackground = Color(0xFFE4E1E9)
private val DarkSurface = Color(0xFF1B1B21)
private val DarkOnSurface = Color(0xFFE4E1E9)
private val DarkSurfaceVariant = Color(0xFF49454F)
private val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
private val DarkError = Color(0xFFF2B8B5)

/** 浅色 ColorScheme。 */
val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
    onError = LightOnPrimary
)

/** 深色 ColorScheme。 */
val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
    onError = DarkOnPrimary
)
