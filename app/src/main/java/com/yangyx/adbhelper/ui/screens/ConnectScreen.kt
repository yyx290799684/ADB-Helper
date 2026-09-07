package com.yangyx.adbhelper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.adb.PairResult
import com.yangyx.adbhelper.data.entity.DeviceEntity
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.ConnectionState
import com.yangyx.adbhelper.ui.models.GroupedDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectScreen(
    viewModel: AdbViewModel,
    onNavigateToRemote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val groupedDevices by viewModel.groupedDevices.collectAsState()

    val isScanningLan by viewModel.isScanningLan.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanStatusText by viewModel.scanStatusText.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    var ipAddress by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("") }

    var showPairDialog by remember { mutableStateOf(false) }
    var pairIp by remember { mutableStateOf("") }
    var pairPort by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }
    var isPairing by remember { mutableStateOf(false) }
    var pairResult by remember { mutableStateOf<PairResult?>(null) }

    var pendingDeleteSingleIp by remember { mutableStateOf<DeviceEntity?>(null) }
    var pendingDeleteGroup by remember { mutableStateOf<GroupedDevice?>(null) }
    var editingGroupDevice by remember { mutableStateOf<GroupedDevice?>(null) }
    var editAliasText by remember { mutableStateOf("") }
    var selectedIconType by remember { mutableStateOf("phone") }

    val context = LocalContext.current
    val ACTION_USB_PERMISSION = "com.yangyx.adbhelper.USB_PERMISSION"

    DisposableEffect(context) {
        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.apply {
                                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                                var adbInterface: android.hardware.usb.UsbInterface? = null
                                for (i in 0 until device.interfaceCount) {
                                    val intf = device.getInterface(i)
                                    if (intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC &&
                                        intf.interfaceSubclass == 0x42 &&
                                        intf.interfaceProtocol == 0x01) {
                                        adbInterface = intf
                                        break
                                    }
                                }
                                if (adbInterface != null) {
                                    var endpointIn: android.hardware.usb.UsbEndpoint? = null
                                    var endpointOut: android.hardware.usb.UsbEndpoint? = null
                                    for (i in 0 until adbInterface.endpointCount) {
                                        val ep = adbInterface.getEndpoint(i)
                                        if (ep.direction == UsbConstants.USB_DIR_IN) {
                                            endpointIn = ep
                                        } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                                            endpointOut = ep
                                        }
                                    }
                                    if (endpointIn != null && endpointOut != null) {
                                        val connection = usbManager.openDevice(device)
                                        if (connection != null) {
                                            connection.claimInterface(adbInterface, true)
                                            viewModel.connectUsb(connection, endpointIn, endpointOut)
                                            onNavigateToRemote()
                                        } else {
                                            Toast.makeText(context, "无法打开 USB 设备", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "此设备不是支持 ADB 的设备", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "用户拒绝了 USB 权限", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        onDispose {
            context.unregisterReceiver(usbReceiver)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Connection Banner Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connect_hero_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phonelink,
                                    contentDescription = "ADB Remote",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "一键连接远端Android设备",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "支持无线调试、Scrcpy高清镜像、低延迟触控",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("目标设备 IP 地址") },
                            placeholder = { Text("192.168.x.x") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("ip_input")
                        )
                        OutlinedTextField(
                            value = portText,
                            onValueChange = { portText = it },
                            label = { Text("端口") },
                            placeholder = { Text("5555") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("port_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val port = portText.toIntOrNull() ?: 5555
                                viewModel.connectToDevice(ipAddress, port)
                            },
                            enabled = connectionState !is ConnectionState.Connecting && ipAddress.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("connect_button")
                        ) {
                            if (connectionState is ConnectionState.Connecting) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("连接中...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Connect")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("一键连接", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                                val deviceList = usbManager.deviceList
                                var foundDevice: UsbDevice? = null
                                for (device in deviceList.values) {
                                    for (i in 0 until device.interfaceCount) {
                                        val intf = device.getInterface(i)
                                        if (intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC &&
                                            intf.interfaceSubclass == 0x42 &&
                                            intf.interfaceProtocol == 0x01) {
                                            foundDevice = device
                                            break
                                        }
                                    }
                                }
                                if (foundDevice != null) {
                                    val permissionIntent = PendingIntent.getBroadcast(
                                        context, 0, Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) },
                                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    usbManager.requestPermission(foundDevice, permissionIntent)
                                } else {
                                    Toast.makeText(context, "未找到通过 OTG 连接的 ADB 设备", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = connectionState !is ConnectionState.Connecting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = "OTG", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OTG直连", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                if (pairIp.isBlank() && ipAddress.isNotBlank()) {
                                    pairIp = ipAddress.trim()
                                }
                                pairResult = null
                                isPairing = false
                                showPairDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.WifiTethering, contentDescription = "Pair", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("无线配对码", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                        }
                    }

                    // Connection status banner
                    when (val state = connectionState) {
                        is ConnectionState.Connecting -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        is ConnectionState.Connected -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFF1B5E20),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Connected", tint = Color.Green)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("已连接至: ${state.deviceName}", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("IP: ${state.ip}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    }
                                    TextButton(onClick = onNavigateToRemote) {
                                        Text("进入屏幕控制 >", color = Color.Green, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is ConnectionState.Error -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "连接失败: ${state.message}",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        // LAN Device Discovery Card Section
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lan_scan_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Top Row: Icon + Title + Action Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "LAN Scan",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "局域网设备自动发现",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                if (isScanningLan) {
                                    viewModel.stopLanScan()
                                } else {
                                    viewModel.startLanScan()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanningLan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isScanningLan) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("停止", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Scan",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("扫描", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "自动检索同一 Wi-Fi 下开放 ADB 端口 (5555) 的设备",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (isScanningLan || scanStatusText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (isScanningLan) {
                            LinearProgressIndicator(
                                progress = scanProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = scanStatusText,
                            fontSize = 12.sp,
                            color = if (isScanningLan) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (discoveredDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "发现可连接设备 (${discoveredDevices.size}):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            discoveredDevices.forEach { device ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            ipAddress = device.ip
                                            portText = device.port.toString()
                                            viewModel.connectToDevice(device.ip, device.port)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Phonelink,
                                                    contentDescription = "Discovered Device",
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val isIpv6 = device.ip.contains(":")
                                            val displayIp = if (isIpv6) "[${device.ip}]:${device.port}" else "${device.ip}:${device.port}"
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isIpv6) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "IPv6",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = displayIp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = if (isIpv6) 12.sp else 14.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "ADB 端口 ${device.port} 开放 · 点击直接连接",
                                                fontSize = 11.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                ipAddress = device.ip
                                                portText = device.port.toString()
                                                viewModel.connectToDevice(device.ip, device.port)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("发起连接", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Grouped Devices Management
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Devices",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "设备连接管理 (${groupedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (groupedDevices.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "No Devices",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无连接过的设备历史记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(
                items = groupedDevices,
                key = { it.key }
            ) { group ->
                GroupedDeviceCard(
                    group = group,
                    onConnectSequentially = {
                        viewModel.connectDeviceSequentially(group)
                    },
                    onConnectSingleIp = { entity ->
                        ipAddress = entity.ipAddress
                        portText = entity.port.toString()
                        viewModel.connectToDevice(entity.ipAddress, entity.port, targetAlias = group.aliasName.ifBlank { null })
                    },
                    onEditGroup = {
                        editingGroupDevice = group
                        editAliasText = group.aliasName
                        selectedIconType = if (group.iconType.isNotBlank()) group.iconType else "phone"
                    },
                    onMoveUp = { index ->
                        viewModel.reorderIpInGroup(group, index, index - 1)
                    },
                    onMoveDown = { index ->
                        viewModel.reorderIpInGroup(group, index, index + 1)
                    },
                    onDeleteSingleIp = { entity ->
                        pendingDeleteSingleIp = entity
                    },
                    onDeleteGroup = {
                        pendingDeleteGroup = group
                    }
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Single IP Deletion Double-Confirmation Dialog
    if (pendingDeleteSingleIp != null) {
        val item = pendingDeleteSingleIp!!
        AlertDialog(
            onDismissRequest = { pendingDeleteSingleIp = null },
            title = { Text("删除 IP 连接记录") },
            text = {
                Text(
                    text = "确定要从设备历史记录中删除该 IP [${item.ipAddress}:${item.port}] 吗？",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSingleIp(item)
                        pendingDeleteSingleIp = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSingleIp = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Entire Device Deletion Double-Confirmation Dialog
    if (pendingDeleteGroup != null) {
        val group = pendingDeleteGroup!!
        val devDisplay = if (group.serialNo.isNotBlank()) "${group.deviceName} (${group.serialNo})" else group.deviceName
        AlertDialog(
            onDismissRequest = { pendingDeleteGroup = null },
            title = { Text("删除整个设备") },
            text = {
                Text(
                    text = "确定要删除设备 [$devDisplay] 及其名下的全部 ${group.ipRecords.size} 个 IP 连接记录吗？此操作无法撤销。",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroupDevice(group)
                        pendingDeleteGroup = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("全部删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGroup = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Wireless Debugging Pairing Dialog
    if (showPairDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isPairing) {
                    showPairDialog = false
                    pairResult = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("无线调试配对与连接", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val currentResult = pairResult
                    if (currentResult != null) {
                        when (currentResult) {
                            is PairResult.Success -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "ADB 调试服务连接成功！",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentResult.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            is PairResult.PairingPortDetected -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.WifiTethering,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "配对端口连通成功",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentResult.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            is PairResult.Failure -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "连接检测失败",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentResult.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        if (currentResult.suggestion != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = currentResult.suggestion,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isPairing) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(38.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "正在连接目标设备并探测服务...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "测试 ADB 授权与无线配对端口，请稍候",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Tip Banner
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "推荐：Android 11+ 可直接使用主页的【直接连接】！只需输入无线调试主页显示的【IP 和端口】，点击连接后远端手机将直接弹出授权框，无需配对码。",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pairIp,
                            onValueChange = { pairIp = it },
                            label = { Text("目标设备 IP 地址") },
                            placeholder = { Text("例如 192.168.1.100") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pairPort,
                            onValueChange = { pairPort = it },
                            label = { Text("服务端口 (配对端口或连接端口)") },
                            placeholder = { Text("例如 37123 或 41235") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pairCode,
                            onValueChange = { pairCode = it },
                            label = { Text("6位数配对码 (选填)") },
                            placeholder = { Text("例如 123456") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                val currentResult = pairResult
                if (currentResult != null) {
                    when (currentResult) {
                        is PairResult.Success -> {
                            Button(
                                onClick = {
                                    ipAddress = currentResult.ip
                                    portText = currentResult.port.toString()
                                    viewModel.connectToDevice(currentResult.ip, currentResult.port)
                                    showPairDialog = false
                                    pairResult = null
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("立即连接 (${currentResult.port})")
                            }
                        }
                        is PairResult.PairingPortDetected -> {
                            Button(
                                onClick = {
                                    ipAddress = currentResult.ip
                                    showPairDialog = false
                                    pairResult = null
                                    Toast.makeText(context, "已填入 IP：$pairIp，请输入无线调试页面的连接端口直接连接", Toast.LENGTH_LONG).show()
                                }
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("填入 IP 并返回主页连接")
                            }
                        }
                        is PairResult.Failure -> {
                            Button(onClick = { pairResult = null }) {
                                Text("重新检测")
                            }
                        }
                    }
                } else if (!isPairing) {
                    Button(
                        enabled = pairIp.isNotBlank() && pairPort.isNotBlank(),
                        onClick = {
                            val pPort = pairPort.trim().toIntOrNull()
                            if (pPort == null || pPort !in 1..65535) {
                                Toast.makeText(context, "请输入有效的端口号 (1~65535)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isPairing = true
                            viewModel.pairDevice(pairIp.trim(), pPort, pairCode.trim()) { res ->
                                isPairing = false
                                pairResult = res
                            }
                        }
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("检测并配对")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isPairing,
                    onClick = {
                        showPairDialog = false
                        pairResult = null
                        isPairing = false
                    }
                ) {
                    Text(if (pairResult != null) "关闭" else "取消")
                }
            }
        )
    }

    // Edit Device Alias & Icon Dialog
    if (editingGroupDevice != null) {
        val group = editingGroupDevice!!
        AlertDialog(
            onDismissRequest = { editingGroupDevice = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("编辑设备备注与图标", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "原设备: ${group.deviceName} ${if (group.serialNo.isNotBlank()) "(${group.serialNo})" else ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editAliasText,
                        onValueChange = { editAliasText = it },
                        label = { Text("设备备注名") },
                        placeholder = { Text(group.deviceName.ifBlank { "Android 设备" }) },
                        singleLine = true,
                        trailingIcon = {
                            if (editAliasText.isNotEmpty()) {
                                IconButton(onClick = { editAliasText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清除",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "设备类型图标 (可选 手机、平板、电视、汽车):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val iconOptions = listOf(
                        Triple("phone", "手机", Icons.Default.PhoneAndroid),
                        Triple("tablet", "平板", Icons.Default.TabletAndroid),
                        Triple("tv", "电视", Icons.Default.Tv),
                        Triple("car", "汽车", Icons.Default.DirectionsCar)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconOptions.forEach { (typeKey, label, iconVec) ->
                            val isSelected = selectedIconType.equals(typeKey, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedIconType = typeKey }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                        viewModel.updateDeviceAliasAndIcon(group, editAliasText, selectedIconType)
                        editingGroupDevice = null
                        Toast.makeText(context, "设备备注及图标已保存", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGroupDevice = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun GroupedDeviceCard(
    group: GroupedDevice,
    onConnectSequentially: () -> Unit,
    onConnectSingleIp: (DeviceEntity) -> Unit,
    onEditGroup: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDeleteSingleIp: (DeviceEntity) -> Unit,
    onDeleteGroup: () -> Unit
) {
    val deviceIcon = when (group.iconType.lowercase()) {
        "tablet" -> Icons.Default.TabletAndroid
        "tv" -> Icons.Default.Tv
        "car" -> Icons.Default.DirectionsCar
        else -> Icons.Default.PhoneAndroid
    }

    val hasAlias = group.aliasName.isNotBlank()
    val mainTitle = if (hasAlias) group.aliasName else {
        if (group.serialNo.isNotBlank()) "${group.deviceName} (${group.serialNo})" else group.deviceName
    }
    val subtitle = "共 ${group.ipRecords.size} 个网络记录"

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Device Icon & Title & Edit & Sequential Connect & Delete Device
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onConnectSequentially() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = deviceIcon,
                            contentDescription = "Device",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = mainTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Edit Button (Custom Alias & Icon)
                IconButton(
                    onClick = onEditGroup,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑设备备注与图标",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                FilledTonalButton(
                    onClick = onConnectSequentially,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Sequential Connect",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("依次连接", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = onDeleteGroup,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Device",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // IP Records List with Order Adjustment and Single IP actions
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                group.ipRecords.forEachIndexed { index, item ->
                    val dateStr = remember(item.lastConnectedTime) {
                        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(item.lastConnectedTime))
                    }
                    val isIpv6 = item.ipAddress.contains(":")
                    val displayIp = if (isIpv6) "[${item.ipAddress}]:${item.port}" else "${item.ipAddress}:${item.port}"

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            // Row 1: Index Badge + IPv6 Chip + IP:Port + Connect Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                if (isIpv6) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 5.dp)
                                    ) {
                                        Text(
                                            text = "IPv6",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = displayIp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = if (isIpv6) 12.sp else 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                FilledTonalButton(
                                    onClick = { onConnectSingleIp(item) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("连接", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            // Row 2: Last connected time + Reordering arrows (Up/Down) + Delete button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "上次连接: $dateStr",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.weight(1f)
                                )

                                // Move Up
                                IconButton(
                                    onClick = { onMoveUp(index) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(2.dp))

                                // Move Down
                                IconButton(
                                    onClick = { onMoveDown(index) },
                                    enabled = index < group.ipRecords.size - 1,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = if (index < group.ipRecords.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Delete Single IP
                                IconButton(
                                    onClick = { onDeleteSingleIp(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete IP",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(15.dp)
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
