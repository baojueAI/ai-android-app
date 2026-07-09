package com.aichat.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aichat.app.data.repository.SettingsRepository
import com.aichat.app.di.AppModule
import com.aichat.app.ui.AppNavHost
import com.aichat.app.ui.theme.AIChatTheme

/**
 * 主 Activity：挂载 Compose 根（[AppNavHost]），并按设置选择深色模式；
 * 申请 RECORD_AUDIO 权限（拒绝仍可文字对话）。
 */
class MainActivity : ComponentActivity() {

    /** 录音权限申请器：被拒绝时给出轻量提示，不阻塞文字对话。 */
    private val recordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, R.string.permission_mic_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 申请录音权限（端侧语音识别需要）；用户可拒绝，应用降级为纯文字对话。
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            val settingsRepository = AppModule.getSettingsRepository()
            val darkMode by settingsRepository.darkModeFlow.collectAsStateWithLifecycle(
                initialValue = SettingsRepository.DARK_MODE_SYSTEM
            )
            // 深色模式：跟随系统 / 强制浅色 / 强制深色
            val darkTheme = when (darkMode) {
                SettingsRepository.DARK_MODE_LIGHT -> false
                SettingsRepository.DARK_MODE_DARK -> true
                else -> isSystemInDarkTheme()
            }
            AIChatTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
