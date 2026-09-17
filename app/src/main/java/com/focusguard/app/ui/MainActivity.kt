package com.focusguard.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.focusguard.app.ui.screens.AppSelectionScreen
import com.focusguard.app.ui.screens.PermissionsScreen
import com.focusguard.app.ui.screens.TimerScreen
import com.focusguard.app.ui.theme.DarkBg
import com.focusguard.app.ui.theme.DarkSurface
import com.focusguard.app.ui.theme.EmeraldPrimary
import com.focusguard.app.ui.theme.FocusGuardTheme
import com.focusguard.app.ui.theme.TextMuted
import com.focusguard.app.ui.theme.TextPrimary

sealed class Screen(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Timer : Screen("타이머", Icons.Filled.HourglassEmpty, Icons.Outlined.HourglassEmpty)
    object Apps : Screen("차단 앱", Icons.Filled.Apps, Icons.Outlined.Apps)
    object Settings : Screen("권한/설정", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            FocusGuardTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val items = listOf(Screen.Timer, Screen.Apps, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = TextPrimary
            ) {
                items.forEachIndexed { index, screen ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TimerScreen(
                    onNavigateToPermissions = { selectedTab = 2 },
                    onNavigateToAppSelection = { selectedTab = 1 }
                )
                1 -> AppSelectionScreen()
                2 -> PermissionsScreen()
            }
        }
    }
}
