package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OrderSearchScreen
import com.example.ui.screens.PackingCameraScreen
import com.example.ui.screens.PackingHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UploadQueueScreen
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CameraViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.UploadQueueViewModel

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CAMERA("Đóng Hàng", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    QUEUE("Hàng Đợi", Icons.Filled.CloudUpload, Icons.Outlined.CloudUpload),
    HISTORY("Lịch Sử", Icons.Filled.History, Icons.Outlined.History),
    SEARCH("Tra Cứu", Icons.Filled.Search, Icons.Outlined.Search),
    SETTINGS("Cài Đặt", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun MainAppScreen() {
    val cameraViewModel: CameraViewModel = viewModel()
    val uploadQueueViewModel: UploadQueueViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    var selectedTab by remember { mutableStateOf(MainTab.CAMERA) }
    val queueItems by uploadQueueViewModel.queueItems.collectAsState()
    val pendingUploadCount = queueItems.size

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == MainTab.QUEUE && pendingUploadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = AccentAmber,
                                            contentColor = Color.Black
                                        ) {
                                            Text("$pendingUploadCount", fontSize = 10.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            unselectedIconColor = Color(0xFF94A3B8),
                            selectedTextColor = PrimaryBlueLight,
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = PrimaryBlueLight
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (selectedTab) {
                MainTab.CAMERA -> {
                    PackingCameraScreen(
                        viewModel = cameraViewModel,
                        onNavigateToQueue = { selectedTab = MainTab.QUEUE }
                    )
                }
                MainTab.QUEUE -> {
                    UploadQueueScreen(
                        viewModel = uploadQueueViewModel
                    )
                }
                MainTab.HISTORY -> {
                    PackingHistoryScreen(
                        viewModel = historyViewModel
                    )
                }
                MainTab.SEARCH -> {
                    OrderSearchScreen(
                        viewModel = historyViewModel
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = authViewModel
                    )
                }
            }
        }
    }
}
