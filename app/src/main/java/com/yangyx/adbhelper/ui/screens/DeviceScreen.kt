package com.yangyx.adbhelper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BatteryChargingFull
import com.yangyx.adbhelper.adb.AdbForwardRule
import com.yangyx.adbhelper.adb.AdbReverseRule
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material.icons.filled.MediaBluetoothOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.text.font.FontFamily
import android.graphics.BitmapFactory
import com.yangyx.adbhelper.ui.models.ScreenRecordState
import com.yangyx.adbhelper.ui.models.RemoteScreenshotResult
import com.yangyx.adbhelper.ui.models.RemoteScreenRecordResult
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.models.RebootOption
import com.yangyx.adbhelper.ui.models.TouchpadMode
import com.yangyx.adbhelper.ui.models.TouchpadProtocol
import com.yangyx.adbhelper.ui.models.BrightnessConverter
import kotlin.math.roundToInt
import com.yangyx.adbhelper.input.VirtualMouseStatus
import com.yangyx.adbhelper.input.DriverType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier,
    isGlassMode: Boolean = false
) {
    val selectedTabIndex by viewModel.deviceSelectedTabIndex.collectAsState()
    val tabs = listOf(
        Triple("信息", Icons.Default.Analytics, 0),
        Triple("控制", Icons.Default.Tune, 1),
        Triple("输入", Icons.Default.Keyboard, 2)
    )

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top Tab Selector with clean Material 3 styling
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { (title, icon, index) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.setDeviceSelectedTabIndex(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }

        // Tab Content Pages
        when (selectedTabIndex) {
            0 -> DeviceSystemInfoTab(viewModel = viewModel, isGlassMode = isGlassMode)
            1 -> DeviceControlTab(viewModel = viewModel, isGlassMode = isGlassMode)
            2 -> DeviceInputTab(viewModel = viewModel, isGlassMode = isGlassMode)
        }
    }
}

/**
 * 选项卡 1：设备信息 (保留现有全部硬件、系统、内存、电池、存储信息，移除底栏外观选择)
 */
@Composable
private fun DeviceSystemInfoTab(
    viewModel: AdbViewModel,
    isGlassMode: Boolean
) {
    val info by viewModel.systemInfo.collectAsState()
    val isRefreshing by viewModel.isRefreshingSystemInfo.collectAsState()
    val listState by viewModel.deviceInfoListState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = if (isGlassMode) 100.dp else 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "info_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "远端设备基础与性能指标",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { viewModel.refreshSystemInfo() },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRefreshing) "刷新中..." else "刷新状态")
                }
            }
        }

        // 硬件与系统 (Hardware & OS)
        item(key = "hardware_os") {
            InfoCard(
                title = "硬件与系统 (Hardware & OS)",
                icon = Icons.Default.PhoneAndroid,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                if (!info.marketName.isNullOrBlank()) {
                    InfoRow("设备通用名 / 市场名", info.marketName!!)
                }
                if (!info.serialNo.isNullOrBlank()) {
                    InfoRow("设备唯一序列号 (Serial)", info.serialNo!!)
                }
                if (info.model.isNotBlank() && info.model != "Unknown") {
                    val brandPrefix = if (info.manufacturer.isNotBlank() && info.manufacturer != "Unknown" && !info.model.startsWith(info.manufacturer, ignoreCase = true)) {
                        "${info.manufacturer} "
                    } else ""
                    InfoRow("设备型号", "$brandPrefix${info.model}")
                } else if (info.manufacturer.isNotBlank() && info.manufacturer != "Unknown") {
                    InfoRow("制造厂商", info.manufacturer)
                }
                if (!info.osVersion.isNullOrBlank()) {
                    InfoRow("系统固件版本 (OS)", info.osVersion!!)
                }
                if (info.androidVersion.isNotBlank() && info.androidVersion != "Unknown") {
                    val sdkStr = if (info.sdkVersion > 0) " (API ${info.sdkVersion})" else ""
                    InfoRow("Android 版本", "Android ${info.androidVersion}$sdkStr")
                }
                if (!info.cpuHardware.isNullOrBlank()) {
                    InfoRow("CPU 厂商代号", info.cpuHardware!!)
                }
                if (!info.socModel.isNullOrBlank()) {
                    InfoRow("SoC 芯片型号", info.socModel!!)
                }
                if (info.cpuArchitecture.isNotBlank() && info.cpuArchitecture != "Unknown") {
                    InfoRow("CPU 指令集架构", info.cpuArchitecture)
                }
            }
        }

        // 内存与 CPU (RAM & CPU)
        item(key = "ram_cpu") {
            InfoCard(
                title = "内存与 CPU (RAM & CPU)",
                icon = Icons.Default.Memory,
                iconTint = Color(0xFF0284C7)
            ) {
                if (info.ramTotalMb > 0) {
                    val ramProgress = (info.ramUsedMb.toFloat() / info.ramTotalMb).coerceIn(0f, 1f)
                    Text(
                        text = "RAM 内存使用: ${info.ramUsedMb} MB / ${info.ramTotalMb} MB (${(ramProgress * 100).toInt()}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ramProgress },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "CPU 占用率: ${info.cpuUsage}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (info.cpuUsage / 100f).coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
        }

        // 电池 (Battery)
        if (info.batteryLevel > 0 || (info.batteryStatus.isNotBlank() && info.batteryStatus != "Unknown")) {
            item(key = "battery") {
                InfoCard(
                    title = "电池健康与电量 (Battery)",
                    icon = Icons.Default.BatteryChargingFull,
                    iconTint = Color(0xFF16A34A)
                ) {
                    if (info.batteryLevel > 0) {
                        InfoRow("电量百分比", "${info.batteryLevel}%")
                    }
                    if (info.batteryTemperature > 0f) {
                        InfoRow("电池温度", "${info.batteryTemperature} °C")
                    }
                    if (info.batteryStatus.isNotBlank() && info.batteryStatus != "Unknown") {
                        InfoRow("供电状态", info.batteryStatus)
                    }
                }
            }
        }

        // 存储空间 (Storage)
        if (info.storageTotalGb > 0f) {
            item(key = "storage") {
                InfoCard(
                    title = "内部存储空间 (Storage)",
                    icon = Icons.Default.SdStorage,
                    iconTint = Color(0xFFD97706)
                ) {
                    val storageProgress = (info.storageUsedGb / info.storageTotalGb).coerceIn(0f, 1f)
                    val freeGb = (info.storageTotalGb - info.storageUsedGb).coerceAtLeast(0f)
                    val usedGbFormatted = String.format(java.util.Locale.US, "%.1f", info.storageUsedGb)
                    val totalGbFormatted = String.format(java.util.Locale.US, "%.1f", info.storageTotalGb)
                    val freeGbFormatted = String.format(java.util.Locale.US, "%.1f", freeGb)
                    Text(
                        text = "已用空间: $usedGbFormatted GB / $totalGbFormatted GB (${(storageProgress * 100).toInt()}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { storageProgress },
                        color = Color(0xFFD97706),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("剩余可用", "$freeGbFormatted GB")
                }
            }
        }
    }
}

/**
 * 选项卡 2：设备控制 (包含现在的电源和重启管理，并增加设备 DPI 控制)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceControlTab(
    viewModel: AdbViewModel,
    isGlassMode: Boolean
) {
    val densityInfo by viewModel.deviceDensity.collectAsState()
    val quickSettings by viewModel.deviceQuickSettings.collectAsState()
    val mediaInfo by viewModel.mediaPlaybackInfo.collectAsState()
    val context = LocalContext.current
    val isCapturingScreenshot by viewModel.isCapturingScreenshot.collectAsState()
    val pendingScreenshotDialog by viewModel.pendingScreenshotDialog.collectAsState()
    val screenRecordState by viewModel.screenRecordState.collectAsState()
    val screenRecordDuration by viewModel.screenRecordDurationSeconds.collectAsState()
    val pendingScreenRecordDialog by viewModel.pendingScreenRecordDialog.collectAsState()
    val isDownloadingScreenRecord by viewModel.isDownloadingScreenRecord.collectAsState()
    var pendingRebootOption by remember { mutableStateOf<RebootOption?>(null) }
    var dpiInputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val forwardRules by viewModel.forwardRules.collectAsState()
    val reverseRules by viewModel.reverseRules.collectAsState()
    var showForwardReverseHelpDialog by remember { mutableStateOf(false) }
    var showAddForwardDialog by remember { mutableStateOf(false) }
    var showAddReverseDialog by remember { mutableStateOf(false) }

    // 初始化时获取最新的 DPI、分辨率、控制中心开关状态、媒体信息与反向代理列表
    LaunchedEffect(Unit) {
        viewModel.fetchDeviceDensity()
        viewModel.fetchQuickSettings()
        viewModel.fetchMediaPlaybackInfo()
        viewModel.fetchReverseRules()
    }

    // 当获取到当前 DPI 时，若输入框为空则填充当前值
    LaunchedEffect(densityInfo.currentDpi) {
        if (densityInfo.currentDpi > 0 && dpiInputText.isBlank()) {
            dpiInputText = densityInfo.currentDpi.toString()
        }
    }

    val listState by viewModel.deviceControlListState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = if (isGlassMode) 100.dp else 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "control_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设备参数与电源控制",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        viewModel.fetchDeviceDensity()
                        viewModel.fetchQuickSettings()
                        viewModel.fetchMediaPlaybackInfo()
                    },
                    enabled = !densityInfo.isLoading && !quickSettings.isLoading && !mediaInfo.isLoading
                ) {
                    if (densityInfo.isLoading || quickSettings.isLoading || mediaInfo.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新控制状态")
                    }
                }
            }
        }

        // 1. 快捷控制中心 Card (模拟真实手机下拉控制栏大药丸滑块与磁贴)
        item(key = "quick_settings_control") {
            InfoCard(
                title = "系统快捷控制中心 (Control Center)",
                icon = Icons.Default.Tune,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "实时读取被控端系统设置状态。点击磁贴一键开关，拖动滑块实时调节屏幕亮度。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.fetchQuickSettings() },
                        enabled = !quickSettings.isLoading,
                        modifier = Modifier.size(30.dp)
                    ) {
                        if (quickSettings.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新控制状态",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 亮度控制滑块胶囊 (真实手机下拉状态栏大药丸滑块风格)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        val currentPerceptualPercent = remember(quickSettings.brightness) {
                            BrightnessConverter.rawBrightnessToPercent(quickSettings.brightness)
                        }

                        var sliderPercent by remember(quickSettings.brightness) {
                            mutableFloatStateOf(currentPerceptualPercent.toFloat())
                        }
                        var isDragging by remember { mutableStateOf(false) }

                        val displayPercent = if (isDragging) sliderPercent.roundToInt() else currentPerceptualPercent
                        val displayRaw = if (isDragging) BrightnessConverter.percentToRawBrightness(sliderPercent) else quickSettings.brightness

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (displayPercent > 70) Icons.Default.BrightnessHigh
                                        else if (displayPercent > 25) Icons.Default.BrightnessMedium
                                        else Icons.Default.BrightnessLow,
                                    contentDescription = "亮度",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "屏幕亮度",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$displayPercent% ($displayRaw)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 自动亮度切换 Pill
                            FilterChip(
                                selected = quickSettings.isAutoBrightnessOn,
                                onClick = { viewModel.toggleAutoBrightness() },
                                label = {
                                    Text(
                                        text = if (quickSettings.isAutoBrightnessOn) "自动: 开" else "自动: 关",
                                        fontSize = 11.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.BrightnessAuto,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = sliderPercent,
                            onValueChange = {
                                isDragging = true
                                sliderPercent = it
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                val targetRaw = BrightnessConverter.percentToRawBrightness(sliderPercent)
                                viewModel.setScreenBrightness(targetRaw)
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )

                        // 快捷百分比挡位微调按钮 (0%, 25%, 50%, 75%, 100%)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0, 25, 50, 75, 100).forEach { preset ->
                                val isSelected = displayPercent == preset
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable {
                                        sliderPercent = preset.toFloat()
                                        val targetRaw = BrightnessConverter.percentToRawBrightness(preset.toFloat())
                                        viewModel.setScreenBrightness(targetRaw)
                                    }
                                ) {
                                    Text(
                                        text = "$preset%",
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 磁贴网格 (Quick Settings Tiles - 2列磁贴布局，复刻真实系统下拉通知控制栏)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: 蓝牙 & Wi-Fi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickTile(
                            title = "蓝牙",
                            subtitle = if (quickSettings.isBluetoothOn) "已开启" else "已关闭",
                            icon = if (quickSettings.isBluetoothOn) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                            isActive = quickSettings.isBluetoothOn,
                            onClick = { viewModel.toggleBluetooth() },
                            modifier = Modifier.weight(1f)
                        )
                        QuickTile(
                            title = "Wi-Fi",
                            subtitle = if (quickSettings.isWifiOn) "已开启" else "已关闭",
                            icon = if (quickSettings.isWifiOn) Icons.Default.Wifi else Icons.Default.WifiOff,
                            isActive = quickSettings.isWifiOn,
                            onClick = { viewModel.toggleWifi() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: 静音模式 & 勿扰模式
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickTile(
                            title = "静音模式",
                            subtitle = if (quickSettings.isMuteOn) "静音/振动" else "正常响铃",
                            icon = if (quickSettings.isMuteOn) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            isActive = quickSettings.isMuteOn,
                            onClick = { viewModel.toggleMute() },
                            modifier = Modifier.weight(1f)
                        )
                        QuickTile(
                            title = "勿扰模式",
                            subtitle = if (quickSettings.isZenModeOn) "已开启勿扰" else "已关闭",
                            icon = if (quickSettings.isZenModeOn) Icons.Default.DoNotDisturb else Icons.Default.NotificationsNone,
                            isActive = quickSettings.isZenModeOn,
                            onClick = { viewModel.toggleZenMode() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: 定位服务 & NFC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickTile(
                            title = "定位服务",
                            subtitle = if (quickSettings.isLocationOn) "GPS 开启" else "已关闭",
                            icon = if (quickSettings.isLocationOn) Icons.Default.LocationOn else Icons.Default.LocationOff,
                            isActive = quickSettings.isLocationOn,
                            onClick = { viewModel.toggleLocation() },
                            modifier = Modifier.weight(1f)
                        )
                        QuickTile(
                            title = "NFC",
                            subtitle = if (!quickSettings.hasNfc) "未搭载/不支持" else if (quickSettings.isNfcOn) "已开启" else "已关闭",
                            icon = Icons.Default.Nfc,
                            isActive = quickSettings.isNfcOn,
                            enabled = quickSettings.hasNfc,
                            onClick = { viewModel.toggleNfc() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 4: 自动旋转 & 移动数据
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickTile(
                            title = "自动旋转",
                            subtitle = if (quickSettings.isAutoRotateOn) "旋转已开启" else "锁定竖屏",
                            icon = if (quickSettings.isAutoRotateOn) Icons.Default.ScreenRotation else Icons.Default.ScreenLockRotation,
                            isActive = quickSettings.isAutoRotateOn,
                            onClick = { viewModel.toggleAutoRotate() },
                            modifier = Modifier.weight(1f)
                        )
                        QuickTile(
                            title = "移动数据",
                            subtitle = if (quickSettings.isMobileDataOn) "已开启" else "已关闭",
                            icon = if (quickSettings.isMobileDataOn) Icons.Default.SignalCellularAlt else Icons.Default.SignalCellularConnectedNoInternet0Bar,
                            isActive = quickSettings.isMobileDataOn,
                            onClick = { viewModel.toggleMobileData() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 1.2. 远程截图与录屏 Card (Remote Capture & Screen Recording)
        item(key = "screen_capture_record") {
            InfoCard(
                title = "远程截图与录屏 (Capture & Recording)",
                icon = Icons.Default.PhotoCamera,
                iconTint = Color(0xFFF43F5E)
            ) {
                Text(
                    text = "一键对被控端屏幕进行远程实时截图或高清录屏，完成后可即时预览并一键下载保存到本地相册与视频库。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 截图按钮
                    Button(
                        onClick = { viewModel.takeRemoteScreenshot(context) },
                        enabled = !isCapturingScreenshot && screenRecordState != ScreenRecordState.RECORDING,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (isCapturingScreenshot) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("截图中...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("远程截图", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 录屏按钮 / 录屏控制器
                    if (screenRecordState == ScreenRecordState.IDLE) {
                        Button(
                            onClick = { viewModel.startRemoteScreenRecord() },
                            enabled = !isCapturingScreenshot,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("开始录屏", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (screenRecordState == ScreenRecordState.STOPPING) {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在结束...", fontSize = 12.sp)
                        }
                    } else {
                        // 录制中时右侧显示录制时长药丸
                        val minutes = screenRecordDuration / 60
                        val seconds = screenRecordDuration % 60
                        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE11D48).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFE11D48)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "rec_pulse"
                                )
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48).copy(alpha = pulseAlpha),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "录制中 $timeStr",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE11D48)
                                )
                            }
                        }
                    }
                }

                // 录制中状态操作面板 (当 screenRecordState == RECORDING 时展开)
                if (screenRecordState == ScreenRecordState.RECORDING) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Recording",
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "被控端正在高清录制屏幕",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.stopRemoteScreenRecord(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("结束录制", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.cancelRemoteScreenRecord() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("取消", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1.5. 当前媒体播放与控制 Card
        item(key = "media_playback_control") {
            InfoCard(
                title = "当前媒体播放 (Now Playing)",
                icon = Icons.Default.GraphicEq,
                iconTint = Color(0xFF06B6D4)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "实时获取对端正在播放的音频、音乐与视频信息，并支持远程遥控播放与切歌。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.fetchMediaPlaybackInfo() },
                        enabled = !mediaInfo.isLoading,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (mediaInfo.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "刷新媒体状态",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (mediaInfo.hasActiveSession || mediaInfo.title.isNotBlank()) {
                    // 正在播放或有活跃媒体会话
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = if (mediaInfo.isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                            contentDescription = "Media Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = mediaInfo.displayTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (mediaInfo.displayArtist.isNotBlank()) {
                                        Text(
                                            text = mediaInfo.displayArtist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (mediaInfo.appName.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = mediaInfo.appName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = if (mediaInfo.isPlaying) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (mediaInfo.isPlaying) "▶ 正在播放" else "⏸ ${mediaInfo.stateText}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (mediaInfo.isPlaying) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (mediaInfo.durationMs > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { mediaInfo.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = mediaInfo.positionText,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = mediaInfo.durationText,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = { viewModel.mediaPrevious() },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        contentDescription = "上一曲",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = { viewModel.mediaPlayPause() },
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    Icon(
                                        imageVector = if (mediaInfo.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "播放/暂停",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = { viewModel.mediaNext() },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = "下一曲",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "暂无检测到活跃的媒体播放",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "支持读取网易云、QQ音乐、酷狗、哔哩哔哩、YouTube、Spotify等任意对端播放中的媒体信息。",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.fetchMediaPlaybackInfo() },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("刷新检测", fontSize = 12.sp)
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.mediaPlayPause() },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("唤醒播放", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. 设备 DPI 与显示控制 Card
        item(key = "dpi_control") {
            InfoCard(
                title = "屏幕 DPI 与分辨率控制 (wm density)",
                icon = Icons.Default.AspectRatio,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "通过底层 wm density 指令动态修改被控端屏幕像素密度 (DPI)，实现界面元素整体放大或缩小。支持即时生效与一键恢复默认物理值。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // DPI 当前状态展示区
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("当前生效 DPI", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (densityInfo.currentDpi > 0) "${densityInfo.currentDpi} dpi" else "读取中...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (densityInfo.overrideDpi != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                                if (densityInfo.overrideDpi != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "已自定义",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("物理默认 DPI", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (densityInfo.physicalDpi > 0) "${densityInfo.physicalDpi} dpi" else "读取中...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (densityInfo.physicalSize.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("屏幕物理分辨率", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = densityInfo.physicalSize,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 常用预设快捷 Chip 组
                Text("常用预设值:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                val presetList = listOf(320, 360, 400, 420, 440, 480, 560)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetList.forEach { presetDpi ->
                        val isSelected = dpiInputText == presetDpi.toString()
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                dpiInputText = presetDpi.toString()
                            },
                            label = { Text("$presetDpi") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // DPI 输入与设置操作区
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dpiInputText,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                dpiInputText = newValue
                            }
                        },
                        label = { Text("目标 DPI (72~1000)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                val target = dpiInputText.toIntOrNull()
                                if (target != null) {
                                    viewModel.setDeviceDensity(target)
                                }
                            }
                        ),
                        trailingIcon = {
                            if (dpiInputText.isNotEmpty()) {
                                IconButton(onClick = { dpiInputText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清空")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val target = dpiInputText.toIntOrNull()
                            if (target != null) {
                                viewModel.setDeviceDensity(target)
                            }
                        },
                        enabled = dpiInputText.toIntOrNull() != null && !densityInfo.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("应用修改")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 恢复默认物理 DPI 按钮
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.resetDeviceDensity()
                    },
                    enabled = !densityInfo.isLoading && densityInfo.overrideDpi != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重置为物理默认值 (wm density reset)")
                }
            }
        }

        // 端口映射与反向代理 (Forward & Reverse) Card
        item(key = "forward_reverse_control") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 头部标题 + 帮助 “?” 按钮 + 刷新按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = "端口映射与反向代理",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "端口映射与反向代理 (Forward & Reverse)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showForwardReverseHelpDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "使用说明与场景解析",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.fetchReverseRules() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新反向代理列表",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "通过底层 ADB 隧道实现主控端与被控手机之间的端口双向穿透访问，用于调试 Web 页面、本地 API 或跨端开发服务。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. 正向端口映射 (Forward) 区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "正向端口映射 (Forward)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "${forwardRules.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (forwardRules.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.stopAllForwardRules() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("停止全部", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "方向：本机 ➡️ 手机 (本机开启端口监听，请求转发至被控手机服务)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (forwardRules.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "暂无活跃的 Forward 映射规则，点击下方按钮添加",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            forwardRules.forEach { rule ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "127.0.0.1:${rule.localPort}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = rule.remoteSpec,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (rule.isRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(6.dp)
                                                ) {}
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (rule.isRunning) "运行中 (活跃连接: ${rule.activeConnections} | 累计: ${rule.totalConnections})" else (rule.errorMessage ?: "已停止"),
                                                    fontSize = 10.sp,
                                                    color = if (rule.isRunning) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeForwardRule(rule.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除映射",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddForwardDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("＋ 添加 Forward 映射 (本机 ➡️ 远端)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. 反向代理 (Reverse) 区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "反向代理 (Reverse)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "${reverseRules.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (reverseRules.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.removeAllReverseRules() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("清空全部", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "方向：手机 ➡️ 本机 (手机内部开启监听，请求逆向打入控制端本机服务)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (reverseRules.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "暂无活跃的反向代理规则，点击下方按钮添加",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            reverseRules.forEach { rule ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "手机 ${rule.remoteSpec}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "本机 ${rule.localSpec}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "手机访问 127.0.0.1 对应端口时自动反向路由至本机",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeReverseRule(rule.remoteSpec) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除代理",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddReverseDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("＋ 添加 Reverse 代理 (远端 ➡️ 本机)", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. 电源与重启管理 Card
        item(key = "power_reboot") {
            InfoCard(
                title = "设备电源与重启管理 (Power & Reboot)",
                icon = Icons.Default.PowerSettingsNew,
                iconTint = Color(0xFFEF4444)
            ) {
                Text(
                    text = "通过底层 ADB 指令控制被控端手机重启或进入特殊引导模式。重启将导致当前的 ADB 连接中断。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Normal reboot button (Primary action)
                Button(
                    onClick = { pendingRebootOption = RebootOption.NORMAL },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reboot",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正常重启系统 (adb reboot)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid / Row of advanced modes: Fastboot & Recovery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { pendingRebootOption = RebootOption.FASTBOOT },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Fastboot",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fastboot 模式", fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = { pendingRebootOption = RebootOption.RECOVERY },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Recovery",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recovery 模式", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Row: Bootloader & Power Off
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { pendingRebootOption = RebootOption.BOOTLOADER },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = "Bootloader",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bootloader", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { pendingRebootOption = RebootOption.POWER_OFF },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power Off",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("关机", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // High-Risk Operation Double Confirmation Dialog
    if (pendingRebootOption != null) {
        val option = pendingRebootOption!!
        AlertDialog(
            onDismissRequest = { pendingRebootOption = null },
            icon = {
                Icon(
                    imageVector = if (option.isSevere) Icons.Default.Warning else Icons.Default.RestartAlt,
                    contentDescription = "Warning",
                    tint = if (option.isSevere) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (option.isSevere) "⚠️ 高危操作确认" else "确认重启设备",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = option.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = option.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = if (option.isSevere) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = option.warningText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = if (option.isSevere) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (option.isSevere) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = option
                        pendingRebootOption = null
                        viewModel.rebootDevice(target)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (option.isSevere) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (option.isSevere) "确认执行" else "确认重启")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRebootOption = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 远程截图完成提示与下载确认弹窗
    pendingScreenshotDialog?.let { screenshot ->
        val bitmap = remember(screenshot.localBytes) {
            try {
                BitmapFactory.decodeByteArray(screenshot.localBytes, 0, screenshot.localBytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
        val sizeMb = String.format(Locale.getDefault(), "%.2f MB", screenshot.fileSizeBytes / (1024.0 * 1024.0))

        AlertDialog(
            onDismissRequest = { viewModel.dismissScreenshotDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("📸 远程截图已生成", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 截图缩略图预览
                    if (bitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "远程截图预览",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "远端路径: ${screenshot.remotePath}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "文件大小: $sizeMb · 时间: ${screenshot.timestamp}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "是否将此截图下载保存到本地相册 (Pictures/ADBHelper)？",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadScreenshotToLocal(context, screenshot) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("下载到本地相册")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissScreenshotDialog() }
                ) {
                    Text("留在远端")
                }
            }
        )
    }

    // 远程录屏完成提示与下载确认弹窗
    pendingScreenRecordDialog?.let { record ->
        val sizeMb = if (record.fileSizeBytes > 0) {
            String.format(Locale.getDefault(), "%.2f MB", record.fileSizeBytes / (1024.0 * 1024.0))
        } else {
            "已生成"
        }
        val minutes = record.durationSeconds / 60
        val seconds = record.durationSeconds % 60
        val durationText = "${minutes}分${seconds}秒"

        AlertDialog(
            onDismissRequest = {
                if (!isDownloadingScreenRecord) {
                    viewModel.dismissScreenRecordDialog()
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("🎥 远程录屏已完成", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("录制时长:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(durationText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("视频大小:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(sizeMb, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "远端位置: ${record.remotePath}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "远程屏幕录制已完成，是否将视频下载保存到控制端本地视频库 (Movies/ADBHelper)？",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isDownloadingScreenRecord) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在拉取视频并保存到本地...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadScreenRecordToLocal(context, record) },
                    enabled = !isDownloadingScreenRecord,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("下载到本地")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissScreenRecordDialog() },
                    enabled = !isDownloadingScreenRecord
                ) {
                    Text("留在远端")
                }
            }
        )
    }

    // 端口映射与反向代理使用说明对话框
    if (showForwardReverseHelpDialog) {
        ForwardReverseHelpDialog(onDismiss = { showForwardReverseHelpDialog = false })
    }

    // 添加 Forward 正向映射弹窗
    if (showAddForwardDialog) {
        AddForwardDialog(
            onDismiss = { showAddForwardDialog = false },
            onConfirm = { localPort, remoteSpec ->
                viewModel.addForwardRule(localPort, remoteSpec)
            }
        )
    }

    // 添加 Reverse 反向代理弹窗
    if (showAddReverseDialog) {
        AddReverseDialog(
            onDismiss = { showAddReverseDialog = false },
            onConfirm = { remoteSpec, localSpec ->
                viewModel.addReverseRule(remoteSpec, localSpec)
            }
        )
    }
}

/**
 * 端口映射 (Forward) 与反向代理 (Reverse) 场景说明与区别指南对话框
 */
@Composable
private fun ForwardReverseHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "帮助说明",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "端口映射与反向代理功能说明",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 核心对比卡片
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 一句话核心区别：",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Forward (正向映射)：本机 ➡️ 手机 (访问手机上的服务)\n• Reverse (反向代理)：手机 ➡️ 本机 (手机访问本机的服务)",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 1. Forward 详细说明
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1. ADB Forward (正向端口映射)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "【流量流向】控制端本机 ➡️ 被控端手机\n" +
                                "【监听位置】本机 (127.0.0.1:端口)\n" +
                                "【转发目标】被控端手机内部指定的网络端口或抽象域 Socket\n\n" +
                                "【使用场景】\n" +
                                "1. 访问手机 Web 服务：被控端手机中运行了 Termux、本地 HTTP 调试服务器或微服务，在本机浏览器直接输入 http://127.0.0.1:端口 即可访问。\n" +
                                "2. 直连手机调试端口：在主控端使用开发工具或脚本直接连接手机上监听的端口 (如 scrcpy 视频套接字、JDWP 虚拟机调试端口、嵌入式数据库端口)。\n" +
                                "3. 穿透手机内网：免去配置被控手机复杂路由或局域网放行的困扰。\n\n" +
                                "【常用语法】tcp:端口 (如 tcp:8080) 或 localabstract:unix_socket_name",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Reverse 详细说明
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2. ADB Reverse (反向代理)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "【流量流向】被控端手机 ➡️ 控制端本机\n" +
                                "【监听位置】被控手机系统内部 (由 adbd 守护进程负责监听)\n" +
                                "【转发目标】回传至控制端本机对应的本地服务端口\n\n" +
                                "【使用场景】\n" +
                                "1. 跨端开发联调：手机运行 React Native 或 Flutter App，直接通过 127.0.0.1:8081 连接主控端电脑/手机上跑的 Metro Bundler 代码热重载服务。\n" +
                                "2. Web 前端页面调试：手机端浏览器直接打开 http://127.0.0.1:3000 即可调试主控端本地运行的 Vite、Next.js 或 Webpack 开发预览。\n" +
                                "3. 局域网防火墙穿透：无需手机与电脑在同一个 WiFi 网段，无视企业级路由器 AP 隔离限制，只要连上 ADB，手机访问 127.0.0.1 就能打到本机后端 API。\n\n" +
                                "【常用语法】tcp:端口 (如 tcp:8081)",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 3. 注意事项
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "📌 开发者使用须知：",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• 建议本地端口使用非特权端口范围 (1024 ~ 65535)。\n" +
                                    "• 端口映射与反向代理均依托 ADB 物理/网络连接，若会话断开，正向监听将关闭，反向代理规则可重新连接后再次配置。\n" +
                                    "• Android 5.0 (Lollipop)+ 均已完整原生支持 ADB Reverse。",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("我知道了")
            }
        }
    )
}

/**
 * 添加 Forward 正向映射弹窗
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddForwardDialog(
    onDismiss: () -> Unit,
    onConfirm: (localPort: Int, remoteSpec: String) -> Unit
) {
    var localPortText by remember { mutableStateOf("8080") }
    var remoteSpecText by remember { mutableStateOf("tcp:8080") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetPorts = listOf(
        Pair("8080", "Web服务"),
        Pair("3000", "Node.js"),
        Pair("5000", "Flask"),
        Pair("8000", "Python"),
        Pair("5555", "ADB端口")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("添加 Forward 正向映射", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "本机作为客户端监听端口，访问时将请求通过 ADB 转发至被控手机服务：",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = localPortText,
                    onValueChange = {
                        localPortText = it.filter { ch -> ch.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("本机监听端口 (Local Port)") },
                    placeholder = { Text("例如 8080 (1024-65535)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = remoteSpecText,
                    onValueChange = {
                        remoteSpecText = it
                        errorMessage = null
                    },
                    label = { Text("远端目标 (Remote Spec)") },
                    placeholder = { Text("例如 tcp:8080 或 localabstract:...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("快捷预设端口：", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetPorts.forEach { (port, label) ->
                        SuggestionChip(
                            onClick = {
                                localPortText = port
                                remoteSpecText = "tcp:$port"
                            },
                            label = { Text("$port ($label)", fontSize = 11.sp) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = localPortText.toIntOrNull()
                    if (port == null || port < 1024 || port > 65535) {
                        errorMessage = "请输入有效的本机端口 (1024 ~ 65535)"
                        return@Button
                    }
                    if (remoteSpecText.isBlank()) {
                        errorMessage = "请输入远端目标格式 (如 tcp:8080)"
                        return@Button
                    }
                    onConfirm(port, remoteSpecText.trim())
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("启动映射")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 添加 Reverse 反向代理弹窗
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddReverseDialog(
    onDismiss: () -> Unit,
    onConfirm: (remoteSpec: String, localSpec: String) -> Unit
) {
    var remotePortText by remember { mutableStateOf("8081") }
    var localPortText by remember { mutableStateOf("8081") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetPorts = listOf(
        Pair("8081", "React Native"),
        Pair("3000", "React/Next.js"),
        Pair("5173", "Vite"),
        Pair("8080", "Web服务"),
        Pair("8000", "Django/FastAPI")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("添加 Reverse 反向代理", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "手机 adbd 在自身开启端口监听，手机应用访问时逆向转发回控制端服务：",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = remotePortText,
                    onValueChange = {
                        remotePortText = it
                        errorMessage = null
                    },
                    label = { Text("远端手机监听端口 (Remote Spec)") },
                    placeholder = { Text("例如 tcp:8081 或 8081") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = localPortText,
                    onValueChange = {
                        localPortText = it
                        errorMessage = null
                    },
                    label = { Text("本机目标端口 (Local Spec)") },
                    placeholder = { Text("例如 tcp:8081 或 8081") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("快捷开发预设：", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetPorts.forEach { (port, label) ->
                        SuggestionChip(
                            onClick = {
                                remotePortText = "tcp:$port"
                                localPortText = "tcp:$port"
                            },
                            label = { Text("$port ($label)", fontSize = 11.sp) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val remote = if (!remotePortText.contains(":")) "tcp:${remotePortText.trim()}" else remotePortText.trim()
                    val local = if (!localPortText.contains(":")) "tcp:${localPortText.trim()}" else localPortText.trim()
                    if (remotePortText.isBlank()) {
                        errorMessage = "请输入远端端口"
                        return@Button
                    }
                    if (localPortText.isBlank()) {
                        errorMessage = "请输入本机目标端口"
                        return@Button
                    }
                    onConfirm(remote, local)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("添加代理")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val containerColor = if (!enabled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    } else if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val iconContainerColor = if (!enabled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val iconColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else if (isActive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(9.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 选项卡 3：输入控制 (支持 input text 文本输入与 input keyevent 丰富按键模拟)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceInputTab(
    viewModel: AdbViewModel,
    isGlassMode: Boolean
) {
    val listState by viewModel.deviceInputListState.collectAsState()
    val inputText by viewModel.deviceInputText.collectAsState()
    val customKeyCode by viewModel.deviceCustomKeyCode.collectAsState()
    val customKeyIsLongPress by viewModel.deviceCustomKeyIsLongPress.collectAsState()
    val mediaInfo by viewModel.mediaPlaybackInfo.collectAsState()
    val focusManager = LocalFocusManager.current

    val densityInfo by viewModel.deviceDensity.collectAsState()
    val (screenWidth, screenHeight) = remember(densityInfo.physicalSize) {
        val parts = densityInfo.physicalSize.lowercase().split("x")
        if (parts.size == 2) {
            val w = parts[0].trim().toIntOrNull() ?: 1080
            val h = parts[1].trim().toIntOrNull() ?: 2400
            w to h
        } else {
            1080 to 2400
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = if (isGlassMode) 100.dp else 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "input_header") {
            Text(
                text = "远端模拟输入与硬件按键",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. 文本输入 (input text)
        item(key = "text_input_card") {
            InfoCard(
                title = "文本输入 (adb shell input text)",
                icon = Icons.Default.TextFields,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "向被控端当前获得焦点的输入框直接注入文本内容（空格将自动转换为 %s，特殊符号自动转义）。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.setDeviceInputText(it) },
                    label = { Text("输入要发送的文本...") },
                    placeholder = { Text("例如：https://google.com 或 测试文本") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotEmpty()) {
                                viewModel.sendInputText(inputText)
                            }
                        }
                    ),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setDeviceInputText("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 快捷词 / 标点
                Text("快捷短语与标点:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                val quickPhrases = listOf("https://", "www.", ".com", ".cn", "123456", "admin")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickPhrases.forEach { phrase ->
                        AssistChip(
                            onClick = { viewModel.setDeviceInputText(inputText + phrase) },
                            label = { Text(phrase, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (inputText.isNotEmpty()) {
                            viewModel.sendInputText(inputText)
                        }
                    },
                    enabled = inputText.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发送文本至远端", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. 虚拟触控板 (Touchpad - 支持 input mouse move / roll / swipe)
        item(key = "touchpad_card") {
            TouchpadCard(
                viewModel = viewModel,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }

        // 2. 核心导航三大金刚按键 (Navigation Keys)
        item(key = "nav_keys_card") {
            InfoCard(
                title = "核心导航键 (Navigation)",
                icon = Icons.Default.TouchApp,
                iconTint = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "模拟 Android 标准底部导航三大按键，随时控制界面层级。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 返回 (BACK - keycode 4)
                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(4, "返回 (BACK)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                            Text("返回", fontSize = 11.sp)
                        }
                    }

                    // 主屏幕 (HOME - keycode 3)
                    Button(
                        onClick = { viewModel.sendInputKeyEvent(3, "主屏幕 (HOME)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(20.dp))
                            Text("主页", fontSize = 11.sp)
                        }
                    }

                    // 多任务 (APP_SWITCH - keycode 187)
                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(187, "多任务 (APP_SWITCH)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ViewCarousel, contentDescription = "Recent Apps", modifier = Modifier.size(20.dp))
                            Text("多任务", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 3. 系统硬件与状态控制按键 (System & Power)
        item(key = "system_keys_card") {
            InfoCard(
                title = "系统硬件与状态按键",
                icon = Icons.Default.Tune,
                iconTint = MaterialTheme.colorScheme.tertiary
            ) {
                Text(
                    text = "控制设备核心系统按键。支持单击唤醒/锁屏，以及长按 (--longpress) 直接呼出系统关机/重启电源菜单。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 电源键快捷区：支持单击与 --longpress 长按呼出电源菜单
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(26, "电源键 (单击)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("电源键 (单击)", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.sendInputKeyEvent(26, "长按电源键 (出电源菜单)", isLongPress = true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Long Press Power", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("长按电源键 (出菜单)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val systemKeys = listOf(
                    Triple(224, "亮屏唤醒", Icons.Default.WbSunny),
                    Triple(223, "息屏休眠", Icons.Default.Bedtime),
                    Triple(83, "下拉通知栏", Icons.Default.Notifications),
                    Triple(82, "菜单键", Icons.Default.Menu)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    systemKeys.forEach { (keyCode, name, icon) ->
                        OutlinedButton(
                            onClick = { viewModel.sendInputKeyEvent(keyCode, name) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(icon, contentDescription = name, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(name, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 4. 音量与多媒体控制按键 (Media & Volume)
        item(key = "media_keys_card") {
            InfoCard(
                title = "音量与媒体控制 (Media & Volume)",
                icon = Icons.Default.VolumeUp,
                iconTint = Color(0xFF10B981)
            ) {
                // 如果对端有媒体在播，展示在播状态条
                if (mediaInfo.hasActiveSession || mediaInfo.title.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (mediaInfo.isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaInfo.displayTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        if (mediaInfo.displayArtist.isNotBlank()) append(mediaInfo.displayArtist)
                                        if (mediaInfo.appName.isNotBlank()) {
                                            if (isNotEmpty()) append(" · ")
                                            append(mediaInfo.appName)
                                        }
                                        if (isNotEmpty()) append(" (")
                                        append(if (mediaInfo.isPlaying) "播放中" else mediaInfo.stateText)
                                        if (isNotEmpty()) append(")")
                                    },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 音量控制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(24, "音量 +") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("音量 +", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(25, "音量 -") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("音量 -", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(164, "静音") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VolumeMute, contentDescription = "Mute", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("静音", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 媒体控制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.mediaPrevious() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("上一曲", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = { viewModel.mediaPlayPause() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Icon(
                            imageVector = if (mediaInfo.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (mediaInfo.isPlaying) "暂停" else "播放", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = { viewModel.mediaNext() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("下一曲", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // 5. 编辑与十字方向导航键 (Editing & DPAD)
        item(key = "dpad_edit_keys_card") {
            InfoCard(
                title = "编辑与方向键 (DPAD & Edit)",
                icon = Icons.Default.KeyboardAlt,
                iconTint = Color(0xFFF59E0B)
            ) {
                // 编辑常用按键
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(66, "回车 (ENTER)") },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.KeyboardReturn, contentDescription = "Enter", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("回车", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(67, "退格 (DEL)") },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("退格删除", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.sendInputKeyEvent(61, "制表 (TAB)") },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.KeyboardTab, contentDescription = "Tab", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Tab", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 十字方向导航盘
                Text("方向导航键 (DPAD):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 上
                    OutlinedButton(
                        onClick = { viewModel.sendInputKeyEvent(19, "方向 上") },
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 左 中 右
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.sendInputKeyEvent(21, "方向 左") },
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left")
                        }

                        Button(
                            onClick = { viewModel.sendInputKeyEvent(23, "确认 (CENTER)") },
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.sendInputKeyEvent(22, "方向 右") },
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 下
                    OutlinedButton(
                        onClick = { viewModel.sendInputKeyEvent(20, "方向 下") },
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
                    }
                }
            }
        }

        // 6. 自定义 KeyCode 发送 (Custom KeyCode)
        item(key = "custom_keycode_card") {
            InfoCard(
                title = "自定义 KeyCode 发送 (input keyevent)",
                icon = Icons.Default.DeveloperBoard,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = "输入 Android 系统的标准整数按键码 (KeyEvent KeyCode)，直接发送至设备执行。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customKeyCode,
                        onValueChange = { if (it.length <= 5 && it.all { ch -> ch.isDigit() }) viewModel.setDeviceCustomKeyCode(it) },
                        label = { Text("KeyCode 数字代码") },
                        placeholder = { Text("例如 26 (电源), 27 (相机)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val code = customKeyCode.toIntOrNull()
                            if (code != null) {
                                viewModel.sendInputKeyEvent(code, isLongPress = customKeyIsLongPress)
                            }
                        },
                        enabled = customKeyCode.toIntOrNull() != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(if (customKeyIsLongPress) "长按发送" else "发送按键")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilterChip(
                    selected = customKeyIsLongPress,
                    onClick = { viewModel.setDeviceCustomKeyIsLongPress(!customKeyIsLongPress) },
                    label = { Text("附加 --longpress 长按参数", fontSize = 11.sp) }
                )
            }
        }
    }
}

enum class TouchpadHeightMode(val title: String, val heightDp: Dp) {
    STANDARD("标准", 190.dp),
    LARGE("加大", 340.dp),
    EXPANDED("超大", 480.dp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TouchpadCard(
    viewModel: AdbViewModel,
    screenWidth: Int,
    screenHeight: Int
) {
    var mode by remember { mutableStateOf(TouchpadMode.MOUSE) }
    val protocol = TouchpadProtocol.VIRTUAL_MOUSE
    var sensitivity by remember { mutableFloatStateOf(1.0f) }
    var touchpadHeightMode by remember { mutableStateOf(TouchpadHeightMode.STANDARD) }

    val virtualMouseStatus by viewModel.virtualMouseStatus.collectAsState()
    val isTouchpadDragging by viewModel.isTouchpadDragging.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var pendingTapJob by remember { mutableStateOf<Job?>(null) }
    var lastTapUpTime by remember { mutableLongStateOf(0L) }
    var lastTapUpPos by remember { mutableStateOf(Offset.Zero) }
    var isDoubleTapDragActive by remember { mutableStateOf(false) }

    // 远端虚拟光标屏幕坐标 (初始居中)
    var curX by remember(screenWidth) { mutableStateOf((screenWidth / 2).toFloat()) }
    var curY by remember(screenHeight) { mutableStateOf((screenHeight / 2).toFloat()) }

    // 亚像素累加器 (确保微小慢速滑动不被整型截断，杜绝丢帧卡顿)
    var subPixelDx by remember { mutableFloatStateOf(0f) }
    var subPixelDy by remember { mutableFloatStateOf(0f) }

    // 手势滑动模式下的起始触控点
    var dragStartX by remember { mutableStateOf(0f) }
    var dragStartY by remember { mutableStateOf(0f) }

    InfoCard(
        title = "虚拟触控板 (Touchpad)",
        icon = Icons.Default.Mouse,
        iconTint = Color(0xFF6366F1)
    ) {
        Text(
            text = "滑动手指模拟鼠标移动或屏幕手势。电脑标准手势：双击不松手并移动即为按住拖拽 (左键按下移动)。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 模式切换与控制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = mode == TouchpadMode.MOUSE,
                onClick = { mode = TouchpadMode.MOUSE },
                label = { Text("鼠标光标", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Mouse, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )

            FilterChip(
                selected = mode == TouchpadMode.SWIPE,
                onClick = { mode = TouchpadMode.SWIPE },
                label = { Text("手势滑动", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            AssistChip(
                onClick = {
                    curX = (screenWidth / 2).toFloat()
                    curY = (screenHeight / 2).toFloat()
                },
                label = { Text("居中", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center", modifier = Modifier.size(14.dp))
                }
            )
        }

        // 鼠标模式下的原生硬件鼠标驱动状态条 (UHID / UInput)
        if (mode == TouchpadMode.MOUSE) {
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (virtualMouseStatus) {
                    is VirtualMouseStatus.Connected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    is VirtualMouseStatus.Connecting -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    is VirtualMouseStatus.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    VirtualMouseStatus.Disconnected -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = when (virtualMouseStatus) {
                        is VirtualMouseStatus.Connected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        is VirtualMouseStatus.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = null,
                            tint = when (virtualMouseStatus) {
                                is VirtualMouseStatus.Connected -> MaterialTheme.colorScheme.primary
                                is VirtualMouseStatus.Connecting -> MaterialTheme.colorScheme.tertiary
                                is VirtualMouseStatus.Error -> MaterialTheme.colorScheme.error
                                VirtualMouseStatus.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val title = when (virtualMouseStatus) {
                                is VirtualMouseStatus.Connected -> "🟢 原生硬件鼠标已插入 (系统光标已显现)"
                                is VirtualMouseStatus.Connecting -> "🔄 正在注册内核 HID 虚拟鼠标..."
                                is VirtualMouseStatus.Error -> "⚠️ 原生鼠标注册失败"
                                VirtualMouseStatus.Disconnected -> "原生硬件鼠标模拟 (呼出系统小箭头)"
                            }
                            val sub = when (val s = virtualMouseStatus) {
                                is VirtualMouseStatus.Connected -> s.description
                                is VirtualMouseStatus.Connecting -> "向 Linux 内核 /dev/uhid 注册硬件节点中..."
                                is VirtualMouseStatus.Error -> s.message
                                VirtualMouseStatus.Disconnected -> "模拟真实物理鼠标插入，在手机屏幕上渲染原生光标"
                            }
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (virtualMouseStatus is VirtualMouseStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sub,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    when (virtualMouseStatus) {
                        is VirtualMouseStatus.Connected -> {
                            OutlinedButton(
                                onClick = { viewModel.virtualMouseManager.disconnect() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("断开", fontSize = 11.sp)
                            }
                        }
                        is VirtualMouseStatus.Connecting -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("连接中", fontSize = 11.sp)
                            }
                        }
                        else -> {
                            Button(
                                onClick = { viewModel.virtualMouseManager.connect() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("插入鼠标", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 触控板尺寸选择栏
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("触控板尺寸: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TouchpadHeightMode.values().forEach { sizeMode ->
                    FilterChip(
                        selected = touchpadHeightMode == sizeMode,
                        onClick = { touchpadHeightMode = sizeMode },
                        label = {
                            Text(
                                text = sizeMode.title,
                                fontSize = 11.sp,
                                fontWeight = if (touchpadHeightMode == sizeMode) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "(${curX.toInt()}, ${curY.toInt()}) [${screenWidth}x${screenHeight}]",
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 灵敏度滑杆 (0.1x - 5.0x，默认 1.0x)
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "灵敏度: ${String.format(java.util.Locale.US, "%.1fx", sensitivity)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Slider(
                value = sensitivity,
                onValueChange = {
                    sensitivity = kotlin.math.round(it * 10f) / 10f
                },
                valueRange = 0.1f..5.0f,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "重置1x",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { sensitivity = 1.0f }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 触控板感应区 (Touchpad Surface - 可动态扩展高度)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(touchpadHeightMode.heightDp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .pointerInput(mode, protocol, sensitivity, screenWidth, screenHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val downPos = down.position
                        val touchSlop = viewConfiguration.touchSlop

                        // 电脑触控板判定：距离上次轻触抬起在 350ms 内且距离在 60dp 范围内，视为双击手势序列
                        val timeSinceLastTap = downTime - lastTapUpTime
                        val distSinceLastTap = (downPos - lastTapUpPos).getDistance()
                        val isSecondTap = timeSinceLastTap in 1..350L && distSinceLastTap < 60.dp.toPx()

                        if (isSecondTap) {
                            pendingTapJob?.cancel()
                            pendingTapJob = null
                        }

                        var isPointerMoved = false
                        var isDraggingWithMouse = false
                        dragStartX = curX
                        dragStartY = curY
                        subPixelDx = 0f
                        subPixelDy = 0f

                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val currentChange = event.changes.firstOrNull { it.id == pointerId } ?: break

                            if (currentChange.pressed) {
                                val delta = currentChange.position - currentChange.previousPosition
                                val totalMovement = (currentChange.position - downPos).getDistance()

                                if (!isPointerMoved && totalMovement > touchSlop) {
                                    isPointerMoved = true
                                    if (isSecondTap && mode == TouchpadMode.MOUSE) {
                                        // 电脑触控板标准核心特性：双击不松手并移动，等于按下鼠标左键拖拽！
                                        isDraggingWithMouse = true
                                        isDoubleTapDragActive = true
                                        viewModel.sendTouchpadMouseDown(curX.toInt(), curY.toInt())
                                    }
                                }

                                if (isPointerMoved) {
                                    currentChange.consume()
                                    val scale = sensitivity * 1.8f
                                    subPixelDx += delta.x * scale
                                    subPixelDy += delta.y * scale

                                    val moveX = subPixelDx.toInt()
                                    val moveY = subPixelDy.toInt()

                                    if (moveX != 0 || moveY != 0) {
                                        subPixelDx -= moveX
                                        subPixelDy -= moveY

                                        curX = (curX + moveX).coerceIn(0f, screenWidth.toFloat())
                                        curY = (curY + moveY).coerceIn(0f, screenHeight.toFloat())

                                        if (mode == TouchpadMode.MOUSE) {
                                            viewModel.sendTouchpadMove(
                                                x = curX.toInt(),
                                                y = curY.toInt(),
                                                dx = moveX,
                                                dy = moveY,
                                                protocol = protocol
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 手指抬起 (UP)
                                val upTime = System.currentTimeMillis()
                                val tapDuration = upTime - downTime

                                if (isDraggingWithMouse) {
                                    // 双击拖拽结束：释放鼠标左键！
                                    currentChange.consume()
                                    viewModel.sendTouchpadMouseUp(curX.toInt(), curY.toInt())
                                    isDraggingWithMouse = false
                                    isDoubleTapDragActive = false
                                    lastTapUpTime = 0L
                                } else if (isPointerMoved) {
                                    // 普通手势滑动结束
                                    if (mode == TouchpadMode.SWIPE) {
                                        viewModel.sendTouchpadSwipe(
                                            x1 = dragStartX.toInt(),
                                            y1 = dragStartY.toInt(),
                                            x2 = curX.toInt(),
                                            y2 = curY.toInt()
                                        )
                                    }
                                    lastTapUpTime = 0L
                                } else {
                                    // 未滑动：轻触或长按
                                    if (tapDuration >= viewConfiguration.longPressTimeoutMillis) {
                                        viewModel.sendTouchpadLongPress(curX.toInt(), curY.toInt())
                                        lastTapUpTime = 0L
                                    } else if (isSecondTap) {
                                        // 连续两次快速轻触且未拖动 -> 触发双击
                                        viewModel.sendTouchpadDoubleTap(curX.toInt(), curY.toInt())
                                        lastTapUpTime = 0L
                                    } else {
                                        // 首次轻触抬起：启动 220ms 延时计时器，若无第二次按下则触发单击
                                        lastTapUpTime = upTime
                                        lastTapUpPos = downPos
                                        val tapX = curX.toInt()
                                        val tapY = curY.toInt()
                                        pendingTapJob = coroutineScope.launch {
                                            delay(220L)
                                            viewModel.sendTouchpadTap(tapX, tapY, useMouse = true)
                                            lastTapUpTime = 0L
                                        }
                                    }
                                }
                                break
                            }
                        }
                    }
                }
        ) {
            // 背景中央提示
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (mode == TouchpadMode.MOUSE) Icons.Default.Mouse else Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (mode == TouchpadMode.MOUSE) "在触控区移动光标 · 轻触单击 · 双击 · 双击按住滑动即可按住拖拽" else "在触控区划动手势 · 松手时注入滑动手势",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // 拖拽激活状态悬浮指示横条
            if (isDoubleTapDragActive || isTouchpadDragging) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.95f),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🖱️ 正在按住拖拽 (鼠标左键按下移动中)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 触控板下方操作栏：双行大按键设计，防止文字被截断，布局清晰宽敞
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 第一行：鼠标核心主按键 (左键 & 拖拽锁定 & 右键)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.sendTouchpadTap(
                            curX.toInt(),
                            curY.toInt(),
                            useMouse = true
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Mouse, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("左键(单击)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                if (isTouchpadDragging) {
                    Button(
                        onClick = { viewModel.sendTouchpadMouseUp(curX.toInt(), curY.toInt()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("释放拖拽", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.sendTouchpadMouseDown(curX.toInt(), curY.toInt()) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("按住拖拽", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.sendTouchpadRightClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("右键(返回)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
            }

            // 第二行：滚轮上滑、滚轮下滑、鼠标中键
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.sendTouchpadScroll(
                            delta = 5,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll Up", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("滚轮上滑", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.sendTouchpadScroll(
                            delta = -5,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll Down", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("滚轮下滑", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.sendTouchpadMiddleClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Adjust, contentDescription = "Middle Click", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("鼠标中键", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 远端光标显示与 UHID 硬件支持检测
        Text(
            text = "光标显示与虚拟硬件支持 (UHID / 视觉辅助):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "由于 Android 仅在检测到硬件鼠标时绘制小箭头光标，可通过下方开关直接开启系统触控圆点或十字准星，或检测设备是否支持 /dev/uhid 硬件模拟。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        val isShowTouches by viewModel.isRemoteShowTouches.collectAsState()
        val isPointerLocation by viewModel.isRemotePointerLocation.collectAsState()
        val uhidStatus by viewModel.uhidSupportStatus.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.queryVisualIndicators()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isShowTouches,
                onClick = { viewModel.setRemoteShowTouches(!isShowTouches) },
                label = { Text("触控小白点", fontSize = 11.sp) }
            )

            FilterChip(
                selected = isPointerLocation,
                onClick = { viewModel.setRemotePointerLocation(!isPointerLocation) },
                label = { Text("十字准星/坐标", fontSize = 11.sp) }
            )

            Spacer(modifier = Modifier.weight(1f))

            AssistChip(
                onClick = { viewModel.detectUhidSupport() },
                label = { Text("探测UHID权限", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        if (uhidStatus != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uhidStatus ?: "",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

