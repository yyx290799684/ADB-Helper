package com.yangyx.adbhelper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.models.RebootOption

@Composable
fun DeviceInfoScreen(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier
) {
    val info by viewModel.systemInfo.collectAsState()
    val isRefreshing by viewModel.isRefreshingSystemInfo.collectAsState()
    var pendingRebootOption by remember { mutableStateOf<RebootOption?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header", contentType = "header") {
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

        // Hardware and OS details card (Display fields ONLY if available)
        item(key = "hardware_os", contentType = "card") {
            InfoCard(
                title = "硬件与系统 (Hardware & OS)",
                icon = Icons.Default.PhoneAndroid,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                // 1. ro.product.marketname (e.g. Xiaomi 14)
                if (!info.marketName.isNullOrBlank()) {
                    InfoRow("设备通用名 / 市场名", info.marketName!!)
                }

                // Serial Number (getprop ro.serialno / adb get-serialno)
                if (!info.serialNo.isNullOrBlank()) {
                    InfoRow("设备唯一序列号 (Serial)", info.serialNo!!)
                }

                // 2. Hardware Model & Manufacturer
                if (info.model.isNotBlank() && info.model != "Unknown") {
                    val brandPrefix = if (info.manufacturer.isNotBlank() && info.manufacturer != "Unknown" && !info.model.startsWith(info.manufacturer, ignoreCase = true)) {
                        "${info.manufacturer} "
                    } else ""
                    InfoRow("设备型号", "$brandPrefix${info.model}")
                } else if (info.manufacturer.isNotBlank() && info.manufacturer != "Unknown") {
                    InfoRow("制造厂商", info.manufacturer)
                }

                // 3. ro.mi.os.version.incremental / custom OS version (non-Android version)
                if (!info.osVersion.isNullOrBlank()) {
                    InfoRow("系统固件版本 (OS)", info.osVersion!!)
                }

                // 4. Android version
                if (info.androidVersion.isNotBlank() && info.androidVersion != "Unknown") {
                    val sdkStr = if (info.sdkVersion > 0) " (API ${info.sdkVersion})" else ""
                    InfoRow("Android 版本", "Android ${info.androidVersion}$sdkStr")
                }

                // 5. ro.hardware (e.g. qcom)
                if (!info.cpuHardware.isNullOrBlank()) {
                    InfoRow("CPU 厂商代号", info.cpuHardware!!)
                }

                // 6. ro.soc.model (e.g. SM8650)
                if (!info.socModel.isNullOrBlank()) {
                    InfoRow("SoC 芯片型号", info.socModel!!)
                }

                // 7. CPU Architecture
                if (info.cpuArchitecture.isNotBlank() && info.cpuArchitecture != "Unknown") {
                    InfoRow("CPU 指令集架构", info.cpuArchitecture)
                }
            }
        }

        // CPU & Memory usage card
        item(key = "ram_cpu", contentType = "card") {
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

        // Battery Card
        if (info.batteryLevel > 0 || (info.batteryStatus.isNotBlank() && info.batteryStatus != "Unknown")) {
            item(key = "battery", contentType = "card") {
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

        // Storage Card
        if (info.storageTotalGb > 0f) {
            item(key = "storage", contentType = "card") {
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

        // Power & Device Reboot Card (Positioned at the end)
        item(key = "power_reboot", contentType = "card") {
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
                        Text("关闭电源 (关机)", fontSize = 13.sp)
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
