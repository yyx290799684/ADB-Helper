package com.yangyx.adbhelper

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.yangyx.adbhelper.ui.screens.DeviceInfoScreen
import com.yangyx.adbhelper.ui.screens.FileExplorerScreen
import com.yangyx.adbhelper.ui.screens.TerminalScreen
import com.yangyx.adbhelper.ui.theme.MyApplicationTheme

data class NavItem(
    val title: String,
    val icon: ImageVector
)

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

                var selectedNavIndex by remember { mutableIntStateOf(0) }

                val navItems = listOf(
                    NavItem("屏幕", Icons.Default.AspectRatio),
                    NavItem("文件", Icons.Default.Folder),
                    NavItem("终端", Icons.Default.Terminal),
                    NavItem("信息", Icons.Default.Analytics),
                    NavItem("应用/进程", Icons.Default.Apps)
                )

                LaunchedEffect(actionMsg) {
                    actionMsg?.let { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearActionMessage()
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
                                        Text(
                                            text = "已连接: ${connectedState.deviceName} (${connectedState.ip})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = { viewModel.disconnect() }) {
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
                                NavigationBar {
                                    navItems.forEachIndexed { index, item ->
                                        NavigationBarItem(
                                            selected = selectedNavIndex == index,
                                            onClick = { selectedNavIndex = index },
                                            icon = { Icon(item.icon, contentDescription = item.title) },
                                            label = { Text(item.title) }
                                        )
                                    }
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            val controller = viewModel.scrcpyController
                            if (controller != null) {
                                val installedApps by viewModel.installedApps.collectAsState()
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
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
                            when (selectedNavIndex) {
                                1 -> FileExplorerScreen(viewModel = viewModel)
                                2 -> TerminalScreen(viewModel = viewModel)
                                3 -> DeviceInfoScreen(viewModel = viewModel)
                                4 -> AppAndProcessScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
