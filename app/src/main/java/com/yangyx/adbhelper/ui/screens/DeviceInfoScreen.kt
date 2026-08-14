package com.yangyx.adbhelper.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.ui.AdbViewModel

@Composable
fun DeviceInfoScreen(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier
) {
    val info by viewModel.systemInfo.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
                Button(onClick = { viewModel.refreshSystemInfo() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("刷新状态")
                }
            }
        }

        // Hardware details card
        item {
            InfoCard(
                title = "硬件与系统 (Hardware & OS)",
                icon = Icons.Default.PhoneAndroid,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                InfoRow("设备型号", "${info.manufacturer} ${info.model}")
                InfoRow("Android 版本", "Android ${info.androidVersion} (API ${info.sdkVersion})")
                InfoRow("CPU 架构", info.cpuArchitecture)
            }
        }

        // CPU & Memory usage card
        item {
            InfoCard(
                title = "内存与 CPU (RAM & CPU)",
                icon = Icons.Default.Memory,
                iconTint = Color(0xFF0284C7)
            ) {
                val ramProgress = if (info.ramTotalMb > 0) info.ramUsedMb.toFloat() / info.ramTotalMb else 0f
                Text(
                    text = "RAM 内存使用: ${info.ramUsedMb} MB / ${info.ramTotalMb} MB (${(ramProgress * 100).toInt()}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = ramProgress,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CPU 占用率: ${info.cpuUsage}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = info.cpuUsage / 100f,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
        }

        // Battery Card
        item {
            InfoCard(
                title = "电池健康与电量 (Battery)",
                icon = Icons.Default.BatteryChargingFull,
                iconTint = Color(0xFF16A34A)
            ) {
                InfoRow("电量百分比", "${info.batteryLevel}%")
                InfoRow("电池温度", "${info.batteryTemperature} °C")
                InfoRow("供电状态", info.batteryStatus)
            }
        }

        // Storage Card
        item {
            InfoCard(
                title = "内部存储空间 (Storage)",
                icon = Icons.Default.SdStorage,
                iconTint = Color(0xFFD97706)
            ) {
                InfoRow("已用存储", "${info.storageUsedGb} GB / ${info.storageTotalGb} GB")
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
