package com.yangyx.adbhelper.scrcpy

import android.content.pm.ActivityInfo
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.ScreenRotationAlt
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.SliderDefaults
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.yangyx.adbhelper.touch.MultiTouchVisualOverlay
import com.yangyx.adbhelper.touch.RemoteTouchpadScreen
import com.yangyx.adbhelper.ui.models.RemoteAppItem
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextAlign
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

        val isDarkTheme = isSystemInDarkTheme()

        Surface(
            color = if (isDarkTheme) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = if (!isDarkTheme) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                            LogLevel.SUCCESS -> if (isDarkTheme) Color(0xFF4CAF50) else Color(0xFF2E7D32)
                            LogLevel.INFO -> if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1565C0)
                            LogLevel.WARN -> if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFE65100)
                            LogLevel.ERROR -> if (isDarkTheme) Color(0xFFE57373) else Color(0xFFC62828)
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

    var showEncoderDialog by remember { mutableStateOf(false) }
    var isQueryingEncoders by remember { mutableStateOf(false) }
    var encoderQueryRawText by remember { mutableStateOf("") }
    var detectedEncoders by remember { mutableStateOf<List<ScrcpyController.ScrcpyEncoderInfo>>(emptyList()) }

    var containerWidth by remember { mutableStateOf(1) }
    var containerHeight by remember { mutableStateOf(1) }

    val isStreaming = screenState is ScreenState.Streaming

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isStreaming) Color.Black else MaterialTheme.colorScheme.background)
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width
                containerHeight = coordinates.size.height
            }
    ) {
        when (val state = screenState) {
            is ScreenState.Idle -> {
                val isCameraSource = config.videoSource.equals("camera", ignoreCase = true)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item(key = "hero_banner") {
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
                    }

                    item(key = "server_component") {
                        // scrcpy-server JAR Component Management Card
                        ScrcpyServerComponentCard(
                            isExpanded = isServerCardExpanded,
                            onToggleExpand = { isServerCardExpanded = !isServerCardExpanded }
                        )
                    }

                    item(key = "settings_section") {
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
                                    },
                                    onQueryEncoders = {
                                        showEncoderDialog = true
                                        isQueryingEncoders = true
                                        encoderQueryRawText = "正在通过 ADB 查询被控端编解码器硬件 (scrcpy --list-encoders)..."
                                        controller.listAvailableEncoders { encoders, rawOutput ->
                                            detectedEncoders = encoders
                                            encoderQueryRawText = rawOutput
                                            isQueryingEncoders = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item(key = "log_console") {
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
                    }
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
                    Text("正在部署 scrcpy-server 并建立低延迟图像传输...", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
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
                        color = MaterialTheme.colorScheme.onBackground,
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
                        },
                        onQueryEncoders = {
                            showEncoderDialog = true
                            isQueryingEncoders = true
                            encoderQueryRawText = "正在通过 ADB 查询被控端编解码器硬件 (scrcpy --list-encoders)..."
                            controller.listAvailableEncoders { encoders, rawOutput ->
                                detectedEncoders = encoders
                                encoderQueryRawText = rawOutput
                                isQueryingEncoders = false
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

        if (showEncoderDialog) {
            EncoderSelectionDialog(
                currentEncoder = config.videoEncoder,
                isQuerying = isQueryingEncoders,
                encoders = detectedEncoders,
                rawOutput = encoderQueryRawText,
                onSelectEncoder = { encoder ->
                    val matchedCodec = encoder?.codec ?: config.videoCodec
                    val encoderName = encoder?.name ?: ""
                    controller.updateConfig(config.copy(videoEncoder = encoderName, videoCodec = matchedCodec))
                    if (encoderName.isBlank()) {
                        Toast.makeText(context, "已恢复系统默认编码器", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "已选择编码器: $encoderName ($matchedCodec)", Toast.LENGTH_SHORT).show()
                    }
                    showEncoderDialog = false
                },
                onDismiss = { showEncoderDialog = false }
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
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = app.packageName,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (app.versionName.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "v${app.versionName}",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
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
    var showMultiTouchDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }
    var isTopBarExpanded by remember(config.isTopBarExpanded) { mutableStateOf(config.isTopBarExpanded) }
    var topBarOffsetX by remember { mutableFloatStateOf(0f) }
    var topBarOffsetY by remember { mutableFloatStateOf(0f) }
    var isToolbarExpanded by remember(config.isToolbarExpanded) { mutableStateOf(config.isToolbarExpanded) }
    var miniFabOffsetX by remember { mutableFloatStateOf(0f) }
    var miniFabOffsetY by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current

    // 投屏连接成功后，过 5 秒自动将上方的分辨率、帧率悬浮窗收起为小胶囊（用户手动再次展开后同样在 5 秒无操作后自动折叠）
    LaunchedEffect(isTopBarExpanded) {
        if (isTopBarExpanded) {
            kotlinx.coroutines.delay(5000L)
            isTopBarExpanded = false
            controller.updateConfig(config.copy(isTopBarExpanded = false))
        }
    }

    val activity = context as? Activity
    val isRemoteLandscape = state.width > state.height && state.width > 0 && state.height > 0

    // Adaptive orientation lifecycle: Automatically match host orientation to remote screen
    LaunchedEffect(isRemoteLandscape, config.autoRotateHost, config.hostOrientationMode, isFullscreen) {
        if (activity != null) {
            when (config.hostOrientationMode) {
                HostOrientationMode.SENSOR -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
                HostOrientationMode.PORTRAIT -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                HostOrientationMode.LANDSCAPE -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                HostOrientationMode.AUTO -> {
                    if (config.autoRotateHost || isFullscreen) {
                        if (isRemoteLandscape) {
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                        }
                    } else {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

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
                ) {
                    // 1. TextureView for rendering Scrcpy / Camera video stream
                    AndroidView(
                        factory = { ctx ->
                            var activeSurfaceWrapper: Surface? = null
                            TextureView(ctx).apply {
                                isOpaque = true
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        activeSurfaceWrapper?.release()
                                        val newSurface = Surface(surfaceTexture)
                                        activeSurfaceWrapper = newSurface
                                        controller.setSurface(newSurface)
                                    }

                                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                        surfaceTexture.setDefaultBufferSize(width, height)
                                        // Do not recreate Surface on size change: the existing Surface stays bound to surfaceTexture
                                    }

                                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                                        controller.setSurface(null)
                                        activeSurfaceWrapper = null
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                                }
                                if (isAvailable && surfaceTexture != null) {
                                    activeSurfaceWrapper?.release()
                                    val newSurface = Surface(surfaceTexture)
                                    activeSurfaceWrapper = newSurface
                                    controller.setSurface(newSurface)
                                }
                            }
                        },
                        update = { view ->
                            if (view.isAvailable && view.surfaceTexture != null && controller.activeSurface == null) {
                                val newSurface = Surface(view.surfaceTexture)
                                controller.setSurface(newSurface)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. High-performance Remote Multi-Touch Capture Layer
                    if (!isCamera) {
                        AndroidView(
                            factory = { ctx ->
                                android.view.View(ctx).apply {
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    setOnTouchListener { v, event ->
                                        val vw = v.width
                                        val vh = v.height
                                        val isAv1 = controller.config.value.videoCodec.equals("av1", ignoreCase = true) || controller.config.value.videoCodec.equals("av01", ignoreCase = true)
                                        val targetW = if (isAv1) {
                                            controller.remoteWidth.takeIf { it > 0 } ?: (if (controller.nativeWidth > 0) controller.nativeWidth else state.width)
                                        } else {
                                            if (controller.nativeWidth > 0) controller.nativeWidth else state.width
                                        }
                                        val targetH = if (isAv1) {
                                            controller.remoteHeight.takeIf { it > 0 } ?: (if (controller.nativeHeight > 0) controller.nativeHeight else state.height)
                                        } else {
                                            if (controller.nativeHeight > 0) controller.nativeHeight else state.height
                                        }
                                        if (vw > 0 && vh > 0 && targetW > 0 && targetH > 0) {
                                            controller.multiTouchController.onNativeMotionEvent(
                                                event = event,
                                                viewWidth = vw,
                                                viewHeight = vh,
                                                remoteWidth = targetW,
                                                remoteHeight = targetH
                                            )
                                        }
                                        true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // 3. Multi-Touch Visual Overlay (Visual Feedback, Rings, Badges, Distance & Trails)
                        MultiTouchVisualOverlay(
                            controller = controller.multiTouchController,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Top info bar (Collapsible & Expandable with smooth centered animation and fixed content metrics to prevent jitter)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isFullscreen) 24.dp else 10.dp)
                .offset { IntOffset(topBarOffsetX.roundToInt(), topBarOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress { change, dragAmount ->
                        change.consume()
                        topBarOffsetX += dragAmount.x
                        topBarOffsetY += dragAmount.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isTopBarExpanded,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.88f, animationSpec = tween(260)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.88f, animationSpec = tween(180))
                        )
                },
                contentAlignment = Alignment.Center,
                label = "TopInfoBarTransition"
            ) { expanded ->
                if (expanded) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 使用固定最小宽度与等宽数字展示，防止 FPS/延迟数值跳动引起胶囊整体宽度频繁抖动
                            val fpsInt = state.fps.toInt().coerceIn(0, 999)
                            val latencyInt = state.latencyMs.coerceIn(0, 9999)
                            val extraCam = if (isCamera && cameraRotationDegrees != 0) " | ${cameraRotationDegrees}°" else ""
                            val statsText = "${state.width}x${state.height} | ${String.format(Locale.US, "%2d", fpsInt)} FPS | ${latencyInt}ms$extraCam"

                            Text(
                                text = statsText,
                                color = Color.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.widthIn(min = 150.dp)
                            )
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (isFullscreen) "退出全屏" else "全屏模式",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Surface(
                                onClick = { controller.stopMirroring() },
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "断开投屏",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    isTopBarExpanded = false
                                    controller.updateConfig(config.copy(isTopBarExpanded = false))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "折叠悬浮条 (长按可拖动)",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Collapsed Top Info Badge (Click to expand, long press to drag)
                    // 设置固定内容宽度与 Monospace 等宽字体，使 0-120 FPS 切换时胶囊宽度始终恒定 92dp，绝无任何抖动
                    Surface(
                        onClick = {
                            isTopBarExpanded = true
                            controller.updateConfig(config.copy(isTopBarExpanded = true))
                        },
                        color = Color.Black.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .width(92.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                .size(7.dp)
                                .background(Color.Green, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val fpsInt = state.fps.toInt().coerceIn(0, 999)
                            Text(
                                text = "${fpsInt} FPS",
                                color = Color.Green,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "展开悬浮条 (长按可拖动)",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating draggable navigation bar (Collapsible & Expandable with smooth animations)
        val isVirtualActive by controller.isVirtualDisplayActive.collectAsState()
        val screenWidthDp = LocalConfiguration.current.screenWidthDp

        AnimatedVisibility(
            visible = isToolbarExpanded,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(280)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(280)) + scaleIn(initialScale = 0.9f, animationSpec = tween(280)),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(200))
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(if (config.isToolbarDoubleRow) 20.dp else 28.dp),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .padding(bottom = if (isFullscreen) 36.dp else 24.dp)
                    .widthIn(max = (screenWidthDp - 20).dp)
                    .offset { IntOffset(overlayOffsetX.roundToInt(), overlayOffsetY.roundToInt()) }
            ) {
                @OptIn(ExperimentalFoundationApi::class)
                @Composable
                fun ToolbarActionItem(
                    icon: androidx.compose.ui.graphics.vector.ImageVector,
                    name: String,
                    detailName: String = name,
                    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick: () -> Unit
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = onClick,
                                onLongClick = {
                                    Toast.makeText(context, "功能: $detailName", Toast.LENGTH_SHORT).show()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                @Composable
                fun RenderToolbarButton(actionId: String) {
                    when (actionId) {
                        "back" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.ArrowBack,
                                name = "返回",
                                detailName = "返回键 (Back)",
                                onClick = { controller.sendBack() }
                            )
                        }
                        "home" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.Home,
                                name = "主页",
                                detailName = "主页键 (Home)",
                                onClick = { controller.sendHome() }
                            )
                        }
                        "recents" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.RecentActors,
                                name = "多任务",
                                detailName = "多任务 / 最近应用 (Recents)",
                                onClick = { controller.sendRecents() }
                            )
                        }
                        "wake_up" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.LockOpen,
                                name = "唤醒屏幕",
                                detailName = "唤醒并点亮对端屏幕",
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { controller.wakeUpRemoteScreen() }
                            )
                        }
                        "orientation" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.ScreenRotation,
                                name = "屏幕方向",
                                detailName = "屏幕方向自适应与旋转控制",
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { showOrientationDialog = true }
                            )
                        }
                        "multi_touch" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.TouchApp,
                                name = "多点触控",
                                detailName = "多点触控与手势辅助设置",
                                onClick = { showMultiTouchDialog = true }
                            )
                        }
                        "keep_active", "stay_awake" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.WbSunny,
                                name = "保持活跃",
                                detailName = if (config.isKeepActive) "关闭设备保持活跃" else "开启保持活跃 (阻止休眠 --keep-active)",
                                tint = if (config.isKeepActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    val newKeepActive = !config.isKeepActive
                                    controller.updateConfig(config.copy(isKeepActive = newKeepActive))
                                }
                            )
                        }
                        "text_input" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.Keyboard,
                                name = "文字输入",
                                detailName = "发送文本/剪贴板内容到远端",
                                onClick = onShowTextInput
                            )
                        }
                        "screen_off" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.ScreenLockPortrait,
                                name = "熄屏控制",
                                detailName = if (config.isScreenOff) "点亮远端实体屏幕" else "熄灭对端实体屏幕 (仅镜像)",
                                tint = if (config.isScreenOff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    val newScreenOff = !config.isScreenOff
                                    controller.updateConfig(config.copy(isScreenOff = newScreenOff))
                                }
                            )
                        }
                        "volume_down" -> ToolbarActionItem(
                            icon = Icons.Default.VolumeDown,
                            name = "音量-",
                            detailName = "降低被控端系统硬件音量 (Volume Down)",
                            onClick = {
                                controller.sendVolumeDown()
                                Toast.makeText(context, "远程音量 -", Toast.LENGTH_SHORT).show()
                            }
                        )
                        "volume_up" -> ToolbarActionItem(
                            icon = Icons.Default.VolumeUp,
                            name = "音量+",
                            detailName = "增加被控端系统硬件音量 (Volume Up)",
                            onClick = {
                                controller.sendVolumeUp()
                                Toast.makeText(context, "远程音量 +", Toast.LENGTH_SHORT).show()
                            }
                        )
                        "volume_panel" -> ToolbarActionItem(
                            icon = Icons.Default.GraphicEq,
                            name = "远端音量",
                            detailName = "远程设备系统音量控制面板 (滑块/静音/通道)",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { showVolumeDialog = true }
                        )
                        "audio_stream" -> ToolbarActionItem(
                            icon = Icons.Default.Headphones,
                            name = "音频流转",
                            detailName = if (config.isAudioEnabled) "关闭对端音频流转到本机" else "开启对端实时音频流转到本机",
                            tint = if (config.isAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                val newAudio = !config.isAudioEnabled
                                controller.updateConfig(config.copy(isAudioEnabled = newAudio))
                            }
                        )
                        "virtual_display" -> if (!isCamera) {
                            ToolbarActionItem(
                                icon = Icons.Default.Apps,
                                name = "虚拟副屏",
                                detailName = "开启独立虚拟副屏分屏",
                                tint = if (isVirtualActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = onShowVirtualDisplay
                            )
                        }
                        "logs" -> ToolbarActionItem(
                            icon = Icons.Default.Terminal,
                            name = "运行日志",
                            detailName = "查看 Scrcpy 底层运行与调试日志",
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = onShowLogs
                        )
                        "settings" -> ToolbarActionItem(
                            icon = Icons.Default.Settings,
                            name = "投屏设置",
                            detailName = "调整分辨率、码率、编解码器与按键排序",
                            onClick = onShowSettings
                        )
                        "fullscreen" -> ToolbarActionItem(
                            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            name = "全屏显示",
                            detailName = if (isFullscreen) "退出全屏模式" else "进入沉浸式全屏模式",
                            tint = if (isFullscreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onToggleFullscreen
                        )
                        "disconnect" -> ToolbarActionItem(
                            icon = Icons.Default.PowerSettingsNew,
                            name = "断开投屏",
                            detailName = "停止画面镜像并关闭连接",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { controller.stopMirroring() }
                        )
                        "collapse" -> ToolbarActionItem(
                            icon = Icons.Default.KeyboardArrowDown,
                            name = "折叠工具栏",
                            detailName = "收起工具栏为悬浮小球",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            onClick = {
                                isToolbarExpanded = false
                                controller.updateConfig(config.copy(isToolbarExpanded = false))
                            }
                        )
                        "camera_rotate_left" -> if (isCamera) {
                            ToolbarActionItem(
                                icon = Icons.AutoMirrored.Filled.RotateLeft,
                                name = "向左旋转",
                                detailName = "摄像头画面逆时针旋转 90°",
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    cameraRotationDegrees = (cameraRotationDegrees - 90 + 360) % 360
                                }
                            )
                        }
                        "camera_rotate_right" -> if (isCamera) {
                            ToolbarActionItem(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                name = "向右旋转",
                                detailName = "摄像头画面顺时针旋转 90°",
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    cameraRotationDegrees = (cameraRotationDegrees + 90) % 360
                                }
                            )
                        }
                        "camera_flash" -> if (isCamera) {
                            ToolbarActionItem(
                                icon = if (config.cameraTorch) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                name = "闪光灯",
                                detailName = if (config.cameraTorch) "关闭摄像头补光灯" else "打开摄像头补光灯",
                                tint = if (config.cameraTorch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    val newTorch = !config.cameraTorch
                                    controller.updateConfig(config.copy(cameraTorch = newTorch))
                                }
                            )
                        }
                    }
                }

                val effectiveOrder = config.getEffectiveToolbarOrder()
                val visibleButtons = effectiveOrder.filter { actionId ->
                    config.isButtonVisible(actionId) && (!isCamera || TOOLBAR_BUTTON_METAS.find { it.id == actionId }?.isDisplayOnly != true)
                }
                val allButtonsToDisplay = buildList {
                    if (isCamera) {
                        add("camera_rotate_left")
                        add("camera_rotate_right")
                        add("camera_flash")
                    }
                    addAll(visibleButtons)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Drag Grip Handle (Dedicated drag region)
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 2.dp)
                            .size(width = 16.dp, height = if (config.isToolbarDoubleRow) 64.dp else 38.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    overlayOffsetX += dragAmount.x
                                    overlayOffsetY += dragAmount.y
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(if (config.isToolbarDoubleRow) 28.dp else 20.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                RoundedCornerShape(2.dp)
                            )
                        )
                    }

                    if (config.isToolbarDoubleRow) {
                        val numColumns = maxOf(4, (screenWidthDp - 56) / 44)
                        val chunks = allButtonsToDisplay.chunked(numColumns)
                        Column(
                            modifier = Modifier
                                .heightIn(max = 98.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            chunks.forEach { rowIds ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowIds.forEach { actionId ->
                                        RenderToolbarButton(actionId)
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allButtonsToDisplay.forEach { actionId ->
                                RenderToolbarButton(actionId)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isToolbarExpanded,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.7f, animationSpec = tween(280)),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.7f, animationSpec = tween(200))
        ) {
            // Collapsed Floating Mini FAB/Bubble
            Surface(
                onClick = {
                    isToolbarExpanded = true
                    controller.updateConfig(config.copy(isToolbarExpanded = true))
                },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                shape = CircleShape,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .padding(end = 20.dp, bottom = if (isFullscreen) 36.dp else 24.dp)
                    .offset { IntOffset(miniFabOffsetX.roundToInt(), miniFabOffsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            miniFabOffsetX += dragAmount.x
                            miniFabOffsetY += dragAmount.y
                        }
                    }
                    .size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "展开工具栏 (长按可拖动)",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Multi-Touch Visual Feedback Settings Dialog
        if (showMultiTouchDialog) {
            val multiTouchStats by controller.multiTouchController.stats.collectAsState()
            val activePointsMap by controller.multiTouchController.activePoints.collectAsState()

            AlertDialog(
                onDismissRequest = { showMultiTouchDialog = false },
                icon = {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text("多点触控视觉反馈", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Visual overlay toggles
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("视觉反馈选项", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("显示触控光圈与按压深度", fontSize = 13.sp)
                                    androidx.compose.material3.Switch(
                                        checked = multiTouchStats.isOverlayVisible,
                                        onCheckedChange = { controller.multiTouchController.toggleOverlayVisibility(it) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("显示指尖滑行轨迹", fontSize = 13.sp)
                                    androidx.compose.material3.Switch(
                                        checked = multiTouchStats.isTrailsEnabled,
                                        onCheckedChange = { controller.multiTouchController.toggleTrails(it) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("显示实时坐标与指尖连线", fontSize = 13.sp)
                                    androidx.compose.material3.Switch(
                                        checked = multiTouchStats.isCoordinatesVisible,
                                        onCheckedChange = { controller.multiTouchController.toggleCoordinates(it) }
                                    )
                                }
                            }
                        }

                        // Real-time telemetry summary
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("触控发包率", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${multiTouchStats.eventsPerSecond.toInt()} pps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("传输平均延迟", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${multiTouchStats.lastLatencyMs} ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("当前触点", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${activePointsMap.size} 指", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showMultiTouchDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }

        // Remote Hardware/System Volume Control Dialog
        if (showVolumeDialog) {
            RemoteVolumeControlDialog(
                controller = controller,
                onDismiss = { showVolumeDialog = false }
            )
        }

        // Remote Screen Orientation and Adaptive Strategy Dialog
        if (showOrientationDialog) {
            RemoteOrientationControlDialog(
                controller = controller,
                config = config,
                state = state,
                onDismiss = { showOrientationDialog = false }
            )
        }
    }
}

@Composable
fun RemoteOrientationControlDialog(
    controller: ScrcpyController,
    config: ScrcpyConfig,
    state: ScreenState.Streaming,
    onDismiss: () -> Unit
) {
    val isLandscape = state.width > state.height && state.width > 0 && state.height > 0
    val aspect = if (state.width > 0 && state.height > 0) state.width.toFloat() / state.height.toFloat() else 0.5625f
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("屏幕方向与自适应", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "实时匹配被控端横竖屏旋转，消除画面拉伸变形",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Current Remote Screen Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "远程分辨率: ${state.width} × ${state.height}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "画面比例: ${String.format(Locale.getDefault(), "%.2f", aspect)}:1 (${if (isLandscape) "横屏显示" else "竖屏显示"})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLandscape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLandscape) Icons.Default.StayCurrentLandscape else Icons.Default.StayCurrentPortrait,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLandscape) "横屏" else "竖屏",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // 2. Host Screen Adaptive Strategy Section
                Text(
                    text = "主控端自适应策略",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HostOrientationMode.values().forEach { mode ->
                        val isSelected = config.hostOrientationMode == mode
                        Surface(
                            onClick = {
                                controller.updateConfig(config.copy(hostOrientationMode = mode))
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        controller.updateConfig(config.copy(hostOrientationMode = mode))
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = mode.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.shortDesc,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Remote Screen Rotation Control
                Text(
                    text = "被控端物理屏幕旋转",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        controller.rotateRemoteDevice()
                        Toast.makeText(context, "已发送远程屏幕旋转指令", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("一键旋转被控端屏幕 (+90°)")
                }

                // Quick Angle Selector Buttons (0°, 90°, 180°, 270°, Auto Sensor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0 to "0° 竖屏", 1 to "90° 横屏", 2 to "180°", 3 to "270°", -1 to "自动感应").forEach { (rot, label) ->
                        OutlinedButton(
                            onClick = {
                                controller.setRemoteOrientation(rot)
                                Toast.makeText(context, "已切换被控端屏幕方向: $label", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 10.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteVolumeControlDialog(
    controller: ScrcpyController,
    onDismiss: () -> Unit
) {
    var selectedStream by remember { mutableIntStateOf(3) } // 3=Music, 0=Voice, 2=Ring, 4=Alarm
    var currentVolume by remember { mutableIntStateOf(7) }
    var maxVolume by remember { mutableIntStateOf(15) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Query volume whenever stream changes or dialog is launched
    LaunchedEffect(selectedStream) {
        isLoading = true
        controller.fetchRemoteVolume(selectedStream) { cur, max ->
            currentVolume = cur
            maxVolume = if (max > 0) max else 15
            isLoading = false
        }
    }

    val streamList = listOf(
        Triple(3, "媒体音乐", Icons.Default.MusicNote),
        Triple(0, "通话语音", Icons.Default.Phone),
        Triple(2, "铃声通知", Icons.Default.Notifications),
        Triple(4, "闹钟提示", Icons.Default.Alarm)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("远程终端系统音量", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "调节被控端设备的实际硬件输出音量",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2x2 Grid Channel Selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: 媒体音乐 + 通话语音
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(3, "媒体音乐", Icons.Default.MusicNote),
                            Triple(0, "通话语音", Icons.Default.Phone)
                        ).forEach { (streamId, streamTitle, streamIcon) ->
                            val isSelected = selectedStream == streamId
                            Surface(
                                onClick = { selectedStream = streamId },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = streamIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = streamTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: 铃声通知 + 闹钟提示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(2, "铃声通知", Icons.Default.Notifications),
                            Triple(4, "闹钟提示", Icons.Default.Alarm)
                        ).forEach { (streamId, streamTitle, streamIcon) ->
                            val isSelected = selectedStream == streamId
                            Surface(
                                onClick = { selectedStream = streamId },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = streamIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = streamTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Volume Card Display & Slider
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "当前通道音量",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val percent = if (maxVolume > 0) (currentVolume * 100 / maxVolume) else 0
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentVolume == 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "$currentVolume / $maxVolume ($percent%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentVolume == 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Slider
                        Slider(
                            value = currentVolume.toFloat().coerceIn(0f, maxVolume.toFloat()),
                            onValueChange = { newVal ->
                                val intVal = newVal.toInt()
                                currentVolume = intVal
                                controller.setRemoteVolumeValue(selectedStream, intVal)
                            },
                            valueRange = 0f..maxVolume.toFloat(),
                            steps = if (maxVolume > 1) maxVolume - 1 else 0,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Preset chips (0% 静音, 25%, 50%, 75%, 100%) - evenly spaced without wrapping
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0 to "静音", 25 to "25%", 50 to "50%", 75 to "75%", 100 to "100%").forEach { (pct, label) ->
                                val targetVol = if (pct == 0) 0 else ((maxVolume * pct) / 100).coerceIn(1, maxVolume)
                                val isCurrent = (currentVolume == targetVol)
                                Surface(
                                    onClick = {
                                        currentVolume = targetVol
                                        controller.setRemoteVolumeValue(selectedStream, targetVol)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Hardware Key Event Buttons (Vol-, Mute, Vol+) - spacious and well aligned
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                controller.sendVolumeDown()
                                currentVolume = (currentVolume - 1).coerceAtLeast(0)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("音量 -", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                controller.sendVolumeMute()
                                Toast.makeText(context, "已触发远程静音切换", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.VolumeMute, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("静音切换", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        FilledTonalButton(
                            onClick = {
                                controller.sendVolumeUp()
                                currentVolume = (currentVolume + 1).coerceAtMost(maxVolume)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("音量 +", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // Notice / info card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "说明：此调节直接修改对端 Android 设备的系统物理硬件音量，与本地播放投屏音频流转互不影响。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

@Composable
fun ScrcpyConfigSettingsSection(
    config: ScrcpyConfig,
    controller: ScrcpyController,
    onQueryCameras: () -> Unit,
    onQueryCameraSizes: () -> Unit,
    onQueryEncoders: () -> Unit = {},
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
                                    Text(fLabel, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { controller.updateConfig(config.copy(cameraFacing = fKey, cameraId = "")) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Text(fLabel, fontSize = 11.sp, maxLines = 1, softWrap = false)
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
                                    Text(arLabel, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { controller.updateConfig(config.copy(cameraAspectRatio = arKey)) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Text(arLabel, fontSize = 11.sp, maxLines = 1, softWrap = false)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(480, 720, 1080, 0).forEach { res ->
                    val isSelected = config.maxResolution == res
                    val label = if (res == 0) "原生" else "${res}p"
                    if (isSelected) {
                        Button(
                            onClick = { controller.updateConfig(config.copy(maxResolution = res)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { controller.updateConfig(config.copy(maxResolution = res)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false)
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("h264" to "H.264 (默认)", "h265" to "H.265 (HEVC)", "av1" to "AV1").forEach { (codecKey, codecLabel) ->
                val isSelected = config.videoCodec.equals(codecKey, ignoreCase = true)
                if (isSelected) {
                    Button(
                        onClick = { controller.updateConfig(config.copy(videoCodec = codecKey)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(codecLabel, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                } else {
                    OutlinedButton(
                        onClick = { controller.updateConfig(config.copy(videoCodec = codecKey)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(codecLabel, fontSize = 11.sp, maxLines = 1, softWrap = false)
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(15, 30, 60).forEach { fps ->
                val isSelected = config.maxFps == fps
                val label = "${fps} FPS"
                if (isSelected) {
                    Button(
                        onClick = { controller.updateConfig(config.copy(maxFps = fps)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                } else {
                    OutlinedButton(
                        onClick = { controller.updateConfig(config.copy(maxFps = fps)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false)
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(4, 8, 16, 32, 64, 80).forEach { mbps ->
                val bps = mbps * 1000000
                val isSelected = config.bitrate == bps
                if (isSelected) {
                    Button(
                        onClick = { controller.updateConfig(config.copy(bitrate = bps)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("${mbps}M", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                } else {
                    OutlinedButton(
                        onClick = { controller.updateConfig(config.copy(bitrate = bps)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("${mbps}M", fontSize = 11.sp, maxLines = 1, softWrap = false)
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

        // Keep active toggle (--keep-active)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("保持设备活跃 (防休眠)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("投屏或镜头采集期间保持活跃，未插电亦可阻止对端设备休眠 (--keep-active)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = config.isKeepActive,
                onCheckedChange = { isChecked ->
                    controller.updateConfig(config.copy(isKeepActive = isChecked))
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

        Spacer(modifier = Modifier.height(14.dp))

        // Host Screen Orientation Adaptation Strategy
        Text(
            text = "主控端屏幕自适应方向",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "当远端发生横竖屏切换时，主控端画面与屏幕方向的处理方式",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HostOrientationMode.values().forEach { mode ->
                val isSelected = config.hostOrientationMode == mode
                if (isSelected) {
                    Button(
                        onClick = { controller.updateConfig(config.copy(hostOrientationMode = mode)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(mode.label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                } else {
                    OutlinedButton(
                        onClick = { controller.updateConfig(config.copy(hostOrientationMode = mode)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(mode.label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // 底部工具栏排布与按键自定义设置 (Toolbar Layout & Button Ordering)
        // =========================================================================
        var showButtonOrderDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "底部工具栏排布与按键自定义",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "支持切换单排横滑或双排紧凑显示，支持调整功能按钮先后顺序，以及隐藏不需要的功能项（自动记住设置）。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. 排布模式选择 (单排横向 vs 双排上下滑动)
                Text(
                    text = "工具栏排布样式",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 单排横滑
                    val isSingle = !config.isToolbarDoubleRow
                    Surface(
                        onClick = { controller.updateConfig(config.copy(isToolbarDoubleRow = false)) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSingle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSingle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewStream,
                                contentDescription = null,
                                tint = if (isSingle) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "单排横滑",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSingle) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "左右滚动浏览",
                                    fontSize = 10.sp,
                                    color = if (isSingle) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 双排上下滑动
                    val isDouble = config.isToolbarDoubleRow
                    Surface(
                        onClick = { controller.updateConfig(config.copy(isToolbarDoubleRow = true)) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDouble) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isDouble) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (isDouble) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "双排显示",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDouble) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "上下滑动出更多",
                                    fontSize = 10.sp,
                                    color = if (isDouble) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 按键排序与显示/隐藏入口
                val currentOrder = config.getEffectiveToolbarOrder()
                val visibleCount = currentOrder.count { config.isButtonVisible(it) }
                val hiddenCount = currentOrder.size - visibleCount
                val isCustomized = config.toolbarButtonOrder.isNotEmpty() || config.toolbarHiddenButtons.isNotEmpty()

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "功能按键排序与可见性",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "共 ${currentOrder.size} 个按键 (已显示 $visibleCount 个, 隐藏 $hiddenCount 个)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { showButtonOrderDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("自定义排序", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (showButtonOrderDialog) {
                    ToolbarButtonSortDialog(
                        config = config,
                        onUpdateConfig = { newCfg ->
                            controller.updateConfig(newCfg)
                        },
                        onDismiss = { showButtonOrderDialog = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // 实验性设置 (Experimental Settings) - 放在最后面并标记提示非必要请勿随意修改
        // =========================================================================
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "实验性设置",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "实验性设置 (非必要请勿随意修改)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "⚠️ 以下参数为高级底层实验性项。设备默认配置通常最优，非必要请勿随意修改！修改编码器或过大缓冲可能导致投屏画面黑屏、崩溃或增加操作延迟。",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // -------------------------------------------------------------
                // 1. 视频编码器选择 (scrcpy --video-encoder)
                // -------------------------------------------------------------
                Text(
                    text = "1. 视频编码器 (scrcpy --video-encoder)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "设备上可能存在多个编码器。默认编码器异常崩溃或产生画面异常时，可通过以下按钮查询并切换其他硬件或软件编码器。",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display current encoder status
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "当前选用编码器",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (config.videoEncoder.isBlank()) "系统默认 (由 scrcpy 自动决策)" else config.videoEncoder,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = if (config.videoEncoder.isBlank()) FontFamily.Default else FontFamily.Monospace,
                                color = if (config.videoEncoder.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (config.videoEncoder.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    controller.updateConfig(config.copy(videoEncoder = ""))
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("恢复默认", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Query and Choose Encoders button
                OutlinedButton(
                    onClick = onQueryEncoders,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("查询并选择编码器 (--list-encoders)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(14.dp))

                // -------------------------------------------------------------
                // 2. 视频缓冲 (scrcpy --video-buffer)
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2. 视频缓冲 (scrcpy --video-buffer)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "默认不对视频进行缓冲以获取超低延迟。若网络不稳定或画面偶有卡顿抖动，可开启缓冲以延迟画面并补偿抖动，获得更平滑的播放体验。",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = config.isVideoBufferEnabled,
                        onCheckedChange = { isChecked ->
                            controller.updateConfig(config.copy(isVideoBufferEnabled = isChecked))
                        }
                    )
                }

                if (config.isVideoBufferEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "缓冲时长滑杆调节 (10ms ~ 200ms)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${config.videoBufferMs} ms",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Slider(
                                value = config.videoBufferMs.toFloat().coerceIn(10f, 200f),
                                onValueChange = { newValue ->
                                    val rounded = ((newValue / 5).toInt() * 5).coerceIn(10, 200)
                                    controller.updateConfig(config.copy(videoBufferMs = rounded))
                                },
                                valueRange = 10f..200f,
                                steps = 37
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("10ms (极低延迟)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("50ms (推荐)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text("200ms (强抗抖动)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick preset chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(10, 50, 120).forEach { ms ->
                                    val isSel = config.videoBufferMs == ms
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { controller.updateConfig(config.copy(videoBufferMs = ms)) },
                                        label = { Text("${ms}ms", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EncoderSelectionDialog(
    currentEncoder: String,
    isQuerying: Boolean,
    encoders: List<ScrcpyController.ScrcpyEncoderInfo>,
    rawOutput: String,
    onSelectEncoder: (ScrcpyController.ScrcpyEncoderInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showRawText by remember { mutableStateOf(false) }
    var customEncoderInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "选择视频编码器 (--list-encoders)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                Text(
                    text = "默认编码器有时会出现异常崩溃。优先选择带有【硬件加速】标签的编码器，以获得最高性能与低发热。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (isQuerying) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("正在通过 ADB 查询被控端编码器...", fontSize = 13.sp)
                    }
                } else if (showRawText) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        Text(
                            text = rawOutput.ifBlank { "未查询到原始输出" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. System Default Option
                        item {
                            val isDefaultSelected = currentEncoder.isBlank()
                            Surface(
                                onClick = { onSelectEncoder(null) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDefaultSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isDefaultSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isDefaultSelected,
                                        onClick = { onSelectEncoder(null) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "系统默认 (推荐)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "AUTO",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "由 scrcpy 根据设备型号与 Codec 自动选定最佳编码器",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Detected Encoders
                        val videoEncoders = encoders.filter { it.isVideo }
                        val displayEncoders = if (videoEncoders.isNotEmpty()) videoEncoders else encoders
                        items(displayEncoders) { enc ->
                            val isSelected = currentEncoder.equals(enc.name, ignoreCase = true)
                            Surface(
                                onClick = { onSelectEncoder(enc) },
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
                                        onClick = { onSelectEncoder(enc) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = enc.codec.uppercase(),
                                                    color = MaterialTheme.colorScheme.onSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Surface(
                                                color = if (enc.isHardware) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (enc.isHardware) "硬件加速" else "软件解码",
                                                    color = if (enc.isHardware) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = enc.name,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Manual Input Item
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("手动指定编码器名称:", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customEncoderInput,
                                            onValueChange = { customEncoderInput = it.trim() },
                                            placeholder = { Text("例如 OMX.qcom.video.encoder.avc", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                if (customEncoderInput.isNotBlank()) {
                                                    val dummyCodec = when {
                                                        customEncoderInput.contains("hevc", ignoreCase = true) || customEncoderInput.contains("h265", ignoreCase = true) -> "h265"
                                                        customEncoderInput.contains("av01", ignoreCase = true) || customEncoderInput.contains("av1", ignoreCase = true) -> "av1"
                                                        else -> "h264"
                                                    }
                                                    onSelectEncoder(ScrcpyController.ScrcpyEncoderInfo(dummyCodec, customEncoderInput, isHardware = true, isVideo = true))
                                                }
                                            },
                                            enabled = customEncoderInput.isNotBlank(),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("应用", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isQuerying && rawOutput.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
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
                if (!isQuerying && rawOutput.isNotBlank()) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("scrcpy-encoders", rawOutput)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制")
                    }
                }
                Button(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )
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
    var versionDropdownExpanded by remember { mutableStateOf(false) }
    var versionDialogVisible by remember { mutableStateOf(false) }

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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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

                    // Target Version Selector Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "下载目标版本 (支持任意特定版本):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = { versionDialogVisible = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("版本列表 (${serverState.availableVersions.size})", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { versionDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Sell,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val targetVer = serverState.selectedTargetVersion ?: serverState.latestVersion ?: if (serverState.availableVersions.isNotEmpty()) serverState.availableVersions.first().version else "未选择 (需拉取)"
                                    Text(
                                        text = "版本: $targetVer",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (targetVer == serverState.latestVersion && serverState.latestVersion != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "最新",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (targetVer == serverState.localVersion && serverState.isReady) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "当前已就绪",
                                                fontSize = 10.sp,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = versionDropdownExpanded,
                            onDismissRequest = { versionDropdownExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            serverState.availableVersions.forEach { verInfo ->
                                val isSelected = verInfo.version == serverState.selectedTargetVersion
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = verInfo.version,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (verInfo.isLatest) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            "最新",
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                if (verInfo.version == serverState.localVersion && serverState.isReady) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            "当前使用",
                                                            fontSize = 9.sp,
                                                            color = Color(0xFF2E7D32),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            val details = listOfNotNull(
                                                verInfo.note.ifEmpty { null },
                                                verInfo.publishedAt.ifEmpty { null },
                                                verInfo.sizeFormatted.ifEmpty { null }
                                            ).joinToString(" · ")
                                            if (details.isNotEmpty()) {
                                                Text(
                                                    text = details,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        ScrcpyServerManager.selectTargetVersion(context, verInfo.version)
                                        versionDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("查看完整版本列表 / 下载指定版本...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    versionDropdownExpanded = false
                                    versionDialogVisible = true
                                }
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
                                    text = "下载中: ${(serverState.downloadProgress * 100).toInt()}% (${serverState.downloadingVersion ?: serverState.selectedTargetVersion ?: ""})",
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
                                val dVer = serverState.downloadingVersion ?: serverState.selectedTargetVersion ?: ""
                                Text("下载中 $dVer...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val targetVer = serverState.selectedTargetVersion ?: serverState.latestVersion ?: "最新"
                                val isInstalledThis = serverState.isReady && serverState.localVersion == targetVer
                                Text(
                                    text = if (!serverState.isReady) "下载 $targetVer"
                                           else if (isInstalledThis) "重新下载 $targetVer"
                                           else "下载并切换到 $targetVer",
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

    if (versionDialogVisible) {
        ScrcpyVersionSelectionDialog(
            serverState = serverState,
            onDismiss = { versionDialogVisible = false },
            onSelectVersion = { ver ->
                ScrcpyServerManager.selectTargetVersion(context, ver)
            },
            onDownloadVersion = { ver, url ->
                coroutineScope.launch {
                    ScrcpyServerManager.downloadServer(context, ver, url)
                }
            },
            onCheckUpdate = {
                coroutineScope.launch {
                    ScrcpyServerManager.checkUpdate(context)
                }
            }
        )
    }
}

@Composable
fun ScrcpyVersionSelectionDialog(
    serverState: ScrcpyServerState,
    onDismiss: () -> Unit,
    onSelectVersion: (String) -> Unit,
    onDownloadVersion: (String, String?) -> Unit,
    onCheckUpdate: () -> Unit
) {
    var customTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCheckUpdate,
                enabled = !serverState.isCheckingUpdate && !serverState.isDownloading
            ) {
                if (serverState.isCheckingUpdate) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("检查更新/刷新", fontSize = 12.sp)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FormatListNumbered,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "选择 scrcpy-server 版本",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                Text(
                    text = "从 GitHub Releases 网络动态拉取 >= 4.1 的版本列表。点击版本项可设为目标版本，或点击右侧按钮直接下载。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Custom version input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        placeholder = { Text("手动输入版本号 (如 v4.1)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val tag = customTag.trim()
                            if (tag.isNotEmpty()) {
                                onSelectVersion(if (tag.startsWith("v") || tag.startsWith("V")) tag else "v$tag")
                                customTag = ""
                            }
                        },
                        enabled = customTag.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("指定", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (serverState.availableVersions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无已拉取的 >= 4.1 版本",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "请点击下方「检查更新/刷新」从 GitHub 网络拉取",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(serverState.availableVersions) { verInfo ->
                        val isSelected = verInfo.version == serverState.selectedTargetVersion
                        val isLocalInstalled = verInfo.version == serverState.localVersion && serverState.isReady
                        val isCurrentlyDownloading = serverState.isDownloading && serverState.downloadingVersion == verInfo.version

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVersion(verInfo.version) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                     else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = verInfo.version,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (verInfo.isLatest) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "最新",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (isLocalInstalled) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFF2E7D32),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "当前已装",
                                                    fontSize = 9.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    val desc = listOfNotNull(
                                        verInfo.releaseName.ifEmpty { null },
                                        verInfo.note.ifEmpty { null },
                                        verInfo.publishedAt.ifEmpty { null },
                                        verInfo.sizeFormatted.ifEmpty { null }
                                    ).distinct().joinToString(" · ")
                                    if (desc.isNotEmpty()) {
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        onSelectVersion(verInfo.version)
                                        onDownloadVersion(verInfo.version, verInfo.downloadUrl)
                                    },
                                    enabled = !serverState.isDownloading,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isCurrentlyDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("下载中", fontSize = 11.sp)
                                    } else {
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isLocalInstalled) "重下" else "下载",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
)
}

fun getToolbarButtonIcon(actionId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (actionId) {
        "back" -> Icons.Default.ArrowBack
        "home" -> Icons.Default.Home
        "recents" -> Icons.Default.RecentActors
        "wake_up" -> Icons.Default.LockOpen
        "orientation" -> Icons.Default.ScreenRotation
        "multi_touch" -> Icons.Default.TouchApp
        "keep_active", "stay_awake" -> Icons.Default.WbSunny
        "text_input" -> Icons.Default.Keyboard
        "screen_off" -> Icons.Default.ScreenLockPortrait
        "volume_down" -> Icons.Default.VolumeDown
        "volume_up" -> Icons.Default.VolumeUp
        "volume_panel" -> Icons.Default.GraphicEq
        "audio_stream" -> Icons.Default.Headphones
        "virtual_display" -> Icons.Default.Apps
        "logs" -> Icons.Default.Terminal
        "settings" -> Icons.Default.Settings
        "fullscreen" -> Icons.Default.Fullscreen
        "disconnect" -> Icons.Default.PowerSettingsNew
        "collapse" -> Icons.Default.KeyboardArrowDown
        else -> Icons.Default.Tune
    }
}

@Composable
fun ToolbarButtonSortDialog(
    config: ScrcpyConfig,
    onUpdateConfig: (ScrcpyConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var buttonOrderList by remember { mutableStateOf(config.getEffectiveToolbarOrder()) }
    var hiddenButtonsSet by remember { mutableStateOf(config.toolbarHiddenButtons) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "底部工具栏按键排序与显示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "点击 ▲▼ 调整顺序，置顶可移至首位，取消勾选隐藏",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
            ) {
                // 顶部统计与快捷操作栏 (全部显示 / 恢复默认)
                val visibleCount = buttonOrderList.count { it !in hiddenButtonsSet }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已启用 $visibleCount / ${buttonOrderList.size} 个按键",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hiddenButtonsSet.isNotEmpty()) {
                                TextButton(
                                    onClick = { hiddenButtonsSet = emptySet() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("全部显示", fontSize = 11.sp)
                                }
                            }
                            TextButton(
                                onClick = {
                                    buttonOrderList = DEFAULT_TOOLBAR_BUTTON_IDS
                                    hiddenButtonsSet = emptySet()
                                    Toast.makeText(context, "已重置为默认按键排序与全显示", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("重置默认", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 按键列表
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(buttonOrderList, key = { _, id -> id }) { index, actionId ->
                        val meta = TOOLBAR_BUTTON_METAS.find { it.id == actionId }
                        val name = meta?.name ?: actionId
                        val detail = meta?.detail ?: ""
                        val isVisible = actionId !in hiddenButtonsSet
                        val icon = getToolbarButtonIcon(actionId)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isVisible) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isVisible) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 序号标签
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.width(20.dp)
                                )

                                // 图标圆底
                                Surface(
                                    shape = CircleShape,
                                    color = if (isVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = name,
                                            tint = if (isVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // 名称与描述
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        if (!isVisible) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "已隐藏",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = detail,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isVisible) 0.8f else 0.5f)
                                    )
                                }

                                // 置顶按钮
                                Surface(
                                    onClick = {
                                        if (index > 0) {
                                            val mutable = buttonOrderList.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(0, item)
                                            buttonOrderList = mutable
                                        }
                                    },
                                    enabled = index > 0,
                                    color = if (index > 0) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "置顶",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index > 0) MaterialTheme.colorScheme.onSecondaryContainer else Color.Transparent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(2.dp))

                                // 上移按钮
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val mutable = buttonOrderList.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index - 1]
                                            mutable[index - 1] = temp
                                            buttonOrderList = mutable
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "上移",
                                        tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // 下移按钮
                                IconButton(
                                    onClick = {
                                        if (index < buttonOrderList.size - 1) {
                                            val mutable = buttonOrderList.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index + 1]
                                            mutable[index + 1] = temp
                                            buttonOrderList = mutable
                                        }
                                    },
                                    enabled = index < buttonOrderList.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "下移",
                                        tint = if (index < buttonOrderList.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(2.dp))

                                // 显示/隐藏开关 Checkbox
                                Checkbox(
                                    checked = isVisible,
                                    onCheckedChange = { checked ->
                                        hiddenButtonsSet = if (checked) {
                                            hiddenButtonsSet - actionId
                                        } else {
                                            hiddenButtonsSet + actionId
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedCfg = config.copy(
                        toolbarButtonOrder = buttonOrderList.joinToString(","),
                        toolbarHiddenButtons = hiddenButtonsSet
                    )
                    onUpdateConfig(updatedCfg)
                    Toast.makeText(context, "工具栏按键设置已保存", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("保存设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

