package com.aichat.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
 * 主 Activity：挂载 Compose 根（[AppNavHost]），申请运行所需权限。
 *
 * 权限申请：
 * - RECORD_AUDIO：端侧语音识别（拒绝仍可文字对话）
 * - READ_EXTERNAL_STORAGE / MANAGE_EXTERNAL_STORAGE：读取 Download 目录中的模型文件
 */
class MainActivity : ComponentActivity() {

    /** 录音权限申请器。 */
    private val recordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, R.string.permission_mic_denied, Toast.LENGTH_LONG).show()
        }
    }

    /** Android 10 及以下：传统存储权限申请器。 */
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "未授予存储权限，无法读取 Download 中的模型文件", Toast.LENGTH_LONG).show()
        }
    }

    /** Android 11+：MANAGE_EXTERNAL_STORAGE 结果处理器。 */
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 从设置返回后检查权限是否已授予
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "未授予文件管理权限，请前往设置手动开启", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 申请录音权限
        requestRecordPermission()

        // 申请存储权限（读取模型文件需要）
        requestStoragePermission()

        setContent {
            val settingsRepository = AppModule.getSettingsRepository()
            val darkMode by settingsRepository.darkModeFlow.collectAsStateWithLifecycle(
                initialValue = SettingsRepository.DARK_MODE_SYSTEM
            )
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

    private fun requestRecordPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+：需要 MANAGE_EXTERNAL_STORAGE 才能访问 Download 目录
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = android.net.Uri.parse("package:${packageName}")
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    // 备用：直接打开设置页
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(fallbackIntent)
                }
            }
        } else {
            // Android 10 及以下：传统 READ_EXTERNAL_STORAGE 权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}
