package com.yangyx.adbhelper

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import com.yangyx.adbhelper.ui.components.BottomBarSettingsSheet
import com.yangyx.adbhelper.ui.components.LiquidGlassBottomBar
import com.yangyx.adbhelper.ui.components.SessionManagementSheet
import com.yangyx.adbhelper.ui.components.WidgetPinningDialog
import com.yangyx.adbhelper.ui.models.BottomBarMode
import com.yangyx.adbhelper.ui.models.NavItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.yangyx.adbhelper.widget.WidgetActionReceiver
import com.yangyx.adbhelper.widget.WidgetUpdateHelper
import android.content.Intent

class MainActivity : ComponentActivity() {

    private val viewModel: AdbViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AdbViewModel.getOrCreate(application) as T
            }
        }
    }
    private var pendingIntentAction by mutableStateOf<Intent?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntentAction = intent

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val connectionState by viewModel.connectionState.collectAsState()
                val actionMsg by viewModel.actionMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                val sessions by viewModel.sessions.collectAsState()
                val currentSessionId by viewModel.currentSessionId.collectAsState()
                val backgroundTimeoutMinutes by viewModel.backgroundMirroringTimeoutMinutes.collectAsState()
                var isViewingHomeScreen by remember { mutableStateOf(false) }
                var showSessionManagementSheet by remember { mutableStateOf(false) }

                LaunchedEffect(isViewingHomeScreen) {
                    viewModel.setHomeScreenActive(isViewingHomeScreen)
                }

                val isConnected = connectionState is ConnectionState.Connected

                val bottomBarMode by viewModel.bottomBarMode.collectAsState()
                val liquidGlassConfig by viewModel.liquidGlassConfig.collectAsState()
                var showBottomBarSettingsSheet by remember { mutableStateOf(false) }
                var showWidgetPinningDialog by remember { mutableStateOf(false) }

                var selectedNavIndex by remember { mutableIntStateOf(0) }

                LaunchedEffect(pendingIntentAction) {
                    val currentIntent = pendingIntentAction ?: return@LaunchedEffect
                    val action = currentIntent.action ?: return@LaunchedEffect
                    when (action) {
                        WidgetActionReceiver.ACTION_WIDGET_QUICK_CONNECT -> {
                            isViewingHomeScreen = false
                            viewModel.connectLatestDevice()
                        }
                        WidgetActionReceiver.ACTION_WIDGET_CONNECT_DEVICE -> {
                            val ip = currentIntent.getStringExtra(WidgetActionReceiver.EXTRA_DEVICE_IP) ?: ""
                            val port = currentIntent.getIntExtra(WidgetActionReceiver.EXTRA_DEVICE_PORT, 5555)
                            val name = currentIntent.getStringExtra(WidgetActionReceiver.EXTRA_DEVICE_NAME) ?: ""
                            val serial = currentIntent.getStringExtra(WidgetActionReceiver.EXTRA_DEVICE_SERIAL) ?: ""
                            isViewingHomeScreen = false
                            viewModel.connectDeviceSmart(
                                serialNo = serial,
                                deviceName = name,
                                fallbackIp = ip,
                                fallbackPort = port
                            )
                        }
                        WidgetUpdateHelper.ACTION_WIDGET_MIRROR -> {
                            isViewingHomeScreen = false
                            selectedNavIndex = 0
                            viewModel.currentSession?.selectedNavIndex = 0
                            viewModel.triggerMirrorFromWidget()
                        }
                        WidgetUpdateHelper.ACTION_WIDGET_FILE -> {
                            isViewingHomeScreen = false
                            selectedNavIndex = 1
                            viewModel.currentSession?.selectedNavIndex = 1
                        }
                        WidgetUpdateHelper.ACTION_WIDGET_TERMINAL -> {
                            isViewingHomeScreen = false
                            selectedNavIndex = 2
                            viewModel.currentSession?.selectedNavIndex = 2
                        }
                        WidgetUpdateHelper.ACTION_WIDGET_OPEN_DEVICE -> {
                            isViewingHomeScreen = false
                        }
                    }
                    pendingIntentAction = null
                }

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
                        isViewingHomeScreen = false
                    }
                }

                LaunchedEffect(connectionState) {
                    if (connectionState is ConnectionState.Connected) {
                        isViewingHomeScreen = false
                    }
                }

                LaunchedEffect(currentSessionId) {
                    viewModel.currentSession?.let { sess ->
                        selectedNavIndex = sess.selectedNavIndex
                    }
                }

                if (isViewingHomeScreen && isConnected) {
                    BackHandler {
                        isViewingHomeScreen = false
                    }
                }

                if (!isConnected || isViewingHomeScreen) {
                    // First Interface: Connection & Pairing / Home Screen
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
                                    if (sessions.isNotEmpty()) {
                                        FilledTonalButton(
                                            onClick = { isViewingHomeScreen = false },
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "返回控制 (${sessions.size})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        IconButton(onClick = { showSessionManagementSheet = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Devices,
                                                contentDescription = "会话管理",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showBottomBarSettingsSheet = true }) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "底栏风格与外观设置",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { showWidgetPinningDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Widgets,
                                            contentDescription = "桌面小部件",
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
                                    isViewingHomeScreen = false
                                },
                                onOpenSessionManager = {
                                    showSessionManagementSheet = true
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
                                        Surface(
                                            onClick = { showSessionManagementSheet = true },
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF4CAF50))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "已连接: ${connectedState.deviceName} ($displayIp)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (sessions.size > 1) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Text(
                                                            text = "${sessions.size}会话",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "会话管理",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
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
                                                    onClick = {
                                                        selectedNavIndex = index
                                                        viewModel.currentSession?.selectedNavIndex = index
                                                    },
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
                                            onItemSelected = {
                                                selectedNavIndex = it
                                                viewModel.currentSession?.selectedNavIndex = it
                                            },
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
                            val sessions by viewModel.sessions.collectAsState()
                            val currentSessionId by viewModel.currentSessionId.collectAsState()
                            val fallbackController = viewModel.scrcpyController
                            val installedApps by viewModel.installedApps.collectAsState()

                            if (sessions.isNotEmpty()) {
                                sessions.forEach { s ->
                                    val isCurrent = s.id == currentSessionId
                                    val sScreenState by s.scrcpyController.screenState.collectAsState()
                                    val isSessionStreaming = sScreenState is ScreenState.Streaming
                                    val scrcpyBottomPadding = if (isGlassMode && !isSessionStreaming) 0.dp else innerPadding.calculateBottomPadding()

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
                                                alpha = if (isCurrent && selectedNavIndex == 0) 1f else 0f
                                                translationX = if (isCurrent && selectedNavIndex == 0) 0f else 99999f
                                            }
                                    ) {
                                        key(s.id) {
                                            ScrcpyView(
                                                controller = s.scrcpyController,
                                                installedApps = if (isCurrent) installedApps else emptyList(),
                                                onRefreshApps = { viewModel.refreshApps() }
                                            )
                                        }
                                    }
                                }
                            } else if (fallbackController != null) {
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
                                    key(fallbackController) {
                                        ScrcpyView(
                                            controller = fallbackController,
                                            installedApps = installedApps,
                                            onRefreshApps = { viewModel.refreshApps() }
                                        )
                                    }
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
                                val remainingSessions = sessions.filter { it.id != currentSessionId }
                                AlertDialog(
                                    onDismissRequest = { showDisconnectConfirmDialog = false },
                                    title = { Text("断开当前连接") },
                                    text = {
                                        val remainMsg = if (remainingSessions.isNotEmpty()) {
                                            "\n\n断开后将自动切换至剩余会话列表中最上面一台设备【${remainingSessions.first().deviceName}】。"
                                        } else {
                                            "\n\n断开后将返回连接首页。"
                                        }
                                        Text(
                                            text = "确定要断开与当前设备【${connectedState.deviceName} (${connectedState.ip})】的连接吗？$remainMsg",
                                            fontSize = 14.sp
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showDisconnectConfirmDialog = false
                                                viewModel.disconnectCurrentSession()
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

                if (showSessionManagementSheet) {
                    SessionManagementSheet(
                        sessions = sessions,
                        currentSessionId = currentSessionId,
                        backgroundTimeoutMinutes = backgroundTimeoutMinutes,
                        onBackgroundTimeoutMinutesChange = { viewModel.setBackgroundMirroringTimeoutMinutes(it) },
                        onSelectSession = { session ->
                            viewModel.switchSession(session.id, selectedNavIndex)
                            selectedNavIndex = session.selectedNavIndex
                            isViewingHomeScreen = false
                            showSessionManagementSheet = false
                        },
                        onTerminateSession = { session ->
                            viewModel.terminateSession(session.id)
                            if (sessions.isEmpty()) {
                                showSessionManagementSheet = false
                                isViewingHomeScreen = false
                            }
                        },
                        onAddNewSession = {
                            showSessionManagementSheet = false
                            isViewingHomeScreen = true
                        },
                        onDismissRequest = { showSessionManagementSheet = false }
                    )
                }

                if (showWidgetPinningDialog) {
                    WidgetPinningDialog(
                        onDismissRequest = { showWidgetPinningDialog = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentAction = intent
    }
}

