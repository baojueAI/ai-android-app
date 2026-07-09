package com.aichat.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * 应用导航图。
 *
 * - `chat`：聊天主界面 [ChatScreen]（起始页）
 * - `settings`：设置界面 [SettingsScreen]
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "chat",
        modifier = modifier
    ) {
        composable(route = "chat") {
            ChatScreen(onSettingsClick = { navController.navigate("settings") })
        }
        composable(route = "settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
