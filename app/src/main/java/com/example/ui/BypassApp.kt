package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.BypassBottomNav
import com.example.ui.components.BypassTopBar
import com.example.ui.components.RootStatusBanner
import com.example.ui.navigation.Screen
import com.example.ui.screens.*

@Composable
fun BypassApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showRootBanner by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                BypassTopBar(
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onRunClick = { /* Handle run */ }
                )
                if (showRootBanner) {
                    RootStatusBanner(
                        onGrantRoot = { /* Handle grant root */ },
                        onDismiss = { showRootBanner = false }
                    )
                }
            }
        },
        bottomBar = {
            if (currentRoute != Screen.Settings.route) {
                BypassBottomNav(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(onNavigateToBuilder = { navController.navigate(Screen.Builder.route) }) }
            composable(Screen.Files.route) { FilesScreen(onNavigateToEditor = { navController.navigate(Screen.Editor.route) }) }
            composable(Screen.Editor.route) { EditorScreen() }
            composable(Screen.Terminal.route) { TerminalScreen() }
            composable(Screen.Preview.route) { PreviewScreen() }
            composable(Screen.Dev.route) { DevScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Builder.route) { BuilderScreen() }
        }
    }
}
