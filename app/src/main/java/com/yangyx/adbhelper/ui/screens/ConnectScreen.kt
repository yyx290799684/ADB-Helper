package com.yangyx.adbhelper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Usb
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.data.entity.DeviceEntity
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.ConnectionState
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
    val savedDevices by viewModel.savedDevices.collectAsState()

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
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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
                            onClick = { showPairDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.DeveloperMode, contentDescription = "Pair", modifier = Modifier.size(18.dp))
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lan_scan_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "局域网设备自动发现",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "自动检索同一 Wi-Fi 下开放 ADB 端口 (5555) 的设备",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanningLan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isScanningLan) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("停止", fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Scan",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("扫描", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    if (isScanningLan || scanStatusText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
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
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                            Text(
                                                text = "${device.ip}:${device.port}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "ADB 端口 5555 开放 · 点击直接连接",
                                                fontSize = 11.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                ipAddress = device.ip
                                                portText = device.port.toString()
                                                viewModel.connectToDevice(device.ip, device.port)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("发起连接", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Saved & Recent Devices
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "最近连接过的设备 (${savedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (savedDevices.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "No Devices",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无最近连接的历史记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(savedDevices) { device ->
                DeviceHistoryCard(
                    device = device,
                    onConnect = {
                        ipAddress = device.ipAddress
                        portText = device.port.toString()
                        viewModel.connectToDevice(device.ipAddress, device.port)
                    },
                    onDelete = {
                        viewModel.deleteDeviceFromHistory(device.ipAddress)
                    }
                )
            }
        }
    }

    // Wireless Debugging Pairing Dialog
    if (showPairDialog) {
        AlertDialog(
            onDismissRequest = { showPairDialog = false },
            title = { Text("Android 11+ 无线调试配对") },
            text = {
                Column {
                    Text("请在目标手机的 [开发者选项 -> 无线调试 -> 使用配对码配对设备] 中查看参数", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pairIp,
                        onValueChange = { pairIp = it },
                        label = { Text("目标设备 IP") },
                        placeholder = { Text("192.168.x.x") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pairPort,
                        onValueChange = { pairPort = it },
                        label = { Text("配对服务端口") },
                        placeholder = { Text("37123") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pairCode,
                        onValueChange = { pairCode = it },
                        label = { Text("6位数配对码") },
                        placeholder = { Text("123456") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pPort = pairPort.toIntOrNull() ?: 37000
                    viewModel.pairDevice(pairIp, pPort, pairCode) { success ->
                        showPairDialog = false
                    }
                }) {
                    Text("开始配对")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun DeviceHistoryCard(
    device: DeviceEntity,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(device.lastConnectedTime) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(device.lastConnectedTime))
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Phonelink,
                        contentDescription = "Device",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (device.name.isNotBlank()) device.name else "Android 设备 (${device.ipAddress})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "IP: ${device.ipAddress}:${device.port} | 上次连接: $dateStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onConnect) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Quick Connect", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
