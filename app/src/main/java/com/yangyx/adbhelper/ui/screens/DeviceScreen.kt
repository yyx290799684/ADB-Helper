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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.models.RebootOption
import com.yangyx.adbhelper.ui.models.TouchpadMode
import com.yangyx.adbhelper.ui.models.TouchpadProtocol
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
    var pendingRebootOption by remember { mutableStateOf<RebootOption?>(null) }
    var dpiInputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // 初始化时获取最新的 DPI 与分辨率
    LaunchedEffect(Unit) {
        viewModel.fetchDeviceDensity()
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
                    onClick = { viewModel.fetchDeviceDensity() },
                    enabled = !densityInfo.isLoading
                ) {
                    if (densityInfo.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新 DPI")
                    }
                }
            }
        }

        // 1. 设备 DPI 与显示控制 Card
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
                        onClick = { viewModel.sendInputKeyEvent(88, "上一曲") },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("上一曲", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = { viewModel.sendInputKeyEvent(85, "播放/暂停") },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("播放/暂停", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = { viewModel.sendInputKeyEvent(87, "下一曲") },
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
            text = "滑动手指模拟鼠标移动或屏幕滑动手势，支持轻触点击、双击、长按及滚轮翻页。",
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
                    detectTapGestures(
                        onTap = {
                            viewModel.sendTouchpadTap(
                                curX.toInt(),
                                curY.toInt(),
                                useMouse = true
                            )
                        },
                        onDoubleTap = {
                            viewModel.sendTouchpadDoubleTap(curX.toInt(), curY.toInt())
                        },
                        onLongPress = {
                            viewModel.sendTouchpadLongPress(curX.toInt(), curY.toInt())
                        }
                    )
                }
                .pointerInput(mode, protocol, sensitivity, screenWidth, screenHeight) {
                    detectDragGestures(
                        onDragStart = {
                            subPixelDx = 0f
                            subPixelDy = 0f
                            dragStartX = curX
                            dragStartY = curY
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // 为高分辨率屏幕适配平滑倍率系数 (1.8f 基础增益 + 用户灵敏度)，微小滑动毫秒级无损累加
                            val scale = sensitivity * 1.8f
                            subPixelDx += dragAmount.x * scale
                            subPixelDy += dragAmount.y * scale

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
                        },
                        onDragEnd = {
                            if (mode == TouchpadMode.SWIPE) {
                                viewModel.sendTouchpadSwipe(
                                    x1 = dragStartX.toInt(),
                                    y1 = dragStartY.toInt(),
                                    x2 = curX.toInt(),
                                    y2 = curY.toInt()
                                )
                            }
                        }
                    )
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
                    text = if (mode == TouchpadMode.MOUSE) "在触控区滑动移动光标 · 单击 · 双击 · 长按拖拽" else "在触控区划动手势 · 松手时注入滑动手势",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 触控板下方操作栏：双行大按键设计，防止文字被截断，布局清晰宽敞
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 第一行：鼠标核心主按键 (左键 & 右键)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Mouse, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("鼠标左键 (单击)", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.sendTouchpadRightClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("鼠标右键 (返回)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
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

