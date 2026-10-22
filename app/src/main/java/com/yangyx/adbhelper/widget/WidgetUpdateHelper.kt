package com.yangyx.adbhelper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.yangyx.adbhelper.MainActivity
import com.yangyx.adbhelper.R
import com.yangyx.adbhelper.data.AppDatabase
import com.yangyx.adbhelper.data.entity.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdateHelper {

    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    const val ACTION_WIDGET_MIRROR = "com.yangyx.adbhelper.widget.ACTION_MIRROR"
    const val ACTION_WIDGET_FILE = "com.yangyx.adbhelper.widget.ACTION_FILE"
    const val ACTION_WIDGET_TERMINAL = "com.yangyx.adbhelper.widget.ACTION_TERMINAL"
    const val ACTION_WIDGET_OPEN_DEVICE = "com.yangyx.adbhelper.widget.ACTION_OPEN_DEVICE"

    fun updateAllWidgets(context: Context) {
        helperScope.launch {
            ensureFallbackDeviceInfo(context)

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return@launch

            // 1. 同步刷新 2x2 小部件 (通过 ids 与 ComponentName 全量更新，避免广播环路)
            val cn2x2 = ComponentName(context, QuickConnectWidgetProvider2x2::class.java)
            val views2x2 = build2x2Views(context)
            val ids2x2 = appWidgetManager.getAppWidgetIds(cn2x2)
            if (ids2x2 != null && ids2x2.isNotEmpty()) {
                try {
                    appWidgetManager.updateAppWidget(ids2x2, views2x2)
                } catch (_: Exception) {}
            }
            try {
                appWidgetManager.updateAppWidget(cn2x2, views2x2)
            } catch (_: Exception) {}

            // 2. 同步刷新 4x2 小部件
            val cn4x2 = ComponentName(context, ControlCenterWidgetProvider4x2::class.java)
            val views4x2 = build4x2Views(context)
            val ids4x2 = appWidgetManager.getAppWidgetIds(cn4x2)
            if (ids4x2 != null && ids4x2.isNotEmpty()) {
                try {
                    appWidgetManager.updateAppWidget(ids4x2, views4x2)
                } catch (_: Exception) {}
            }
            try {
                appWidgetManager.updateAppWidget(cn4x2, views4x2)
            } catch (_: Exception) {}
        }
    }

    private suspend fun ensureFallbackDeviceInfo(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val allDevices = db.deviceDao().getAllDevicesDirect()
            if (allDevices.isEmpty()) {
                WidgetStateHolder.recentDevices = emptyList()
                return
            }

            // 按物理设备（有序列号的按 serialNo 分组，无序列号的按设备名或 IP 分组）
            val withSerial = allDevices.filter { it.serialNo.isNotBlank() }
            val withoutSerial = allDevices.filter { it.serialNo.isBlank() }

            val deviceGroups = mutableListOf<WidgetDeviceInfo>()

            // 1. 有 serialNo 的物理设备分组
            withSerial.groupBy { it.serialNo }.forEach { (serial, ipRecords) ->
                val sortedIps = ipRecords.sortedWith(
                    compareByDescending<DeviceEntity> { it.lastConnectedTime }
                        .thenByDescending { it.isFavorite }
                        .thenBy { it.sortOrder }
                )
                val primary = sortedIps.first()
                val bestAlias = sortedIps.firstOrNull { it.aliasName.isNotBlank() }?.aliasName ?: ""
                val bestName = sortedIps.firstOrNull { it.name.isNotBlank() }?.name ?: ""
                val bestModel = sortedIps.firstOrNull { it.model.isNotBlank() }?.model ?: ""

                // 优先设备别名 > 真实设备名称 > 型号 > Android 设备
                val finalName = when {
                    bestAlias.isNotBlank() -> bestAlias
                    bestName.isNotBlank() -> bestName
                    bestModel.isNotBlank() -> bestModel
                    else -> "Android 设备"
                }

                deviceGroups.add(
                    WidgetDeviceInfo(
                        name = finalName,
                        ip = primary.ipAddress,
                        port = primary.port,
                        serialNo = serial,
                        lastConnectedTime = sortedIps.maxOfOrNull { it.lastConnectedTime } ?: primary.lastConnectedTime,
                        isFavorite = sortedIps.any { it.isFavorite },
                        ipCount = sortedIps.size
                    )
                )
            }

            // 2. 无 serialNo 的设备（按别名或名称分组）
            withoutSerial.groupBy {
                when {
                    it.aliasName.isNotBlank() -> "alias_${it.aliasName}"
                    it.name.isNotBlank() -> "name_${it.name}"
                    else -> "ip_${it.ipAddress}_${it.port}"
                }
            }.forEach { (_, ipRecords) ->
                val sortedIps = ipRecords.sortedWith(
                    compareByDescending<DeviceEntity> { it.lastConnectedTime }
                        .thenByDescending { it.isFavorite }
                )
                val primary = sortedIps.first()
                val bestAlias = sortedIps.firstOrNull { it.aliasName.isNotBlank() }?.aliasName ?: ""
                val bestName = sortedIps.firstOrNull { it.name.isNotBlank() }?.name ?: ""
                val bestModel = sortedIps.firstOrNull { it.model.isNotBlank() }?.model ?: ""

                val finalName = when {
                    bestAlias.isNotBlank() -> bestAlias
                    bestName.isNotBlank() -> bestName
                    bestModel.isNotBlank() -> bestModel
                    else -> "Android 设备 (${primary.ipAddress})"
                }

                deviceGroups.add(
                    WidgetDeviceInfo(
                        name = finalName,
                        ip = primary.ipAddress,
                        port = primary.port,
                        serialNo = "",
                        lastConnectedTime = sortedIps.maxOfOrNull { it.lastConnectedTime } ?: primary.lastConnectedTime,
                        isFavorite = sortedIps.any { it.isFavorite },
                        ipCount = sortedIps.size
                    )
                )
            }

            // 严格按最近连接时间倒序排序
            val top5 = deviceGroups.sortedWith(
                compareByDescending<WidgetDeviceInfo> { it.lastConnectedTime }
                    .thenByDescending { it.isFavorite }
            ).take(5)

            WidgetStateHolder.recentDevices = top5

            if (WidgetStateHolder.currentState == WidgetStateHolder.State.DISCONNECTED &&
                WidgetStateHolder.deviceName.isBlank() && top5.isNotEmpty()
            ) {
                val first = top5.first()
                WidgetStateHolder.setFallbackDeviceInfo(first.name, "${first.ip}:${first.port}")
            }
        } catch (_: Exception) {}
    }

    fun build2x2Views(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_2x2)
        val state = WidgetStateHolder.currentState
        val recents = WidgetStateHolder.recentDevices
        val name = WidgetStateHolder.deviceName.ifBlank { "点击连接设备" }
        val ip = WidgetStateHolder.deviceIp.ifBlank { "上次会话设备已就绪" }

        // 1. Header 点击进入应用
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openAppIntent,
            getPendingIntentFlags()
        )
        views.setOnClickPendingIntent(R.id.layout_header, openAppPendingIntent)

        // 2. 状态圆点与文字
        when (state) {
            WidgetStateHolder.State.CONNECTED -> {
                views.setImageViewResource(R.id.iv_status_dot, R.drawable.widget_status_dot_connected)
                val count = WidgetStateHolder.connectedDeviceCount
                val statusTitle = if (count > 1) "已连($count)" else "已连接"
                views.setTextViewText(R.id.tv_status_text, statusTitle)

                views.setViewVisibility(R.id.layout_disconnected_2x2, View.GONE)
                views.setViewVisibility(R.id.layout_connected_2x2, View.VISIBLE)

                views.setTextViewText(R.id.tv_device_name, name)
                views.setTextViewText(R.id.tv_device_ip, ip)

                // 点击设备名或卡片头部进入 App 该设备连接控制页面
                val openDeviceIntent = createOpenDevicePendingIntent(context, 105)
                views.setOnClickPendingIntent(R.id.layout_device_info_2x2, openDeviceIntent)
                views.setOnClickPendingIntent(R.id.tv_device_name, openDeviceIntent)
                views.setOnClickPendingIntent(R.id.tv_device_ip, openDeviceIntent)

                // 2 个自定义快捷操作按钮
                val actions = WidgetPreferences.get2x2Actions(context)
                bind2x2ActionSlot(context, views, R.id.btn_action_1_2x2, R.id.iv_action_1_2x2, R.id.tv_action_1_2x2, actions.getOrElse(0) { WidgetActionType.LOCK }, 110)
                bind2x2ActionSlot(context, views, R.id.btn_action_2_2x2, R.id.iv_action_2_2x2, R.id.tv_action_2_2x2, actions.getOrElse(1) { WidgetActionType.SCREENSHOT }, 111)

                // 断开连接按钮
                val disconnectIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = WidgetActionReceiver.ACTION_WIDGET_DISCONNECT
                }
                views.setOnClickPendingIntent(
                    R.id.btn_disconnect_2x2,
                    PendingIntent.getBroadcast(context, 106, disconnectIntent, getPendingIntentFlags())
                )
            }
            WidgetStateHolder.State.CONNECTING -> {
                views.setImageViewResource(R.id.iv_status_dot, R.drawable.widget_status_dot_connecting)
                views.setTextViewText(R.id.tv_status_text, "连接中...")

                views.setViewVisibility(R.id.layout_disconnected_2x2, View.VISIBLE)
                views.setViewVisibility(R.id.layout_connected_2x2, View.GONE)
                bind2x2DeviceRows(context, views, recents)
            }
            WidgetStateHolder.State.DISCONNECTED -> {
                views.setImageViewResource(R.id.iv_status_dot, R.drawable.widget_status_dot_disconnected)
                views.setTextViewText(R.id.tv_status_text, "未连接")

                views.setViewVisibility(R.id.layout_disconnected_2x2, View.VISIBLE)
                views.setViewVisibility(R.id.layout_connected_2x2, View.GONE)
                bind2x2DeviceRows(context, views, recents)
            }
        }

        return views
    }

    private fun bind2x2DeviceRows(context: Context, views: RemoteViews, recents: List<WidgetDeviceInfo>) {
        val rowIds = listOf(
            Triple(R.id.item_dev_1_2x2, R.id.tv_dev_name_1_2x2, R.id.tv_dev_action_1_2x2),
            Triple(R.id.item_dev_2_2x2, R.id.tv_dev_name_2_2x2, R.id.tv_dev_action_2_2x2),
            Triple(R.id.item_dev_3_2x2, R.id.tv_dev_name_3_2x2, R.id.tv_dev_action_3_2x2),
            Triple(R.id.item_dev_4_2x2, R.id.tv_dev_name_4_2x2, R.id.tv_dev_action_4_2x2),
            Triple(R.id.item_dev_5_2x2, R.id.tv_dev_name_5_2x2, R.id.tv_dev_action_5_2x2)
        )

        for (i in 0 until 5) {
            val (itemId, nameId, actionId) = rowIds[i]
            if (i < recents.size) {
                val d = recents[i]
                views.setViewVisibility(itemId, View.VISIBLE)
                views.setTextViewText(nameId, d.name)
                views.setTextViewText(actionId, "连接")
                views.setOnClickPendingIntent(
                    itemId,
                    createConnectDevicePendingIntent(context, d.ip, d.port, d.name, 120 + i, d.serialNo)
                )
            } else if (i == recents.size && i == 0) {
                views.setViewVisibility(itemId, View.VISIBLE)
                views.setTextViewText(nameId, "快速添加设备")
                views.setTextViewText(actionId, "添加")
                views.setOnClickPendingIntent(itemId, createOpenAppPendingIntent(context, 120 + i))
            } else if (i == recents.size && i < 4) {
                views.setViewVisibility(itemId, View.VISIBLE)
                views.setTextViewText(nameId, "局域网扫描新设备")
                views.setTextViewText(actionId, "扫描")
                views.setOnClickPendingIntent(itemId, createOpenAppPendingIntent(context, 120 + i))
            } else {
                views.setViewVisibility(itemId, View.GONE)
            }
        }
    }

    private fun bind2x2ActionSlot(
        context: Context,
        views: RemoteViews,
        btnId: Int,
        ivId: Int,
        tvId: Int,
        action: WidgetActionType,
        requestCode: Int
    ) {
        views.setImageViewResource(ivId, action.iconResId)
        views.setTextViewText(tvId, action.title)
        views.setOnClickPendingIntent(btnId, createActionPendingIntent(context, action, requestCode))
    }

    fun build4x2Views(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_control_4x2)
        val state = WidgetStateHolder.currentState
        val recents = WidgetStateHolder.recentDevices
        val name = WidgetStateHolder.deviceName.ifBlank { "已连接设备" }
        val ip = WidgetStateHolder.deviceIp.ifBlank { "点击连接设备" }

        // 1. 左侧面板状态与设备列表切换
        if (state == WidgetStateHolder.State.CONNECTED) {
            views.setViewVisibility(R.id.layout_disconnected_panel_4x2, View.GONE)
            views.setViewVisibility(R.id.layout_connected_panel_4x2, View.VISIBLE)

            views.setImageViewResource(R.id.iv_status_dot_4x2, R.drawable.widget_status_dot_connected)
            val count = WidgetStateHolder.connectedDeviceCount
            val statusTitle = if (count > 1) "已连接 ($count)" else "已连接"
            views.setTextViewText(R.id.tv_status_text_4x2, statusTitle)

            views.setTextViewText(R.id.tv_device_name_4x2, name)
            views.setTextViewText(R.id.tv_device_ip_4x2, ip)

            // 点击设备名或设备区域进入 App 连接控制页面
            val openDeviceIntent = createOpenDevicePendingIntent(context, 205)
            views.setOnClickPendingIntent(R.id.layout_device_info_4x2, openDeviceIntent)
            views.setOnClickPendingIntent(R.id.tv_device_name_4x2, openDeviceIntent)
            views.setOnClickPendingIntent(R.id.tv_device_ip_4x2, openDeviceIntent)

            // 断开连接按钮
            val disconnectIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_WIDGET_DISCONNECT
            }
            views.setOnClickPendingIntent(
                R.id.btn_disconnect_4x2,
                PendingIntent.getBroadcast(context, 201, disconnectIntent, getPendingIntentFlags())
            )
        } else {
            views.setViewVisibility(R.id.layout_disconnected_panel_4x2, View.VISIBLE)
            views.setViewVisibility(R.id.layout_connected_panel_4x2, View.GONE)

            // 绑定最多 5 台快速连接设备
            bind4x2DeviceRows(context, views, recents)
        }

        // 2. 右侧 4 个动态可配置的快捷操作磁贴
        val actions = WidgetPreferences.get4x2Actions(context)
        bind4x2TileSlot(context, views, R.id.btn_tile_1, R.id.iv_tile_1, R.id.tv_tile_title_1, R.id.tv_tile_subtitle_1, actions.getOrElse(0) { WidgetActionType.LOCK }, 220)
        bind4x2TileSlot(context, views, R.id.btn_tile_2, R.id.iv_tile_2, R.id.tv_tile_title_2, R.id.tv_tile_subtitle_2, actions.getOrElse(1) { WidgetActionType.SCREENSHOT }, 221)
        bind4x2TileSlot(context, views, R.id.btn_tile_3, R.id.iv_tile_3, R.id.tv_tile_title_3, R.id.tv_tile_subtitle_3, actions.getOrElse(2) { WidgetActionType.MIRROR }, 222)
        bind4x2TileSlot(context, views, R.id.btn_tile_4, R.id.iv_tile_4, R.id.tv_tile_title_4, R.id.tv_tile_subtitle_4, actions.getOrElse(3) { WidgetActionType.HOME }, 223)

        return views
    }

    private fun bind4x2DeviceRows(context: Context, views: RemoteViews, recents: List<WidgetDeviceInfo>) {
        val rowIds = listOf(
            Triple(R.id.item_dev_1_4x2, R.id.tv_dev_name_1_4x2, R.id.tv_dev_ip_1_4x2),
            Triple(R.id.item_dev_2_4x2, R.id.tv_dev_name_2_4x2, R.id.tv_dev_ip_2_4x2),
            Triple(R.id.item_dev_3_4x2, R.id.tv_dev_name_3_4x2, R.id.tv_dev_ip_3_4x2),
            Triple(R.id.item_dev_4_4x2, R.id.tv_dev_name_4_4x2, R.id.tv_dev_ip_4_4x2),
            Triple(R.id.item_dev_5_4x2, R.id.tv_dev_name_5_4x2, R.id.tv_dev_ip_5_4x2)
        )

        for (i in 0 until 5) {
            val (itemId, nameId, actionId) = rowIds[i]
            if (i < recents.size) {
                val d = recents[i]
                views.setViewVisibility(itemId, View.VISIBLE)
                views.setTextViewText(nameId, d.name)
                views.setTextViewText(actionId, "连接")
                views.setOnClickPendingIntent(
                    itemId,
                    createConnectDevicePendingIntent(context, d.ip, d.port, d.name, 210 + i, d.serialNo)
                )
            } else if (i == recents.size && i == 0) {
                views.setViewVisibility(itemId, View.VISIBLE)
                views.setTextViewText(nameId, "暂无保存设备")
                views.setTextViewText(actionId, "点击添加")
                views.setOnClickPendingIntent(itemId, createOpenAppPendingIntent(context, 210 + i))
            } else if (i == recents.size && i < 4) {
                views.setViewVisibility(itemId, View.VISIBLE)
                views.setTextViewText(nameId, "局域网扫描新设备")
                views.setTextViewText(actionId, "扫描")
                views.setOnClickPendingIntent(itemId, createOpenAppPendingIntent(context, 210 + i))
            } else {
                views.setViewVisibility(itemId, View.GONE)
            }
        }
    }

    private fun bind4x2TileSlot(
        context: Context,
        views: RemoteViews,
        btnId: Int,
        ivId: Int,
        titleId: Int,
        subTitleId: Int,
        action: WidgetActionType,
        requestCode: Int
    ) {
        views.setImageViewResource(ivId, action.iconResId)
        views.setTextViewText(titleId, action.title)
        views.setTextViewText(subTitleId, action.subtitle)
        views.setOnClickPendingIntent(btnId, createActionPendingIntent(context, action, requestCode))
    }

    private fun createActionPendingIntent(context: Context, action: WidgetActionType, requestCode: Int): PendingIntent {
        return when (action) {
            WidgetActionType.MIRROR -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    this.action = ACTION_WIDGET_MIRROR
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                PendingIntent.getActivity(context, requestCode, intent, getPendingIntentFlags())
            }
            WidgetActionType.FILE_EXPLORER -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    this.action = ACTION_WIDGET_FILE
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                PendingIntent.getActivity(context, requestCode, intent, getPendingIntentFlags())
            }
            WidgetActionType.TERMINAL -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    this.action = ACTION_WIDGET_TERMINAL
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                PendingIntent.getActivity(context, requestCode, intent, getPendingIntentFlags())
            }
            else -> {
                val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                    this.action = WidgetActionReceiver.ACTION_WIDGET_EXECUTE_ACTION
                    putExtra(WidgetActionReceiver.EXTRA_ACTION_ID, action.id)
                }
                PendingIntent.getBroadcast(context, requestCode, intent, getPendingIntentFlags())
            }
        }
    }

    private fun createConnectDevicePendingIntent(
        context: Context,
        ip: String,
        port: Int,
        name: String,
        requestCode: Int,
        serialNo: String = ""
    ): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_WIDGET_CONNECT_DEVICE
            putExtra(WidgetActionReceiver.EXTRA_DEVICE_IP, ip)
            putExtra(WidgetActionReceiver.EXTRA_DEVICE_PORT, port)
            putExtra(WidgetActionReceiver.EXTRA_DEVICE_NAME, name)
            putExtra(WidgetActionReceiver.EXTRA_DEVICE_SERIAL, serialNo)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, getPendingIntentFlags())
    }

    private fun createOpenAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, getPendingIntentFlags())
    }

    private fun createOpenDevicePendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_WIDGET_OPEN_DEVICE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, getPendingIntentFlags())
    }

    private fun getPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    fun isPinWidgetSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return false
            return appWidgetManager.isRequestPinAppWidgetSupported
        }
        return false
    }

    fun requestPinWidget(context: Context, is2x2: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return false
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = if (is2x2) {
                    ComponentName(context, QuickConnectWidgetProvider2x2::class.java)
                } else {
                    ComponentName(context, ControlCenterWidgetProvider4x2::class.java)
                }
                return try {
                    appWidgetManager.requestPinAppWidget(provider, null, null)
                } catch (_: Exception) {
                    false
                }
            }
        }
        return false
    }
}
