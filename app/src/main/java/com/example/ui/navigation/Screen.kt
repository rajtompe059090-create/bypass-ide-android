package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Files : Screen("files")
    object Editor : Screen("editor")
    object Terminal : Screen("terminal")
    object Preview : Screen("preview")
    object Dev : Screen("dev")
    object Settings : Screen("settings")
    object Builder : Screen("builder")
}
