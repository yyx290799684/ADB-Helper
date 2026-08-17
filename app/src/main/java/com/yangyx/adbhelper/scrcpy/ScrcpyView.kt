package com.yangyx.adbhelper.scrcpy

import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ZoomIn
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

data class ScrcpyCameraItem(
    val id: String,
    val facing: String,
    val title: String,
    val details: String,
    val rawLine: String
)

data class ScrcpyCameraSizeItem(
    val size: String,
    val fpsInfo: String,
    val rawLine: String
)

enum class CameraQueryMode {
    CAMERAS, SIZES
}

fun parseCameraList(output: String): List<ScrcpyCameraItem> {
    val items = mutableListOf<ScrcpyCameraItem>()
    val lines = output.lines()
    val idRegex = Regex("""(?:--camera-id=|camera_id=|\bid=)([0-9a-zA-Z_-]+)""")

    for (line in lines) {
        val trimmed = line.trim()
        val match = idRegex.find(trimmed)
        if (match != null) {
            val id = match.groupValues[1]
            val lower = trimmed.lowercase()
            val facing = when {
                lower.contains("front") -> "front"
                lower.contains("back") -> "back"
                lower.contains("external") -> "external"
                else -> "any"
            }
            val facingText = when (facing) {
                "front" -> "前置镜头 (Front)"
                "back" -> "后置主摄/镜头 (Back)"
                "external" -> "外接摄像头 (External)"
                else -> "通用镜头 (ID: $id)"
            }
            val details = if (trimmed.contains("(") && trimmed.contains(")")) {
                trimmed.substringAfter("(").substringBeforeLast(")")
            } else {
                trimmed
            }
            items.add(
                ScrcpyCameraItem(
                    id = id,
                    facing = facing,
                    title = "镜头 ID: $id - $facingText",
                    details = details,
                    rawLine = trimmed
                )
            )
        }
    }
    return items
}

fun parseCameraSizeList(output: String): List<ScrcpyCameraSizeItem> {
    val items = mutableListOf<ScrcpyCameraSizeItem>()
    val lines = output.lines()
    val sizeRegex = Regex("""(\d{3,5}x\d{3,5})""")
    for (line in lines) {
        val trimmed = line.trim()
        val match = sizeRegex.find(trimmed)
        if (match != null) {
            val size = match.groupValues[1]
            val fps = if (trimmed.contains("fps", ignoreCase = true)) {
                trimmed.substringAfter("fps").trim().removePrefix("=").trim().removePrefix("(").removeSuffix(")")
            } else ""
            items.add(ScrcpyCameraSizeItem(size = size, fpsInfo = fps, rawLine = trimmed))
        }
    }
    return items.distinctBy { it.size }
}

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
    var isServerCardExpanded by remember { mutableStateOf(false) }
    val isFullscreen by controller.isFullscreen.collectAsState()

    var showCameraInfoDialog by remember { mutableStateOf(false) }
    var cameraInfoTitle by remember { mutableStateOf("") }
    var cameraInfoText by remember { mutableStateOf("") }
    var isQueryingCameraInfo by remember { mutableStateOf(false) }
    var cameraQueryMode by remember { mutableStateOf(CameraQueryMode.CAMERAS) }

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
                val isCameraSource = config.videoSource.equals("camera", ignoreCase = true)

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
                                imageVector = if (isCameraSource) Icons.Default.Videocam else Icons.Default.AspectRatio,
                                contentDescription = "Screen Remote",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isCameraSource) "远程摄像头画面镜像" else "远程屏幕投屏与同步操作",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isCameraSource) "低延迟摄像头视频流捕获 (支持 Android 12+ 及麦克风声音采集)" else "低延迟画面传输与实时触摸手势交互",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!ScrcpyServerManager.isServerReady(context)) {
                                            isServerCardExpanded = true
                                            android.widget.Toast.makeText(context, "scrcpy-server 组件未就绪，请先下载", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            controller.startMirroring()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(if (isCameraSource) Icons.Default.Videocam else Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isCameraSource) "启动相机镜像" else "启动屏幕投屏", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

                    // scrcpy-server JAR Component Management Card
                    ScrcpyServerComponentCard(
                        isExpanded = isServerCardExpanded,
                        onToggleExpand = { isServerCardExpanded = !isServerCardExpanded }
                    )

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
                                    Text("投屏与相机参数预设 (自动保存)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

                            ScrcpyConfigSettingsSection(
                                config = config,
                                controller = controller,
                                onQueryCameras = {
                                    cameraQueryMode = CameraQueryMode.CAMERAS
                                    cameraInfoTitle = "被控端可用摄像头列表 (--list-cameras)"
                                    cameraInfoText = "正在通过 ADB 查询设备摄像头硬件..."
                                    isQueryingCameraInfo = true
                                    showCameraInfoDialog = true
                                    controller.listAvailableCameras { result ->
                                        cameraInfoText = result
                                        isQueryingCameraInfo = false
                                    }
                                },
                                onQueryCameraSizes = {
                                    cameraQueryMode = CameraQueryMode.SIZES
                                    val camId = config.cameraId.ifEmpty { "0" }
                                    cameraInfoTitle = "摄像头 (ID: $camId) 支持的分辨率尺寸"
                                    cameraInfoText = "正在通过 ADB 查询相机尺寸列表..."
                                    isQueryingCameraInfo = true
                                    showCameraInfoDialog = true
                                    controller.listAvailableCameraSizes(camId) { result ->
                                        cameraInfoText = result
                                        isQueryingCameraInfo = false
                                    }
                                }
                            )
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
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = "投屏与摄像头参数设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ScrcpyConfigSettingsSection(
                        config = config,
                        controller = controller,
                        onQueryCameras = {
                            cameraQueryMode = CameraQueryMode.CAMERAS
                            cameraInfoTitle = "被控端可用摄像头列表 (--list-cameras)"
                            cameraInfoText = "正在通过 ADB 查询设备摄像头硬件..."
                            isQueryingCameraInfo = true
                            showCameraInfoDialog = true
                            controller.listAvailableCameras { result ->
                                cameraInfoText = result
                                isQueryingCameraInfo = false
                            }
                        },
                        onQueryCameraSizes = {
                            cameraQueryMode = CameraQueryMode.SIZES
                            val camId = config.cameraId.ifEmpty { "0" }
                            cameraInfoTitle = "摄像头 (ID: $camId) 支持的分辨率尺寸"
                            cameraInfoText = "正在通过 ADB 查询相机尺寸列表..."
                            isQueryingCameraInfo = true
                            showCameraInfoDialog = true
                            controller.listAvailableCameraSizes(camId) { result ->
                                cameraInfoText = result
                                isQueryingCameraInfo = false
                            }
                        }
                    )

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
                            Text("应用设置并重启镜像", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                            Text("停止镜像 (保持 ADB 连接)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showCameraInfoDialog) {
            var showRawText by remember { mutableStateOf(false) }
            val parsedCameras = remember(cameraInfoText, cameraQueryMode) {
                if (cameraQueryMode == CameraQueryMode.CAMERAS) parseCameraList(cameraInfoText) else emptyList()
            }
            val parsedSizes = remember(cameraInfoText, cameraQueryMode) {
                if (cameraQueryMode == CameraQueryMode.SIZES) parseCameraSizeList(cameraInfoText) else emptyList()
            }

            AlertDialog(
                onDismissRequest = { showCameraInfoDialog = false },
                icon = {
                    Icon(
                        imageVector = if (cameraQueryMode == CameraQueryMode.CAMERAS) Icons.Default.Videocam else Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        cameraInfoTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        if (isQueryingCameraInfo) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("正在执行 ADB 探测指令...", fontSize = 13.sp)
                            }
                        } else if (!showRawText && cameraQueryMode == CameraQueryMode.CAMERAS && parsedCameras.isNotEmpty()) {
                            Text(
                                "点击可直接选择对应镜头并自动填充配置：",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(parsedCameras) { cam ->
                                    val isSelected = config.cameraId == cam.id
                                    Surface(
                                        onClick = {
                                            controller.updateConfig(config.copy(cameraId = cam.id, cameraFacing = cam.facing))
                                            android.widget.Toast.makeText(context, "已选择镜头 ID: ${cam.id}", android.widget.Toast.LENGTH_SHORT).show()
                                            showCameraInfoDialog = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    controller.updateConfig(config.copy(cameraId = cam.id, cameraFacing = cam.facing))
                                                    android.widget.Toast.makeText(context, "已选择镜头 ID: ${cam.id}", android.widget.Toast.LENGTH_SHORT).show()
                                                    showCameraInfoDialog = false
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = cam.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (cam.details.isNotBlank()) {
                                                    Text(
                                                        text = cam.details,
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (!showRawText && cameraQueryMode == CameraQueryMode.SIZES && parsedSizes.isNotEmpty()) {
                            Text(
                                "点击可直接选择对应分辨率并自动填充配置：",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(parsedSizes) { sz ->
                                    val isSelected = config.cameraSize == sz.size
                                    Surface(
                                        onClick = {
                                            controller.updateConfig(config.copy(cameraSize = sz.size, cameraAspectRatio = ""))
                                            android.widget.Toast.makeText(context, "已选择尺寸: ${sz.size}", android.widget.Toast.LENGTH_SHORT).show()
                                            showCameraInfoDialog = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    controller.updateConfig(config.copy(cameraSize = sz.size, cameraAspectRatio = ""))
                                                    android.widget.Toast.makeText(context, "已选择尺寸: ${sz.size}", android.widget.Toast.LENGTH_SHORT).show()
                                                    showCameraInfoDialog = false
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sz.size,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (sz.fpsInfo.isNotBlank()) {
                                                    Text(
                                                        text = "支持帧率: ${sz.fpsInfo}",
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                            ) {
                                Text(
                                    text = cameraInfoText.ifBlank { "未读取到数据" },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                )
                            }
                        }

                        if (!isQueryingCameraInfo && ((cameraQueryMode == CameraQueryMode.CAMERAS && parsedCameras.isNotEmpty()) || (cameraQueryMode == CameraQueryMode.SIZES && parsedSizes.isNotEmpty()))) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { showRawText = !showRawText },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (showRawText) "返回选择列表" else "查看原始 ADB 输出", fontSize = 11.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Row {
                        if (!isQueryingCameraInfo && cameraInfoText.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("scrcpy-camera-info", cameraInfoText)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("复制")
                            }
                        }
                        Button(onClick = { showCameraInfoDialog = false }) {
                            Text("关闭")
                        }
                    }
                }
            )
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
    var cameraRotationDegrees by remember { mutableIntStateOf(0) }

    val isCamera = config.videoSource.equals("camera", ignoreCase = true)
    val isRotated90or270 = isCamera && (cameraRotationDegrees % 180 != 0)
    val baseAspect = if (state.width > 0 && state.height > 0) state.width.toFloat() / state.height.toFloat() else 9f / 16f
    val effectiveAspect = if (isRotated90or270) {
        if (state.height > 0 && state.width > 0) state.height.toFloat() / state.width.toFloat() else 16f / 9f
    } else {
        baseAspect
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val containerAspect = maxWidth.value / maxHeight.value
            val (targetW, targetH) = if (containerAspect > effectiveAspect) {
                (maxHeight.value * effectiveAspect).dp to maxHeight
            } else {
                maxWidth to (maxWidth.value / effectiveAspect).dp
            }

            val innerW = if (isRotated90or270) targetH else targetW
            val innerH = if (isRotated90or270) targetW else targetH

            Box(
                modifier = Modifier.size(targetW, targetH),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(innerW, innerH)
                        .graphicsLayer {
                            rotationZ = if (isCamera) cameraRotationDegrees.toFloat() else 0f
                        }
                        .onGloballyPositioned { coordinates ->
                            videoWidth = coordinates.size.width.toFloat()
                            videoHeight = coordinates.size.height.toFloat()
                        }
                        .then(
                            if (!isCamera) {
                                Modifier.pointerInput(state.width, state.height, videoWidth, videoHeight) {
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
                            } else Modifier
                        )
                ) {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                isOpaque = true
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        controller.setSurface(Surface(surfaceTexture))
                                    }

                                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        controller.setSurface(Surface(surfaceTexture))
                                    }

                                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                                        controller.setSurface(null)
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                                }
                                if (isAvailable && surfaceTexture != null) {
                                    controller.setSurface(Surface(surfaceTexture))
                                }
                            }
                        },
                        update = { view ->
                            if (view.isAvailable && view.surfaceTexture != null && controller.activeSurface == null) {
                                controller.setSurface(Surface(view.surfaceTexture))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
                    text = "${state.width}x${state.height} | ${state.fps.toInt()} FPS | ${state.latencyMs}ms" +
                        if (isCamera && cameraRotationDegrees != 0) " | ${cameraRotationDegrees}°" else "",
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
                if (!isCamera) {
                    IconButton(onClick = { controller.sendBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                    IconButton(onClick = { controller.sendHome() }) {
                        Icon(Icons.Default.Home, contentDescription = "主页")
                    }
                    IconButton(onClick = { controller.sendRecents() }) {
                        Icon(Icons.Default.RecentActors, contentDescription = "多任务")
                    }
                    IconButton(onClick = { controller.wakeUpRemoteScreen() }) {
                        Icon(Icons.Default.LockOpen, contentDescription = "唤醒点亮屏幕", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val newStayAwake = !config.isStayAwake
                        controller.updateConfig(config.copy(isStayAwake = newStayAwake))
                    }) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = "阻止休眠",
                            tint = if (config.isStayAwake) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onShowTextInput) {
                        Icon(Icons.Default.Keyboard, contentDescription = "文字输入")
                    }
                    IconButton(onClick = {
                        val newScreenOff = !config.isScreenOff
                        controller.updateConfig(config.copy(isScreenOff = newScreenOff))
                    }) {
                        Icon(
                            Icons.Default.ScreenLockPortrait,
                            contentDescription = "熄屏控制",
                            tint = if (config.isScreenOff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (isCamera) {
                    IconButton(onClick = {
                        cameraRotationDegrees = (cameraRotationDegrees - 90 + 360) % 360
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = "向左旋转90°",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        cameraRotationDegrees = (cameraRotationDegrees + 90) % 360
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "向右旋转90°",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        val newTorch = !config.cameraTorch
                        controller.updateConfig(config.copy(cameraTorch = newTorch))
                    }) {
                        Icon(
                            if (config.cameraTorch) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "闪光灯",
                            tint = if (config.cameraTorch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = {
                    val newAudio = !config.isAudioEnabled
                    controller.updateConfig(config.copy(isAudioEnabled = newAudio))
                }) {
                    Icon(
                        imageVector = if (config.isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "音频传输",
                        tint = if (config.isAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!isCamera) {
                    IconButton(onClick = onShowVirtualDisplay) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "虚拟副屏",
                            tint = if (isVirtualActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = onShowLogs) {
                    Icon(Icons.Default.Terminal, contentDescription = "运行日志", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onShowSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "投屏设置")
                }
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "退出全屏" else "全屏显示",
                        tint = if (isFullscreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { controller.stopMirroring() }) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "断开投屏",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ScrcpyConfigSettingsSection(
    config: ScrcpyConfig,
    controller: ScrcpyController,
    onQueryCameras: () -> Unit,
    onQueryCameraSizes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCamera = config.videoSource.equals("camera", ignoreCase = true)

    Column(modifier = modifier.fillMaxWidth()) {
        // Modern Segmented Control for Video Source Selector
        Text(
            text = "视频采集源",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val displaySelected = !isCamera
                Surface(
                    onClick = { controller.updateConfig(config.copy(videoSource = "display")) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (displaySelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    tonalElevation = if (displaySelected) 3.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = if (displaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "手机屏幕",
                            fontSize = 13.sp,
                            fontWeight = if (displaySelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (displaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val cameraSelected = isCamera
                Surface(
                    onClick = { controller.updateConfig(config.copy(videoSource = "camera")) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (cameraSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    tonalElevation = if (cameraSelected) 3.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (cameraSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "相机镜头",
                            fontSize = 13.sp,
                            fontWeight = if (cameraSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (cameraSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isCamera) {
            // Camera Specific Settings
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("摄像头硬件参数 (Android 12+)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hardware Query Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onQueryCameras,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("查询镜头列表", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onQueryCameraSizes,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("查询支持尺寸", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Camera Facing
                    Text("镜头朝向:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("back" to "后置主摄", "front" to "前置自拍", "external" to "外接镜头", "any" to "智能自选").forEach { (fKey, fLabel) ->
                            val isSel = config.cameraFacing.equals(fKey, ignoreCase = true) && config.cameraId.isBlank()
                            if (isSel) {
                                Button(
                                    onClick = { controller.updateConfig(config.copy(cameraFacing = fKey, cameraId = "")) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Text(fLabel, fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { controller.updateConfig(config.copy(cameraFacing = fKey, cameraId = "")) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Text(fLabel, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Explicit Camera ID & Size text inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = config.cameraId,
                            onValueChange = { controller.updateConfig(config.copy(cameraId = it)) },
                            label = { Text("指定镜头ID", fontSize = 11.sp) },
                            placeholder = { Text("例: 0, 1, 2", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = config.cameraSize,
                            onValueChange = { controller.updateConfig(config.copy(cameraSize = it)) },
                            label = { Text("指定画面尺寸", fontSize = 11.sp) },
                            placeholder = { Text("例: 1920x1080", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Aspect ratio chips
                    Text("画面比例:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("" to "默认自适应", "16:9" to "16:9 宽屏", "4:3" to "4:3 画幅", "sensor" to "传感器原生").forEach { (arKey, arLabel) ->
                            val isSel = config.cameraAspectRatio.equals(arKey, ignoreCase = true)
                            if (isSel) {
                                Button(
                                    onClick = { controller.updateConfig(config.copy(cameraAspectRatio = arKey)) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Text(arLabel, fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { controller.updateConfig(config.copy(cameraAspectRatio = arKey)) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Text(arLabel, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Zoom level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("变焦缩放: ${String.format(Locale.getDefault(), "%.1fx", config.cameraZoom)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        if (config.cameraZoom > 1.0f) {
                            TextButton(onClick = { controller.updateConfig(config.copy(cameraZoom = 1.0f)) }) {
                                Text("重置 1.0x", fontSize = 11.sp)
                            }
                        }
                    }
                    Slider(
                        value = config.cameraZoom,
                        onValueChange = { controller.updateConfig(config.copy(cameraZoom = (it * 10).toInt() / 10f)) },
                        valueRange = 1.0f..5.0f,
                        steps = 39
                    )

                    // Flashlight / High-speed switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启动时开启闪光灯 (--camera-torch)", fontSize = 12.sp)
                        Switch(
                            checked = config.cameraTorch,
                            onCheckedChange = { controller.updateConfig(config.copy(cameraTorch = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("高速捕捉模式 (--camera-high-speed)", fontSize = 12.sp)
                        Switch(
                            checked = config.cameraHighSpeed,
                            onCheckedChange = { controller.updateConfig(config.copy(cameraHighSpeed = it)) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        } else {
            // Display Specific Resolution Settings
            Text(
                text = "画面传输分辨率: ${if (config.maxResolution == 0) "原生" else "${config.maxResolution}p"}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
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
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Video Codec
        Text(
            text = "视频编码格式: ${config.videoCodec.uppercase()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("h264" to "H.264 (默认)", "h265" to "H.265 (HEVC)", "av1" to "AV1").forEach { (codecKey, codecLabel) ->
                val isSelected = config.videoCodec.equals(codecKey, ignoreCase = true)
                if (isSelected) {
                    Button(
                        onClick = { controller.updateConfig(config.copy(videoCodec = codecKey)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(codecLabel, fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = { controller.updateConfig(config.copy(videoCodec = codecKey)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(codecLabel, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Target FPS
        Text(
            text = "目标帧率: ${config.maxFps} FPS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
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

        Spacer(modifier = Modifier.height(14.dp))

        // Bitrate
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

        Spacer(modifier = Modifier.height(12.dp))

        // Power On at start toggle (--no-power-on)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("启动时点亮屏幕", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("开启时启动投屏自动点亮唤醒屏幕；关闭则禁止唤醒保持当前状态 (--no-power-on)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = config.isPowerOn,
                onCheckedChange = { isChecked ->
                    controller.updateConfig(config.copy(isPowerOn = isChecked))
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Power Off on close toggle (--power-off-on-close)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("退出时自动息屏", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("在关闭投屏或停止会话时让对端设备自动息屏 (--power-off-on-close)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = config.isPowerOffOnClose,
                onCheckedChange = { isChecked ->
                    controller.updateConfig(config.copy(isPowerOffOnClose = isChecked))
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stay awake toggle (--stay-awake)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("阻止设备休眠", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("投屏或镜头采集期间阻止对端设备进入睡眠休眠状态 (--stay-awake)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = config.isStayAwake,
                onCheckedChange = { isChecked ->
                    controller.updateConfig(config.copy(isStayAwake = isChecked))
                }
            )
        }

        if (!isCamera) {
            Spacer(modifier = Modifier.height(10.dp))

            // Screen off control toggle (--turn-screen-off)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("熄屏镜像控制", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("开启后关闭对端手机屏幕背光亮屏，但保持投屏正常流转 (--turn-screen-off)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = config.isScreenOff,
                    onCheckedChange = { isChecked ->
                        controller.updateConfig(config.copy(isScreenOff = isChecked))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Audio Forwarding toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCamera) "音频转发 (采集麦克风声音)" else "音频转发 (传输系统声音)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isCamera) "摄像头镜像时自动采集麦克风音频流并实时传输" else "开启后从远端设备采集系统声音并实时传输",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
fun ScrcpyServerComponentCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val serverState by ScrcpyServerManager.state.collectAsState()
    var proxyDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ScrcpyServerManager.init(context)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row (Clickable to toggle expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = "Server Component",
                        tint = if (serverState.isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "scrcpy-server 服务端组件",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (serverState.isReady) "版本: ${serverState.localVersion ?: "已就绪"}" else "未就绪 (投屏前需从 GitHub 下载)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (serverState.isReady) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (serverState.isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (serverState.isReady) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (serverState.isReady) {
                                    val sizeStr = ScrcpyServerManager.formatSize(serverState.localFileSize)
                                    "已就绪 ($sizeStr)"
                                } else {
                                    "未就绪"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (serverState.isReady) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "收起" else "展开"
                        )
                    }
                }
            }

            // Expanded Area
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "本地状态: ${if (serverState.isReady) "已就绪 (${serverState.localVersion ?: ""}, ${ScrcpyServerManager.formatSize(serverState.localFileSize)})" else "未下载"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (serverState.latestVersion != null) {
                            Text(
                                text = "GitHub 最新: ${serverState.latestVersion}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Proxy Selector
                    Text(
                        text = "GitHub 下载代理节点 (国内加速):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { proxyDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = serverState.selectedProxyName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = proxyDropdownExpanded,
                            onDismissRequest = { proxyDropdownExpanded = false }
                        ) {
                            ScrcpyServerManager.PROXY_OPTIONS.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(opt.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (opt.description.isNotEmpty()) {
                                                Text(opt.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        ScrcpyServerManager.setProxy(context, opt.name)
                                        proxyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Downloading Progress Indicator
                    if (serverState.isDownloading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "下载中: ${(serverState.downloadProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = serverState.downloadSpeedText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = serverState.downloadProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Status message
                    if (!serverState.statusMessage.isNullOrBlank()) {
                        Text(
                            text = serverState.statusMessage ?: "",
                            fontSize = 12.sp,
                            color = if (serverState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    ScrcpyServerManager.checkUpdate(context)
                                }
                            },
                            enabled = !serverState.isDownloading && !serverState.isCheckingUpdate,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (serverState.isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("检查更新", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    ScrcpyServerManager.downloadServer(context)
                                }
                            },
                            enabled = !serverState.isDownloading,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (serverState.isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("下载中...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (!serverState.isReady) "从 GitHub 下载" else "更新/重新下载",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (serverState.isReady) {
                            IconButton(
                                onClick = { ScrcpyServerManager.deleteServer(context) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "删除本地组件",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

