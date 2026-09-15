package com.example.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.navigation.Screen
import com.example.ui.theme.BgColor
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MutedTextColor
import com.example.ui.theme.SurfaceVariantColor
import com.example.ui.theme.PrimaryTextColor

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
    BottomNavItem("Files", Screen.Files.route, Icons.Default.Folder),
    BottomNavItem("Editor", Screen.Editor.route, Icons.Default.Code),
    BottomNavItem("Terminal", Screen.Terminal.route, Icons.Default.Terminal),
    BottomNavItem("Preview", Screen.Preview.route, Icons.Default.PlayArrow),
    BottomNavItem("Dev", Screen.Dev.route, Icons.Default.Person)
)

@Composable
fun BypassBottomNav(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit
) {
    NavigationBar(
        containerColor = BgColor,
        contentColor = PrimaryTextColor
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToRoute(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(text = item.title)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyanAccent,
                    unselectedIconColor = MutedTextColor,
                    selectedTextColor = CyanAccent,
                    unselectedTextColor = MutedTextColor,
                    indicatorColor = SurfaceVariantColor
                )
            )
        }
    }
}
