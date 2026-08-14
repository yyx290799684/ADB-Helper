package com.yangyx.adbhelper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.ui.AdbViewModel
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

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    var pendingConfirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
    var showDiagnosisDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (apps.isEmpty()) {
            viewModel.refreshApps()
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
                    if (selectedTabIndex == 0) viewModel.refreshApps() else viewModel.refreshProcesses()
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
                                progress = appLoadingProgress,
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
                    items(filteredApps) { app ->
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
                    items(filteredProcesses) { proc ->
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
                onClick = { apkPickerLauncher.launch("*/*") },
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
                    Text("安装 APK/APKS", fontWeight = FontWeight.Bold)
                }
            }
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
