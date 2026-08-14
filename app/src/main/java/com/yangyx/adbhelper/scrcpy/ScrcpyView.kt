package com.yangyx.adbhelper.scrcpy

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecentActors
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import com.yangyx.adbhelper.ui.models.RemoteAppItem
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.FloatingActionButton
import java.util.Locale
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private fun mapLocalTouchToRemote(
    touchX: Float,
    touchY: Float,
    videoWidth: Float,
    videoHeight: Float,
    remoteWidth: Int,
    remoteHeight: Int
): Pair<Int, Int>? {
    if (videoWidth <= 0f || videoHeight <= 0f || remoteWidth <= 0 || remoteHeight <= 0) return null

    val clampedX = touchX.coerceIn(0f, videoWidth)
    val clampedY = touchY.coerceIn(0f, videoHeight)

    val remoteX = ((clampedX / videoWidth) * remoteWidth).toInt().coerceIn(0, remoteWidth - 1)
    val remoteY = ((clampedY / videoHeight) * remoteHeight).toInt().coerceIn(0, remoteHeight - 1)

    return Pair(remoteX, remoteY)
}

@Composable
fun LogConsoleView(
    logs: List<ScrcpyLog>,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = "Logs",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "投屏与服务端部署日志 (${logs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Logs", modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无运行日志，点击“启动投屏”查看服务初始化与部署详情",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val levelColor = when (log.level) {
                            LogLevel.SUCCESS -> Color(0xFF4CAF50)
                            LogLevel.INFO -> Color(0xFF64B5F6)
                            LogLevel.WARN -> Color(0xFFFFB74D)
                            LogLevel.ERROR -> Color(0xFFE57373)
                        }
                        Text(
                            text = "[${log.timestamp}] ${log.message}",
                            color = levelColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrcpyView(
    controller: ScrcpyController,
    installedApps: List<RemoteAppItem> = emptyList(),
    onRefreshApps: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenState by controller.screenState.collectAsState()
    val config by controller.config.collectAsState()
    val logs by controller.logs.collectAsState()

    var showTextInputDialog by remember { mutableStateOf(false) }
    var textToSend by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showVirtualDisplayDialog by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }
    val isFullscreen by controller.isFullscreen.collectAsState()

    var containerWidth by remember { mutableStateOf(1) }
    var containerHeight by remember { mutableStateOf(1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width
                containerHeight = coordinates.size.height
            }
    ) {
        when (val state = screenState) {
            is ScreenState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Screen Remote",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "远程屏幕投屏与同步操作",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "低延迟画面传输与实时触摸手势交互",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { controller.startMirroring() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("启动投屏", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { controller.wakeUpRemoteScreen() },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("唤醒屏幕")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pre-Start Settings Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("投屏参数预设 (自动保存)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "已记住",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "画面传输分辨率: ${if (config.maxResolution == 0) "原生" else "${config.maxResolution}p"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(480, 720, 1080, 0).forEach { res ->
                                    val isSelected = config.maxResolution == res
                                    val label = if (res == 0) "原生" else "${res}p"
                                    if (isSelected) {
                                        Button(
                                            onClick = { controller.updateConfig(config.copy(maxResolution = res)) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(label, fontSize = 13.sp)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { controller.updateConfig(config.copy(maxResolution = res)) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(label, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "目标帧率: ${config.maxFps} FPS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 30, 60).forEach { fps ->
                                    val isSelected = config.maxFps == fps
                                    val label = "${fps} FPS"
                                    if (isSelected) {
                                        Button(
                                            onClick = { controller.updateConfig(config.copy(maxFps = fps)) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(label, fontSize = 13.sp)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { controller.updateConfig(config.copy(maxFps = fps)) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(label, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("传输码率: ${String.format(Locale.getDefault(), "%.1f", config.bitrate / 1000000f)} Mbps", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(4, 8, 16, 32, 64, 80).forEach { mbps ->
                                    val bps = mbps * 1000000
                                    val isSelected = config.bitrate == bps
                                    if (isSelected) {
                                        Button(
                                            onClick = { controller.updateConfig(config.copy(bitrate = bps)) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("${mbps}M", fontSize = 11.sp)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { controller.updateConfig(config.copy(bitrate = bps)) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("${mbps}M", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            Slider(
                                value = (config.bitrate / 1000000f).coerceIn(1f, 80f),
                                onValueChange = { newValue ->
                                    controller.updateConfig(config.copy(bitrate = (newValue * 1000000).toInt()))
                                },
                                valueRange = 1f..80f,
                                steps = 78
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("息屏控制 (投屏时关闭对端屏幕亮屏)", fontSize = 14.sp)
                                Switch(
                                    checked = config.isScreenOff,
                                    onCheckedChange = { isChecked ->
                                        controller.updateConfig(config.copy(isScreenOff = isChecked))
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("音频转发 (传输对端系统声音)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("开启后将从远端设备采集并实时传输系统声音", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = config.isAudioEnabled,
                                    onCheckedChange = { isChecked ->
                                        controller.updateConfig(config.copy(isAudioEnabled = isChecked))
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Realtime Log Card View
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LogConsoleView(
                            logs = logs,
                            onClear = { controller.clearLogs() },
                            onCopy = { controller.copyLogsToClipboard(context) },
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            is ScreenState.Connecting -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在部署 scrcpy-server 并建立低延迟图像传输...", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    val lastLog = logs.lastOrNull()?.message ?: ""
                    if (lastLog.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "最新进度: $lastLog",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp),
                                maxLines = 3
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { showLogsSheet = true }
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("查看完整部署日志")
                        }
                        OutlinedButton(
                            onClick = { controller.stopMirroring() }
                        ) {
                            Text("取消 / 停止")
                        }
                    }
                }
            }
            is ScreenState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "投屏连接异常",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { controller.startMirroring() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("重试连接")
                        }
                        OutlinedButton(onClick = { showLogsSheet = true }) {
                            Icon(Icons.Default.Terminal, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("查看部署日志")
                        }
                    }
                }
            }
            is ScreenState.Streaming -> {
                StreamingViewContent(
                    state = state,
                    controller = controller,
                    config = config,
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = { controller.toggleFullscreen() },
                    onShowTextInput = { showTextInputDialog = true },
                    onShowVirtualDisplay = { showVirtualDisplayDialog = true },
                    onShowLogs = { showLogsSheet = true },
                    onShowSettings = { showSettingsSheet = true }
                )
            }
        }

        if (showLogsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLogsSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    LogConsoleView(
                        logs = logs,
                        onClear = { controller.clearLogs() },
                        onCopy = { controller.copyLogsToClipboard(context) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        if (showTextInputDialog) {
            AlertDialog(
                onDismissRequest = { showTextInputDialog = false },
                title = { Text("远程文字输入") },
                text = {
                    OutlinedTextField(
                        value = textToSend,
                        onValueChange = { textToSend = it },
                        label = { Text("要发送的文本内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (textToSend.isNotEmpty()) {
                            controller.sendText(textToSend)
                            textToSend = ""
                        }
                        showTextInputDialog = false
                    }) {
                        Text("发送")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextInputDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "屏幕投影与传输设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "传输画面压缩分辨率: ${if (config.maxResolution == 0) "原生" else "${config.maxResolution}p"}",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(480, 720, 1080, 0).forEach { res ->
                            val isSelected = config.maxResolution == res
                            val label = if (res == 0) "原生" else "${res}p"
                            if (isSelected) {
                                Button(
                                    onClick = { controller.updateConfig(config.copy(maxResolution = res)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 13.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { controller.updateConfig(config.copy(maxResolution = res)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "目标帧率: ${config.maxFps} FPS",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60).forEach { fps ->
                            val isSelected = config.maxFps == fps
                            val label = "${fps} FPS"
                            if (isSelected) {
                                Button(
                                    onClick = { controller.updateConfig(config.copy(maxFps = fps)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 13.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { controller.updateConfig(config.copy(maxFps = fps)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("传输码率: ${String.format(Locale.getDefault(), "%.1f", config.bitrate / 1000000f)} Mbps", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(4, 8, 16, 32, 64, 80).forEach { mbps ->
                            val bps = mbps * 1000000
                            val isSelected = config.bitrate == bps
                            if (isSelected) {
                                Button(
                                    onClick = { controller.updateConfig(config.copy(bitrate = bps)) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("${mbps}M", fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { controller.updateConfig(config.copy(bitrate = bps)) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("${mbps}M", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Slider(
                        value = (config.bitrate / 1000000f).coerceIn(1f, 80f),
                        onValueChange = { newValue ->
                            controller.updateConfig(config.copy(bitrate = (newValue * 1000000).toInt()))
                        },
                        valueRange = 1f..80f,
                        steps = 78
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("息屏控制 (关闭对端屏幕亮屏但不锁屏)")
                        Switch(
                            checked = config.isScreenOff,
                            onCheckedChange = { isChecked ->
                                controller.updateConfig(config.copy(isScreenOff = isChecked))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("音频转发 (传输对端设备音频到本地)", fontWeight = FontWeight.Medium)
                            Text("开启后将从远端设备采集并实时传输系统声音", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = config.isAudioEnabled,
                            onCheckedChange = { isChecked ->
                                controller.updateConfig(config.copy(isAudioEnabled = isChecked))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (screenState is ScreenState.Streaming) {
                        Button(
                            onClick = {
                                showSettingsSheet = false
                                controller.stopMirroring()
                                controller.startMirroring()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("应用设置并重启投屏", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                showSettingsSheet = false
                                controller.stopMirroring()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("停止投屏 (保持 ADB 连接)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showVirtualDisplayDialog) {
            val isVirtualActive by controller.isVirtualDisplayActive.collectAsState()
            var vWidth by remember { mutableStateOf(if (config.virtualDisplayWidth > 0) config.virtualDisplayWidth.toString() else "1080") }
            var vHeight by remember { mutableStateOf(if (config.virtualDisplayHeight > 0) config.virtualDisplayHeight.toString() else "1920") }
            var pkgName by remember { mutableStateOf(config.launchPackageOnVirtualDisplay) }
            var selectedAppName by remember { mutableStateOf("") }
            var showAppPickerModal by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showVirtualDisplayDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("独立虚拟副屏", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = if (isVirtualActive) "当前模式: 运行独立虚拟屏 (${controller.activeVirtualWidth}x${controller.activeVirtualHeight})" else "当前模式: 被控端物理主屏幕",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isVirtualActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = vWidth,
                                onValueChange = { vWidth = it },
                                label = { Text("宽度 (px)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = vHeight,
                                onValueChange = { vHeight = it },
                                label = { Text("高度 (px)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("选择要在副屏运行的应用", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            onClick = {
                                if (installedApps.isEmpty() && onRefreshApps != null) {
                                    onRefreshApps()
                                }
                                showAppPickerModal = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val matchedApp = installedApps.firstOrNull { it.packageName == pkgName }
                                    val displayLabel = if (pkgName.isNotBlank()) {
                                        matchedApp?.appName ?: selectedAppName.ifEmpty { pkgName }
                                    } else {
                                        "选择应用..."
                                    }
                                    Text(
                                        text = displayLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (pkgName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (pkgName.isNotBlank()) {
                                        Text(
                                            text = pkgName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "点击从设备已安装应用清单中选择",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (pkgName.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            pkgName = ""
                                            selectedAppName = ""
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "清除选择",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val w = vWidth.toIntOrNull() ?: 1080
                                val h = vHeight.toIntOrNull() ?: 1920
                                controller.updateConfig(
                                    config.copy(
                                        virtualDisplayWidth = w,
                                        virtualDisplayHeight = h,
                                        launchPackageOnVirtualDisplay = pkgName
                                    )
                                )
                                controller.createVirtualDisplayAndLaunch(w, h, pkgName)
                                showVirtualDisplayDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("创建副屏并启动应用", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        if (isVirtualActive) {
                            OutlinedButton(
                                onClick = {
                                    controller.switchToMainDisplay()
                                    showVirtualDisplayDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text("切回物理主屏", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        TextButton(
                            onClick = { showVirtualDisplayDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("取消")
                        }
                    }
                },
                dismissButton = null
            )

            if (showAppPickerModal) {
                var searchQuery by remember { mutableStateOf("") }
                var filterType by remember { mutableIntStateOf(0) }

                ModalBottomSheet(
                    onDismissRequest = { showAppPickerModal = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "选择要启动的应用",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索应用名称或包名...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filterType == 0,
                                onClick = { filterType = 0 },
                                label = { Text("全部 (${installedApps.size})") }
                            )
                            FilterChip(
                                selected = filterType == 1,
                                onClick = { filterType = 1 },
                                label = { Text("第三方 (${installedApps.count { !it.isSystemApp }})") }
                            )
                            FilterChip(
                                selected = filterType == 2,
                                onClick = { filterType = 2 },
                                label = { Text("系统 (${installedApps.count { it.isSystemApp }})") }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            onClick = {
                                pkgName = ""
                                selectedAppName = ""
                                showAppPickerModal = false
                            },
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "不启动应用（仅创建空白虚拟副屏）",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        val filteredApps = remember(installedApps, searchQuery, filterType) {
                            installedApps.filter { app ->
                                val matchesFilter = when (filterType) {
                                    1 -> !app.isSystemApp
                                    2 -> app.isSystemApp
                                    else -> true
                                }
                                val matchesSearch = searchQuery.isBlank() ||
                                    app.appName.contains(searchQuery, ignoreCase = true) ||
                                    app.packageName.contains(searchQuery, ignoreCase = true)
                                matchesFilter && matchesSearch
                            }
                        }

                        if (filteredApps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (installedApps.isEmpty()) "正在读取已安装应用列表..." else "未找到匹配的应用",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (installedApps.isEmpty() && onRefreshApps != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(onClick = { onRefreshApps() }) {
                                            Text("刷新应用列表")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredApps, key = { it.packageName }) { app ->
                                    Surface(
                                        onClick = {
                                            pkgName = app.packageName
                                            selectedAppName = app.appName
                                            showAppPickerModal = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (pkgName == app.packageName)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else
                                            Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (app.isSystemApp)
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                else
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = app.appName.take(1).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = if (app.isSystemApp)
                                                            MaterialTheme.colorScheme.onTertiaryContainer
                                                        else
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.appName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = app.packageName,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (app.isSystemApp) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "系统",
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (pkgName == app.packageName) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "已选择",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingViewContent(
    state: ScreenState.Streaming,
    controller: ScrcpyController,
    config: ScrcpyConfig,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onShowTextInput: () -> Unit,
    onShowVirtualDisplay: () -> Unit,
    onShowLogs: () -> Unit,
    onShowSettings: () -> Unit
) {
    var videoWidth by remember { mutableStateOf(1f) }
    var videoHeight by remember { mutableStateOf(1f) }
    var overlayOffsetX by remember { mutableStateOf(0f) }
    var overlayOffsetY by remember { mutableStateOf(0f) }

    val aspect = if (state.width > 0 && state.height > 0) state.width.toFloat() / state.height.toFloat() else 9f / 16f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(aspect)
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        videoWidth = coordinates.size.width.toFloat()
                        videoHeight = coordinates.size.height.toFloat()
                    }
                    .pointerInput(state.width, state.height, videoWidth, videoHeight) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pathList = mutableListOf<Pair<Int, Int>>()

                            val startCoord = mapLocalTouchToRemote(
                                down.position.x, down.position.y,
                                videoWidth, videoHeight,
                                state.width, state.height
                            )
                            if (startCoord != null) {
                                pathList.add(startCoord)
                                controller.sendTouchEvent("DOWN", startCoord.first, startCoord.second)
                            }

                            var lastPos = down.position

                            do {
                                val event = awaitPointerEvent()
                                val pointerChange = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (pointerChange.pressed) {
                                    val currentPos = pointerChange.position
                                    if ((currentPos - lastPos).getDistance() > 4f) {
                                        lastPos = currentPos
                                        val remoteCoord = mapLocalTouchToRemote(
                                            currentPos.x, currentPos.y,
                                            videoWidth, videoHeight,
                                            state.width, state.height
                                        )
                                        if (remoteCoord != null) {
                                            pathList.add(remoteCoord)
                                            controller.sendTouchEvent("MOVE", remoteCoord.first, remoteCoord.second)
                                        }
                                    }
                                } else {
                                    val upCoord = mapLocalTouchToRemote(
                                        pointerChange.position.x, pointerChange.position.y,
                                        videoWidth, videoHeight,
                                        state.width, state.height
                                    ) ?: pathList.lastOrNull()

                                    if (upCoord != null) {
                                        controller.sendTouchEvent("UP", upCoord.first, upCoord.second)
                                    }
                                    break
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        android.view.SurfaceView(ctx).apply {
                            holder.setFormat(android.graphics.PixelFormat.OPAQUE)
                            holder.addCallback(object : android.view.SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                    controller.setSurface(holder.surface)
                                }

                                override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}

                                override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                    controller.setSurface(null)
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top info bar
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isFullscreen) 28.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${state.width}x${state.height} | ${state.fps.toInt()} FPS | ${state.latencyMs}ms",
                    color = Color.Green,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "退出全屏" else "全屏模式",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Surface(
                    onClick = { controller.stopMirroring() },
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "断开投屏",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Floating draggable navigation bar
        val isVirtualActive by controller.isVirtualDisplayActive.collectAsState()
        val screenWidthDp = LocalConfiguration.current.screenWidthDp

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isFullscreen) 36.dp else 24.dp)
                .widthIn(max = (screenWidthDp - 20).dp)
                .offset { IntOffset(overlayOffsetX.roundToInt(), overlayOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        overlayOffsetX += dragAmount.x
                        overlayOffsetY += dragAmount.y
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { controller.sendBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                IconButton(onClick = { controller.sendHome() }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { controller.sendRecents() }) {
                    Icon(Icons.Default.RecentActors, contentDescription = "Recents")
                }
                IconButton(onClick = { controller.wakeUpRemoteScreen() }) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Wake Screen", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onShowTextInput) {
                    Icon(Icons.Default.Keyboard, contentDescription = "Text Input")
                }
                IconButton(onClick = {
                    val newScreenOff = !config.isScreenOff
                    controller.updateConfig(config.copy(isScreenOff = newScreenOff))
                }) {
                    Icon(
                        Icons.Default.ScreenLockPortrait,
                        contentDescription = "Screen Off Control",
                        tint = if (config.isScreenOff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    val newAudio = !config.isAudioEnabled
                    controller.updateConfig(config.copy(isAudioEnabled = newAudio))
                }) {
                    Icon(
                        imageVector = if (config.isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Audio Forwarding",
                        tint = if (config.isAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onShowVirtualDisplay) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Virtual Display",
                        tint = if (isVirtualActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onShowLogs) {
                    Icon(Icons.Default.Terminal, contentDescription = "Logs", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onShowSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Stream Settings")
                }
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                        tint = if (isFullscreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { controller.stopMirroring() }) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "Stop Mirroring",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

