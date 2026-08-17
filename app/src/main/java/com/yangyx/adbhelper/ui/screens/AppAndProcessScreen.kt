package com.yangyx.adbhelper.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.models.LocalAppItem
import com.yangyx.adbhelper.ui.models.RemoteAppItem
import com.yangyx.adbhelper.ui.models.RemoteProcessItem

private sealed class ConfirmAction {
    data class ForceStop(val app: RemoteAppItem) : ConfirmAction()
    data class ClearData(val app: RemoteAppItem) : ConfirmAction()
    data class Uninstall(val app: RemoteAppItem) : ConfirmAction()
    data class KillProcess(val proc: RemoteProcessItem) : ConfirmAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppAndProcessScreen(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("应用管理 (APK/APKS)", "进程管理 (Processes)")

    val apps by viewModel.installedApps.collectAsState()
    val processes by viewModel.runningProcesses.collectAsState()
    val isAppLoading by viewModel.isAppLoading.collectAsState()
    val appLoadingStatus by viewModel.appLoadingStatus.collectAsState()
    val appLoadingProgress by viewModel.appLoadingProgress.collectAsState()

    val localApps by viewModel.localInstalledApps.collectAsState()
    val isLocalAppsLoading by viewModel.isLocalAppsLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    var pendingConfirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
    var showDiagnosisDialog by remember { mutableStateOf(false) }
    var showInstallChoiceSheet by remember { mutableStateOf(false) }
    var showLocalAppsDialog by remember { mutableStateOf(false) }

    // Deduplicated initial loading
    LaunchedEffect(Unit) {
        if (apps.isEmpty() && !isAppLoading) {
            viewModel.refreshApps(force = false)
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && processes.isEmpty()) {
            viewModel.refreshProcesses(force = false)
        }
    }

    // APK / APKS Installer Launcher
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "install.apk"
            viewModel.installRemoteApk(context, uri, fileName)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索应用或进程包名/PID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showDiagnosisDialog = true }) {
                    Icon(Icons.Default.BugReport, contentDescription = "分析日志")
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = {
                    if (selectedTabIndex == 0) viewModel.refreshApps(force = true) else viewModel.refreshProcesses(force = true)
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTabIndex == 0) {
                // Apps Tab
                val filteredApps = remember(apps, searchQuery, showSystemApps) {
                    apps.filter { app ->
                        (showSystemApps || !app.isSystemApp) &&
                                (searchQuery.isBlank() ||
                                        app.packageName.contains(searchQuery, ignoreCase = true) ||
                                        app.appName.contains(searchQuery, ignoreCase = true))
                    }
                }

                if (isAppLoading) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = appLoadingStatus.ifEmpty { "正在加载应用解析中..." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${(appLoadingProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { appLoadingProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "共 ${filteredApps.size} 个应用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = showSystemApps,
                        onClick = { showSystemApps = !showSystemApps },
                        label = { Text("显示系统应用") },
                        leadingIcon = if (showSystemApps) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppItemCard(
                            app = app,
                            onLaunch = { viewModel.launchApp(app.packageName) },
                            onForceStop = { pendingConfirmAction = ConfirmAction.ForceStop(app) },
                            onUninstall = { pendingConfirmAction = ConfirmAction.Uninstall(app) },
                            onClearData = { pendingConfirmAction = ConfirmAction.ClearData(app) }
                        )
                    }
                }
            } else {
                // Processes Tab
                val filteredProcesses = remember(processes, searchQuery) {
                    if (searchQuery.isBlank()) processes else processes.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.pid.toString().contains(searchQuery)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProcesses, key = { it.pid }) { proc ->
                        ProcessItemCard(
                            proc = proc,
                            onKill = { pendingConfirmAction = ConfirmAction.KillProcess(proc) }
                        )
                    }
                }
            }
        }

        // Install APK / APKS Floating Button
        if (selectedTabIndex == 0) {
            FloatingActionButton(
                onClick = { showInstallChoiceSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Install APK")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("安装应用", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Install Choice Sheet (Choose between Local file or Host App)
        if (showInstallChoiceSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showInstallChoiceSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "安装应用到对端设备",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        onClick = {
                            showInstallChoiceSheet = false
                            apkPickerLauncher.launch("*/*")
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.FileOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "从本地文件选择安装包",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "选择设备存储中的 .apk 或 .apks 文件进行安装",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        onClick = {
                            showInstallChoiceSheet = false
                            showLocalAppsDialog = true
                            viewModel.loadLocalApps(context, force = false)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "从本机提取已安装应用",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "推荐",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "直接选择控制端本手机上的应用，一键提取并安装到被控端",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Local Apps Selection Dialog
        if (showLocalAppsDialog) {
            LocalAppsInstallDialog(
                localApps = localApps,
                isLoading = isLocalAppsLoading,
                onRefresh = { viewModel.loadLocalApps(context, force = true) },
                onSelectApp = { app ->
                    showLocalAppsDialog = false
                    viewModel.installLocalAppToRemote(app)
                },
                onDismiss = { showLocalAppsDialog = false }
            )
        }

        // Confirmation Dialog
        pendingConfirmAction?.let { action ->
            val title: String
            val text: String
            val confirmLabel: String
            val onConfirm: () -> Unit

            when (action) {
                is ConfirmAction.ForceStop -> {
                    title = "确认强行停止应用"
                    text = "确定要强制停止应用「${action.app.appName}」(${action.app.packageName}) 吗？"
                    confirmLabel = "强行停止"
                    onConfirm = { viewModel.forceStopApp(action.app.packageName) }
                }
                is ConfirmAction.ClearData -> {
                    title = "确认清除应用数据"
                    text = "确定要清除应用「${action.app.appName}」的所有数据与缓存吗？此操作无法撤销！"
                    confirmLabel = "清除数据"
                    onConfirm = { viewModel.clearAppData(action.app.packageName) }
                }
                is ConfirmAction.Uninstall -> {
                    title = "确认卸载应用"
                    text = "确定要从被控端设备卸载应用「${action.app.appName}」(${action.app.packageName}) 吗？"
                    confirmLabel = "卸载"
                    onConfirm = { viewModel.uninstallApp(action.app.packageName) }
                }
                is ConfirmAction.KillProcess -> {
                    title = "确认结束进程"
                    text = "确定要结束进程「${action.proc.name}」(PID: ${action.proc.pid}) 吗？"
                    confirmLabel = "结束进程"
                    onConfirm = { viewModel.killProcess(action.proc.pid) }
                }
            }

            AlertDialog(
                onDismissRequest = { pendingConfirmAction = null },
                title = { Text(title) },
                text = { Text(text) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onConfirm()
                            pendingConfirmAction = null
                        }
                    ) {
                        Text(confirmLabel, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingConfirmAction = null }) {
                        Text("取消")
                    }
                }
            )
        }

        // Diagnosis Logs Dialog
        if (showDiagnosisDialog) {
            val diagnosisLogs by viewModel.appDiagnosisLogs.collectAsState()
            val clipboardManager = LocalClipboardManager.current

            AlertDialog(
                onDismissRequest = { showDiagnosisDialog = false },
                title = { Text("应用解析与执行诊断日志", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Text(
                                text = diagnosisLogs,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(scrollState)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString(diagnosisLogs))
                    }) {
                        Text("复制日志")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiagnosisDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}

@Composable
fun LocalAppsInstallDialog(
    localApps: List<LocalAppItem>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSelectApp: (LocalAppItem) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableIntStateOf(0) } // 0: 全部, 1: 单 APK (推荐), 2: 组合包 (Split)
    var showSystemApps by remember { mutableStateOf(false) }
    var pendingSplitConfirmApp by remember { mutableStateOf<LocalAppItem?>(null) }

    val filteredList = remember(localApps, searchQuery, filterMode, showSystemApps) {
        localApps.filter { app ->
            (showSystemApps || !app.isSystemApp) &&
                    (when (filterMode) {
                        1 -> app.isSingleApk
                        2 -> !app.isSingleApk
                        else -> true
                    }) &&
                    (searchQuery.isBlank() ||
                            app.appName.contains(searchQuery, ignoreCase = true) ||
                            app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "选择本机已安装应用",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "提取控制端 APK 安装到被控端设备",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新本机应用")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索本机应用名称或包名...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "清空")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = filterMode == 0,
                        onClick = { filterMode = 0 },
                        label = { Text("全部 (${localApps.size})") }
                    )
                    val singleCount = localApps.count { it.isSingleApk }
                    FilterChip(
                        selected = filterMode == 1,
                        onClick = { filterMode = 1 },
                        label = { Text("单 APK ($singleCount)") },
                        leadingIcon = if (filterMode == 1) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    val splitCount = localApps.count { !it.isSingleApk }
                    FilterChip(
                        selected = filterMode == 2,
                        onClick = { filterMode = 2 },
                        label = { Text("组合包 ($splitCount)") },
                        leadingIcon = if (filterMode == 2) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("正在检索并解析本机应用列表...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到符合条件的应用",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.packageName }) { app ->
                            LocalAppItemCard(
                                app = app,
                                onInstall = {
                                    if (app.isSingleApk) {
                                        onSelectApp(app)
                                    } else {
                                        pendingSplitConfirmApp = app
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Split APK confirmation dialog
    pendingSplitConfirmApp?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingSplitConfirmApp = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("组合包 (Split APKs) 安装说明") },
            text = {
                Column {
                    Text(
                        text = "「${app.appName}」在控制端包含 ${app.splitSourceDirs.size + 1} 个分片 APK (App Bundle)。",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "本工具将同时提取并向被控端推送所有分片进行组合安装。\n\n⚠️ 注意：分片安装包通常绑定了当前手机的 CPU 架构 (ABI) 及屏幕参数。若被控端硬件规格与控制端不同，安装可能会被对端系统拦截。建议优先使用『单 APK』或完整通用安装包。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = pendingSplitConfirmApp
                    pendingSplitConfirmApp = null
                    if (target != null) {
                        onSelectApp(target)
                    }
                }) {
                    Text("继续提取安装")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSplitConfirmApp = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun LocalAppItemCard(
    app: LocalAppItem,
    onInstall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            val imageBitmap = remember(app.packageName) {
                app.icon?.let { drawable ->
                    try {
                        if (drawable is BitmapDrawable && drawable.bitmap != null) {
                            drawable.bitmap.asImageBitmap()
                        } else {
                            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val c = Canvas(b)
                            drawable.setBounds(0, 0, c.width, c.height)
                            drawable.draw(c)
                            b.asImageBitmap()
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Android, contentDescription = app.appName, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (app.isSingleApk) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "单 APK",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "组合包 (${app.splitSourceDirs.size + 1}分片)",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val sizeMb = String.format("%.1f MB", app.totalSizeBytes / (1024.0 * 1024.0))
                val verStr = if (app.versionName.isNotEmpty()) "v${app.versionName}" else ""
                Text(
                    text = "${app.packageName} • $verStr $sizeMb",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onInstall,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("提取安装", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AppItemCard(
    app: RemoteAppItem,
    onLaunch: () -> Unit,
    onForceStop: () -> Unit,
    onUninstall: () -> Unit,
    onClearData: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Android, contentDescription = "App", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = app.appName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = app.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onLaunch) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Launch", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("启动")
                }
                TextButton(onClick = onForceStop) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("强停")
                }
                TextButton(onClick = onClearData) {
                    Icon(Icons.Default.CleanHands, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清除")
                }
                TextButton(onClick = onUninstall) {
                    Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("卸载", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ProcessItemCard(
    proc: RemoteProcessItem,
    onKill: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = proc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "PID: ${proc.pid} | User: ${proc.user} | CPU: ${proc.cpuUsage}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            TextButton(onClick = onKill) {
                Icon(Icons.Default.Delete, contentDescription = "Kill Process", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("结束进程", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
