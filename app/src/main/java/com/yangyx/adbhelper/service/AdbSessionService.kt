package com.yangyx.adbhelper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yangyx.adbhelper.MainActivity
import com.yangyx.adbhelper.R
import com.yangyx.adbhelper.ui.AdbViewModel

class AdbSessionService : Service() {

    companion object {
        const val CHANNEL_ID = "adb_session_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.yangyx.adbhelper.action.START_SESSION"
        const val ACTION_STOP = "com.yangyx.adbhelper.action.STOP_SESSION"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
        const val EXTRA_DEVICE_IP = "extra_device_ip"

        @Volatile
        var activeInstance: AdbSessionService? = null
            private set

        @Volatile
        private var explicitlyStopping: Boolean = false

        fun startService(context: Context, deviceName: String, deviceIp: String) {
            try {
                explicitlyStopping = false
                val intent = Intent(context, AdbSessionService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_DEVICE_NAME, deviceName)
                    putExtra(EXTRA_DEVICE_IP, deviceIp)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun startConnectingService(context: Context, deviceName: String) {
            startService(context, deviceName, "正在依次探测与连接...")
        }

        fun stopService(context: Context) {
            try {
                explicitlyStopping = true
                val intent = Intent(context, AdbSessionService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var lastDeviceName: String = "ADB 远程设备"
    private var lastDeviceIp: String = ""
    @Volatile
    private var isShowingMirrorLiveAsForeground: Boolean = false

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            explicitlyStopping = true
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (_: Exception) {}
            return START_NOT_STICKY
        }

        explicitlyStopping = false
        lastDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "ADB 远程设备"
        lastDeviceIp = intent.getStringExtra(EXTRA_DEVICE_IP) ?: ""

        if (!isShowingMirrorLiveAsForeground) {
            startForegroundCompat(NOTIFICATION_ID, buildNotification(lastDeviceName, lastDeviceIp))
        }

        return START_NOT_STICKY
    }

    fun showMirrorLiveForeground(notification: Notification) {
        try {
            if (!isShowingMirrorLiveAsForeground) {
                isShowingMirrorLiveAsForeground = true
                startForegroundCompat(MirrorLiveNotificationManager.NOTIFICATION_ID, notification)
                try {
                    NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
                } catch (_: Exception) {}
            } else {
                try {
                    NotificationManagerCompat.from(this).notify(
                        MirrorLiveNotificationManager.NOTIFICATION_ID,
                        notification
                    )
                } catch (_: SecurityException) {
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun restoreAdbSessionForeground() {
        if (!isShowingMirrorLiveAsForeground) {
            try {
                NotificationManagerCompat.from(this).cancel(MirrorLiveNotificationManager.NOTIFICATION_ID)
            } catch (_: Exception) {}
            return
        }
        isShowingMirrorLiveAsForeground = false
        startForegroundCompat(NOTIFICATION_ID, buildNotification(lastDeviceName, lastDeviceIp))
        try {
            NotificationManagerCompat.from(this).cancel(MirrorLiveNotificationManager.NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    id,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(id, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(id, notification)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        explicitlyStopping = true
        try {
            AdbViewModel.currentInstance?.onAppTaskRemoved(fromServiceDestroy = true)
        } catch (_: Exception) {}
        cancelAllServiceNotifications()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (_: Exception) {}
    }

    private fun cancelAllServiceNotifications() {
        try {
            NotificationManagerCompat.from(applicationContext).cancel(MirrorLiveNotificationManager.NOTIFICATION_ID)
        } catch (_: Exception) {}
        try {
            NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AdbHelper:SessionWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // Max 24 hours
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager?.createWifiLock(
                lockMode,
                "AdbHelper:SessionWifiLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        try {
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ADB 会话后台保活服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持 ADB 调试与投屏在后台稳定连接"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(deviceName: String, deviceIp: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val content = if (deviceIp.isNotBlank()) "正在与 $deviceName ($deviceIp) 保持后台 ADB 会话" else "正在与 $deviceName 保持后台 ADB 会话"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mirror_live)
            .setContentTitle("ADB 调试助手运行中")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        isShowingMirrorLiveAsForeground = false
        val stoppedBySystem = !explicitlyStopping
        explicitlyStopping = false
        if (stoppedBySystem) {
            try {
                AdbViewModel.currentInstance?.onAppTaskRemoved(fromServiceDestroy = true)
            } catch (_: Exception) {}
        }
        cancelAllServiceNotifications()
        super.onDestroy()
        releaseLocks()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
