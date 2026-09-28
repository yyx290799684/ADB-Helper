package com.yangyx.adbhelper

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.unit.LayoutDirection
import com.yangyx.adbhelper.ui.components.BottomBarSettingsSheet
import com.yangyx.adbhelper.ui.components.LiquidGlassBottomBar
import com.yangyx.adbhelper.ui.models.BottomBarMode
import com.yangyx.adbhelper.ui.models.NavItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import android.app.Activity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.DisposableEffect
import com.yangyx.adbhelper.scrcpy.ScreenState
import com.yangyx.adbhelper.scrcpy.ScrcpyView
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.ConnectionState
import com.yangyx.adbhelper.ui.screens.AppAndProcessScreen
import com.yangyx.adbhelper.ui.screens.ConnectScreen
import com.yangyx.adbhelper.ui.screens.DeviceScreen
import com.yangyx.adbhelper.ui.screens.DeviceInfoScreen
import com.yangyx.adbhelper.ui.screens.FileExplorerScreen
import com.yangyx.adbhelper.ui.screens.TerminalScreen
import com.yangyx.adbhelper.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AdbViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val connectionState by viewModel.connectionState.collectAsState()
                val actionMsg by viewModel.actionMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                val isConnected = connectionState is ConnectionState.Connected

                val bottomBarMode by viewModel.bottomBarMode.collectAsState()
                val liquidGlassConfig by viewModel.liquidGlassConfig.collectAsState()
                var showBottomBarSettingsSheet by remember { mutableStateOf(false) }

                var selectedNavIndex by remember { mutableIntStateOf(0) }

                val navItems = listOf(
                    NavItem("屏幕", Icons.Default.AspectRatio),
                    NavItem("文件", Icons.Default.Folder),
                    NavItem("终端", Icons.Default.Terminal),
                    NavItem("设备", Icons.Default.PhoneAndroid),
                    NavItem("应用/进程", Icons.Default.Apps)
                )

                LaunchedEffect(actionMsg) {
                    actionMsg?.let { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearActionMessage()
                    }
                }

                LaunchedEffect(isConnected) {
                    if (!isConnected) {
                        selectedNavIndex = 0
                    }
                }

                if (!isConnected) {
                    // First Interface: Connection & Pairing
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "ADB 远程助手",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                actions = {
                                    IconButton(onClick = { showBottomBarSettingsSheet = true }) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "底栏风格与外观设置",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        ) {
                            ConnectScreen(
                                viewModel = viewModel,
                                onNavigateToRemote = {
                                    // Handled automatically via connection state transition
                                }
                            )
                        }
                    }
                } else {
                    // Second Interface: Remote Control Dashboard (Screen, Files, Terminal, Info, Apps)
                    val connectedState = connectionState as ConnectionState.Connected
                    val scrcpyController = viewModel.scrcpyController
                    val isScrcpyFullscreen by scrcpyController?.isFullscreen?.collectAsState() ?: remember { mutableStateOf(false) }
                    val scrcpyState by scrcpyController?.screenState?.collectAsState() ?: remember { mutableStateOf<ScreenState>(ScreenState.Idle) }
                    val showFullscreen = isScrcpyFullscreen && selectedNavIndex == 0 && scrcpyState is ScreenState.Streaming
                    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }

                    val activity = LocalContext.current as? Activity
                    DisposableEffect(showFullscreen) {
                        if (activity != null) {
                            val window = activity.window
                            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                            if (showFullscreen) {
                                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            } else {
                                insetsController.show(WindowInsetsCompat.Type.systemBars())
                            }
                        }
                        onDispose {
                            if (activity != null) {
                                val window = activity.window
                                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                                insetsController.show(WindowInsetsCompat.Type.systemBars())
                            }
                        }
                    }

                    Scaffold(
                        topBar = {
                            if (!showFullscreen) {
                                TopAppBar(
                                    title = {
                                        val displayIp = if (connectedState.ip.contains(":")) "[${connectedState.ip}]" else connectedState.ip
                                        Text(
                                            text = "已连接: ${connectedState.deviceName} ($displayIp)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = { showBottomBarSettingsSheet = true }) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "底栏风格与外观设置",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { showDisconnectConfirmDialog = true }) {
                                            Icon(
                                                Icons.Default.LinkOff,
                                                contentDescription = "断开连接",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (!showFullscreen) {
                                when (bottomBarMode) {
                                    BottomBarMode.CLASSIC_M3 -> {
                                        NavigationBar {
                                            navItems.forEachIndexed { index, item ->
                                                NavigationBarItem(
                                                    selected = selectedNavIndex == index,
                                                    onClick = { selectedNavIndex = index },
                                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                                    label = {
                                                        Text(
                                                            text = item.title,
                                                            maxLines = 1,
                                                            fontSize = 11.sp,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    alwaysShowLabel = true
                                                )
                                            }
                                        }
                                    }
                                    BottomBarMode.LIQUID_GLASS -> {
                                        val isStreaming = selectedNavIndex == 0 && scrcpyState is ScreenState.Streaming
                                        LiquidGlassBottomBar(
                                            navItems = navItems,
                                            selectedIndex = selectedNavIndex,
                                            onItemSelected = { selectedNavIndex = it },
                                            config = liquidGlassConfig,
                                            isDocked = isStreaming
                                        )
                                    }
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        val isGlassMode = bottomBarMode == BottomBarMode.LIQUID_GLASS
                        val isStreaming = selectedNavIndex == 0 && scrcpyState is ScreenState.Streaming

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .consumeWindowInsets(innerPadding)
                        ) {
                            val controller = viewModel.scrcpyController
                            if (controller != null) {
                                val installedApps by viewModel.installedApps.collectAsState()
                                // When streaming (投屏进去以后), scrcpy video must NOT overlap with the navigation bar, so bottom = innerPadding.calculateBottomPadding().
                                // When on the Idle/settings control page, scrcpy fills down to 0.dp in Glass Mode so the glass bar floats over the scrollable page.
                                val scrcpyBottomPadding = if (isGlassMode && !isStreaming) 0.dp else innerPadding.calculateBottomPadding()
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = innerPadding.calculateTopPadding(),
                                            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                                            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                                            bottom = scrcpyBottomPadding
                                        )
                                        .graphicsLayer {
                                            alpha = if (selectedNavIndex == 0) 1f else 0f
                                            translationX = if (selectedNavIndex == 0) 0f else 99999f
                                        }
                                ) {
                                    ScrcpyView(
                                        controller = controller,
                                        installedApps = installedApps,
                                        onRefreshApps = { viewModel.refreshApps() }
                                    )
                                }
                            }
                            // In Glass Mode, content screens with scrolling lists (File Explorer, Device Info, Apps)
                            // fill the screen to the bottom (bottom = 0.dp) so that the glass bottom bar floats over the list.
                            // Their internal LazyColumns provide 100.dp contentPadding at the bottom so that the last item
                            // scrolls cleanly above the floating bar with full safety distance.
                            // Terminal has a pinned bottom input field, so it dynamically tracks the navigation bar height with compact spacing.
                            val listScreenBottomPadding = if (isGlassMode) 0.dp else innerPadding.calculateBottomPadding()
                            val terminalBottomPadding = if (isGlassMode) (innerPadding.calculateBottomPadding() - 6.dp).coerceAtLeast(0.dp) else innerPadding.calculateBottomPadding()

                            val listScreenModifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                                    bottom = listScreenBottomPadding
                                )

                            val terminalScreenModifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                                    bottom = terminalBottomPadding
                                )

                            when (selectedNavIndex) {
                                1 -> FileExplorerScreen(viewModel = viewModel, modifier = listScreenModifier, isGlassMode = isGlassMode)
                                2 -> TerminalScreen(viewModel = viewModel, modifier = terminalScreenModifier)
                                3 -> DeviceScreen(viewModel = viewModel, modifier = listScreenModifier, isGlassMode = isGlassMode)
                                4 -> AppAndProcessScreen(viewModel = viewModel, modifier = listScreenModifier, isGlassMode = isGlassMode)
                            }

                            // Global Floating Overlay for APK push and install progress
                            com.yangyx.adbhelper.ui.components.ApkInstallOverlay(
                                viewModel = viewModel
                            )

                            // Global Floating Overlay for Remote File Download progress
                            com.yangyx.adbhelper.ui.components.FileDownloadOverlay(
                                viewModel = viewModel
                            )

                            if (showDisconnectConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDisconnectConfirmDialog = false },
                                    title = { Text("断开 ADB 连接") },
                                    text = {
                                        Text(
                                            text = "确定要断开与设备【${connectedState.deviceName} (${connectedState.ip})】的 ADB 连接吗？断开后当前的投屏与操作会话将立即终止。",
                                            fontSize = 14.sp
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showDisconnectConfirmDialog = false
                                                viewModel.disconnect()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("确认断开")
                                        }
                                    },
                                    dismissButton = {
                                        OutlinedButton(onClick = { showDisconnectConfirmDialog = false }) {
                                            Text("取消")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (showBottomBarSettingsSheet) {
                    BottomBarSettingsSheet(
                        currentMode = bottomBarMode,
                        onModeChange = { viewModel.setBottomBarMode(it) },
                        onDismissRequest = { showBottomBarSettingsSheet = false }
                    )
                }
            }
        }
    }
}
