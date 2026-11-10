package com.yangyx.adbhelper.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.data.DeviceHotspotConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceHotspotConfigDialog(
    config: DeviceHotspotConfig,
    onDismissRequest: () -> Unit,
    onSave: (DeviceHotspotConfig) -> Unit,
    onReReadFromSystem: () -> Unit
) {
    val context = LocalContext.current

    var ssid by remember(config) { mutableStateOf(config.ssid) }
    var securityType by remember(config) { mutableStateOf(config.securityType) }
    var passphrase by remember(config) { mutableStateOf(config.passphrase) }
    var band by remember(config) { mutableStateOf(config.band) }
    var bandwidth by remember(config) { mutableStateOf(config.bandwidth) }

    var isPasswordVisible by remember { mutableStateOf(false) }

    val isPasswordNeeded = !securityType.equals("open", ignoreCase = true) && !securityType.startsWith("owe", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.WifiTethering,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "便携热点参数设置",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 提示卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "配置按设备独立保存在本地，不会修改对端系统文件。快捷开启热点时将按此参数执行 cmd wifi start-softap 指令。",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. 热点 SSID
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("热点名称 (SSID)") },
                    leadingIcon = {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. 安全类型 (Security Type)
                Text(
                    text = "安全类型 (Security)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                val securityOptions = listOf(
                    "wpa2" to "WPA2 PSK",
                    "wpa3" to "WPA3 SAE",
                    "wpa3_transition" to "WPA2/WPA3 混合",
                    "open" to "开放 (无密码)",
                    "owe" to "OWE",
                    "owe_transition" to "OWE 混合"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    securityOptions.forEach { (typeKey, label) ->
                        val isSelected = securityType.equals(typeKey, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { securityType = typeKey },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                if (isPasswordNeeded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. 密码输入
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("热点密码 (8位以上)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "切换密码可见性",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        supportingText = {
                            if (passphrase.length < 8) {
                                Text("密码长度至少需 8 个字符", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                            }
                        },
                        isError = passphrase.length < 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. 频段选择 (Bands -b)
                Text(
                    text = "工作频段 (Bands -b)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                val bandOptions = listOf(
                    "any" to "自动推荐 (Any)",
                    "2" to "2.4 GHz",
                    "5" to "5 GHz (推荐)",
                    "6" to "6 GHz (Wi-Fi 6E)",
                    "bridged" to "双频并发 (2.4G+5G)",
                    "bridged_2_6" to "双频并发 (2.4G+6G)",
                    "bridged_5_6" to "双频并发 (5G+6G)"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bandOptions.forEach { (bandKey, label) ->
                        val isSelected = band.equals(bandKey, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { band = bandKey },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. 最大信道频宽 (Bandwidth -w)
                Text(
                    text = "信道频宽 (Bandwidth -w)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                val widthOptions = listOf(
                    "auto" to "自动频宽",
                    "20" to "20 MHz",
                    "40" to "40 MHz",
                    "80" to "80 MHz",
                    "160" to "160 MHz",
                    "320" to "320 MHz"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    widthOptions.forEach { (widthKey, label) ->
                        val isSelected = bandwidth.equals(widthKey, ignoreCase = true) ||
                                (widthKey == "auto" && (bandwidth == "-1" || bandwidth.isBlank()))
                        FilterChip(
                            selected = isSelected,
                            onClick = { bandwidth = widthKey },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 从系统重新读取按钮
                OutlinedButton(
                    onClick = onReReadFromSystem,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("从系统文件重新读取默认配置", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ssid.isBlank()) {
                        Toast.makeText(context, "SSID 不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isPasswordNeeded && passphrase.length < 8) {
                        Toast.makeText(context, "热点密码长度至少需要 8 个字符", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val updated = DeviceHotspotConfig(
                        ssid = ssid.trim(),
                        securityType = securityType,
                        passphrase = passphrase.trim(),
                        band = band,
                        bandwidth = bandwidth
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("保存设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
