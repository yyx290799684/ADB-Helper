package com.yangyx.adbhelper.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.app.Application
import com.yangyx.adbhelper.MainActivity
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WIDGET_QUICK_CONNECT = "com.yangyx.adbhelper.widget.ACTION_QUICK_CONNECT"
        const val ACTION_WIDGET_CONNECT_DEVICE = "com.yangyx.adbhelper.widget.ACTION_CONNECT_DEVICE"
        const val ACTION_WIDGET_DISCONNECT = "com.yangyx.adbhelper.widget.ACTION_DISCONNECT"
        const val ACTION_WIDGET_EXECUTE_ACTION = "com.yangyx.adbhelper.widget.ACTION_EXECUTE_ACTION"
        const val ACTION_WIDGET_START_SCAN = "com.yangyx.adbhelper.widget.ACTION_START_SCAN"

        const val EXTRA_DEVICE_IP = "extra_device_ip"
        const val EXTRA_DEVICE_PORT = "extra_device_port"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
        const val EXTRA_DEVICE_SERIAL = "extra_device_serial"
        const val EXTRA_ACTION_ID = "extra_action_id"

        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                when (action) {
                    ACTION_WIDGET_QUICK_CONNECT -> {
                        showToast(context, "正在后台发起连接常用设备...")
                        WidgetStateHolder.onConnecting("常用设备", "")
                        WidgetUpdateHelper.updateAllWidgets(context)
                        com.yangyx.adbhelper.service.AdbSessionService.startConnectingService(context, "常用设备")
                        val app = context.applicationContext as Application
                        val vm = AdbViewModel.getOrCreate(app)
                        vm.connectLatestDevice()
                    }

                    ACTION_WIDGET_CONNECT_DEVICE -> {
                        val ip = intent.getStringExtra(EXTRA_DEVICE_IP) ?: ""
                        val port = intent.getIntExtra(EXTRA_DEVICE_PORT, 5555)
                        val rawName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: ""
                        val serial = intent.getStringExtra(EXTRA_DEVICE_SERIAL) ?: ""

                        val isAddressOnly = rawName.isBlank() || rawName.contains(":") || rawName.startsWith("开放")
                        val displayTitle = if (isAddressOnly) "$ip:$port" else rawName
                        val passDeviceName = if (isAddressOnly) "" else rawName

                        try {
                            val db = com.yangyx.adbhelper.data.AppDatabase.getDatabase(context)
                            val now = System.currentTimeMillis()
                            if (serial.isNotBlank()) {
                                val devs = db.deviceDao().getDevicesBySerialNo(serial)
                                devs.forEach { dev ->
                                    db.deviceDao().insertOrUpdateDevice(dev.copy(lastConnectedTime = now))
                                }
                            } else {
                                val existing = db.deviceDao().getDeviceByIpAndPort(ip, port)
                                if (existing != null && !existing.name.startsWith("开放")) {
                                    db.deviceDao().insertOrUpdateDevice(existing.copy(lastConnectedTime = now))
                                }
                            }
                        } catch (_: Exception) {}

                        WidgetStateHolder.onConnecting(displayTitle, "$ip:$port")
                        WidgetUpdateHelper.updateAllWidgets(context)

                        showToast(context, "正在连接 $displayTitle ...")
                        // 启动前台服务保活，避免在依次探测多个 IP 时被系统杀死或挂起
                        com.yangyx.adbhelper.service.AdbSessionService.startConnectingService(context, displayTitle)

                        val app = context.applicationContext as Application
                        val vm = AdbViewModel.getOrCreate(app)
                        vm.connectDeviceSmart(
                            serialNo = serial,
                            deviceName = passDeviceName,
                            fallbackIp = ip,
                            fallbackPort = port
                        )
                    }

                    ACTION_WIDGET_DISCONNECT -> {
                        val session = WidgetStateHolder.activeSession
                        if (session != null) {
                            try {
                                session.connection.disconnect()
                            } catch (_: Exception) {}
                        }
                        AdbViewModel.currentInstance?.disconnectAll(stopSessionService = true)
                        WidgetStateHolder.onDisconnected()
                        WidgetUpdateHelper.clearWidgetConnectedSnapshot(context)
                        WidgetUpdateHelper.cancelConnectionHealthCheckAlarm(context)
                        com.yangyx.adbhelper.service.AdbSessionService.stopService(context)
                        WidgetUpdateHelper.updateAllWidgets(context)
                        showToast(context, "已断开 ADB 连接")
                    }

                    ACTION_WIDGET_EXECUTE_ACTION -> {
                        val actionId = intent.getStringExtra(EXTRA_ACTION_ID) ?: ""
                        executeWidgetAction(context, actionId)
                    }

                    ACTION_WIDGET_START_SCAN -> {
                        showToast(context, "正在后台扫描局域网 ADB 设备...")
                        WidgetStateHolder.onScanStarted()
                        WidgetUpdateHelper.updateAllWidgets(context)

                        // 启动保活前台服务，防止扫描局域网时被系统休眠或冻结
                        com.yangyx.adbhelper.service.AdbSessionService.startService(
                            context,
                            "局域网扫描",
                            "正在探测开放 5555 端口的设备..."
                        )

                        try {
                            val db = com.yangyx.adbhelper.data.AppDatabase.getDatabase(context)
                            try {
                                db.deviceDao().deletePlaceholderDevices()
                            } catch (_: Exception) {}

                            val scanner = com.yangyx.adbhelper.adb.LanScanner()
                            val discovered = scanner.scanLanForAdbDevices(
                                ports = listOf(5555),
                                timeoutMs = 300
                            )

                            val now = System.currentTimeMillis()
                            val mappedList = mutableListOf<WidgetDeviceInfo>()

                            for (d in discovered) {
                                mappedList.add(
                                    WidgetDeviceInfo(
                                        name = "${d.ip}:${d.port}",
                                        ip = d.ip,
                                        port = d.port,
                                        serialNo = "",
                                        lastConnectedTime = now,
                                        ipCount = 1
                                    )
                                )
                            }

                            WidgetStateHolder.onScanFinished(
                                devices = mappedList,
                                progressText = if (mappedList.isNotEmpty()) "发现 ${mappedList.size} 台设备" else "未发现新设备",
                                timestamp = now
                            )
                            WidgetUpdateHelper.saveScannedDevicesToCache(context, mappedList, now)
                            WidgetUpdateHelper.scheduleScanExpirationAlarm(context, WidgetStateHolder.SCAN_EXPIRATION_MILLIS)

                            if (mappedList.isNotEmpty()) {
                                showToast(context, "扫描完成，发现 ${mappedList.size} 台设备 (保留10分钟)")
                            } else {
                                showToast(context, "扫描完成，未找到开放 5555 端口的设备")
                            }
                        } catch (e: Exception) {
                            WidgetStateHolder.onScanFinished(emptyList(), "扫描出错: ${e.message}")
                            showToast(context, "扫描出错: ${e.message}")
                        } finally {
                            val hasActive = WidgetStateHolder.activeConnection?.isConnected == true
                            if (!hasActive) {
                                com.yangyx.adbhelper.service.AdbSessionService.stopService(context)
                            }
                            WidgetUpdateHelper.updateAllWidgets(context)
                        }
                    }

                    WidgetUpdateHelper.ACTION_WIDGET_EXPIRE_SCAN -> {
                        if (WidgetStateHolder.isScanExpired()) {
                            WidgetStateHolder.resetScanState()
                            WidgetUpdateHelper.clearScannedDevicesCache(context)
                            WidgetUpdateHelper.updateAllWidgets(context)
                        }
                    }

                    WidgetUpdateHelper.ACTION_WIDGET_HEALTH_CHECK -> {
                        // 1. 同时检查扫描结果是否过期 (10分钟)
                        if (WidgetStateHolder.isScanExpired()) {
                            WidgetStateHolder.resetScanState()
                            WidgetUpdateHelper.clearScannedDevicesCache(context)
                            WidgetUpdateHelper.updateAllWidgets(context)
                        }

                        // 2. 10 分钟连接存活检测：支持真实通信探活、后台/进程恢复后自动重连与重试，确认无法连接再回到未连接状态
                        val snapshot = WidgetUpdateHelper.loadWidgetConnectedSnapshot(context)
                        val existingVm = AdbViewModel.currentInstance
                        val hasLogicalConnection =
                            (WidgetStateHolder.currentState == WidgetStateHolder.State.CONNECTED) ||
                            (snapshot != null) ||
                            (existingVm?.connectionState?.value is ConnectionState.Connected) ||
                            (existingVm?.sessions?.value?.isNotEmpty() == true)

                        if (hasLogicalConnection) {
                            val app = context.applicationContext as Application
                            val vm = AdbViewModel.getOrCreate(app)
                            vm.performConnectionHealthCheck(context, force = true)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun executeWidgetAction(context: Context, actionId: String) {
        val actionType = WidgetActionType.fromId(actionId)

        // 1. 对于需要进入 App 界面的快捷操作：镜像投屏、文件、终端
        when (actionType) {
            WidgetActionType.MIRROR -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    action = WidgetUpdateHelper.ACTION_WIDGET_MIRROR
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(launchIntent)
                showToast(context, "正在启动屏幕投屏...")
                return
            }
            WidgetActionType.FILE_EXPLORER -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    action = WidgetUpdateHelper.ACTION_WIDGET_FILE
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(launchIntent)
                showToast(context, "正在打开文件管理...")
                return
            }
            WidgetActionType.TERMINAL -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    action = WidgetUpdateHelper.ACTION_WIDGET_TERMINAL
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(launchIntent)
                showToast(context, "正在打开远程终端...")
                return
            }
            else -> {}
        }

        // 2. 对于直接发送底层 ADB 模拟指令的快捷操作
        val existingVm = AdbViewModel.currentInstance
        val currentConn = WidgetStateHolder.activeConnection ?: existingVm?.activeConnection
        val snapshot = WidgetUpdateHelper.loadWidgetConnectedSnapshot(context)
        val hasLogicalConnection =
            (WidgetStateHolder.currentState == WidgetStateHolder.State.CONNECTED) ||
            (currentConn != null) ||
            (snapshot != null) ||
            (existingVm?.connectionState?.value is ConnectionState.Connected)

        if (!hasLogicalConnection) {
            WidgetStateHolder.onDisconnected()
            WidgetUpdateHelper.updateAllWidgets(context)
            showToast(context, "未连接 ADB 设备，请先点击快速连接")
            return
        }

        val app = context.applicationContext as Application
        val adbVm = AdbViewModel.getOrCreate(app)

        var targetConn = currentConn?.takeIf { it.isConnected }
        if (targetConn == null) {
            showToast(context, "连接已断开，正在尝试自动重连...")
            targetConn = adbVm.reconnectAndEnsureSession(
                fallbackIp = snapshot?.ip ?: "",
                fallbackPort = snapshot?.port ?: 5555,
                fallbackName = snapshot?.name ?: "",
                fallbackSerial = snapshot?.serialNo ?: "",
                timeoutMs = 4500,
                forceWireProbe = false
            )
            if (targetConn == null) {
                adbVm.handleUnrecoverableWidgetDisconnect()
                showToast(context, "自动重连失败，设备已回到未连接状态")
                return
            }
        }

        try {
            performShellWidgetAction(context, targetConn, actionType, adbVm)
        } catch (_: Exception) {
            // 若原 Socket 处于半开或 IP 变更导致指令失败，与主程序一致优先尝试完整自动重连后再重试执行
            showToast(context, "连接异常，正在重新建立会话...")
            val recoveredConn = adbVm.reconnectAndEnsureSession(
                fallbackIp = snapshot?.ip ?: "",
                fallbackPort = snapshot?.port ?: 5555,
                fallbackName = snapshot?.name ?: "",
                fallbackSerial = snapshot?.serialNo ?: "",
                timeoutMs = 4500,
                forceWireProbe = true
            )
            if (recoveredConn != null) {
                try {
                    performShellWidgetAction(context, recoveredConn, actionType, adbVm)
                    return
                } catch (_: Exception) {}
            }
            adbVm.handleUnrecoverableWidgetDisconnect()
            showToast(context, "无法重新建立连接，设备已回到未连接状态")
        }
    }

    private fun performShellWidgetAction(
        context: Context,
        connection: com.yangyx.adbhelper.adb.AdbConnection,
        actionType: WidgetActionType,
        adbVm: AdbViewModel
    ) {
        when (actionType) {
            WidgetActionType.LOCK -> {
                connection.executeShell("input keyevent 26")
                showToast(context, "已发送锁屏/电源指令")
            }
            WidgetActionType.SCREENSHOT -> {
                val time = System.currentTimeMillis()
                val filename = "/sdcard/Pictures/Screenshots/screenshot_$time.png"
                connection.executeShell("mkdir -p /sdcard/Pictures/Screenshots && screencap -p $filename 2>/dev/null || screencap -p /sdcard/screenshot.png")
                showToast(context, "截屏指令已发送，画面已保存")
            }
            WidgetActionType.HOME -> {
                connection.executeShell("input keyevent 3")
                showToast(context, "已发送返回主页指令")
            }
            WidgetActionType.BACK -> {
                connection.executeShell("input keyevent 4")
                showToast(context, "已发送返回指令")
            }
            WidgetActionType.RECENTS -> {
                connection.executeShell("input keyevent 187")
                showToast(context, "已发送多任务切换指令")
            }
            WidgetActionType.MUTE -> {
                connection.executeShell("input keyevent 164")
                showToast(context, "已发送静音切换指令")
            }
            WidgetActionType.NOTIFICATIONS -> {
                connection.executeShell("cmd statusbar expand-notifications 2>/dev/null || service call statusbar 1 2>/dev/null || input keyevent 83")
                showToast(context, "已展开下拉通知栏")
            }
            WidgetActionType.VOLUME_DOWN -> {
                connection.executeShell("input keyevent 25")
                showToast(context, "音量 -")
            }
            WidgetActionType.VOLUME_UP -> {
                connection.executeShell("input keyevent 24")
                showToast(context, "音量 +")
            }
            WidgetActionType.SCREEN_OFF -> {
                connection.executeShell("input keyevent 223")
                showToast(context, "已发送息屏指令")
            }
            WidgetActionType.WAKEUP -> {
                connection.executeShell("input keyevent 224")
                showToast(context, "已发送唤醒指令")
            }
            WidgetActionType.PLAY_PAUSE -> {
                // 1. 发送与设备控制页面完全一致的全局媒体播放/暂停按键 (KEYCODE_MEDIA_PLAY_PAUSE = 85)
                connection.executeShell("input keyevent 85")

                // 2. 状态取反并即时更新桌面小部件（瞬时给用户视觉与触控反馈）
                val willBePlaying = !WidgetStateHolder.isMediaPlaying
                WidgetStateHolder.updateMediaPlaybackState(willBePlaying)
                WidgetUpdateHelper.updateAllWidgets(context)

                showToast(context, if (willBePlaying) "已发送播放指令" else "已发送暂停指令")

                // 3. 延时 400ms 与远端对齐真实播放状态 (与 AdbViewModel.mediaPlayPause 保持完全一致的节奏)
                receiverScope.launch {
                    kotlinx.coroutines.delay(400L)
                    adbVm.fetchMediaPlaybackInfo()
                    val actual = WidgetUpdateHelper.queryMediaPlayingState(connection)
                    if (WidgetStateHolder.isMediaPlaying != actual) {
                        WidgetStateHolder.updateMediaPlaybackState(actual)
                        WidgetUpdateHelper.updateAllWidgets(context)
                    }
                }
            }
            else -> {}
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }
}
