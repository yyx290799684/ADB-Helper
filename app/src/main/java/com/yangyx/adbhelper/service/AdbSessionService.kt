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
import com.yangyx.adbhelper.MainActivity
import com.yangyx.adbhelper.R

class AdbSessionService : Service() {

    companion object {
        const val CHANNEL_ID = "adb_session_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.yangyx.adbhelper.action.START_SESSION"
        const val ACTION_STOP = "com.yangyx.adbhelper.action.STOP_SESSION"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
        const val EXTRA_DEVICE_IP = "extra_device_ip"

        fun startService(context: Context, deviceName: String, deviceIp: String) {
            try {
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

        fun stopService(context: Context) {
            try {
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (_: Exception) {}
            return START_NOT_STICKY
        }

        val deviceName = intent?.getStringExtra(EXTRA_DEVICE_NAME) ?: "ADB 远程设备"
        val deviceIp = intent?.getStringExtra(EXTRA_DEVICE_IP) ?: ""

        val notification = buildNotification(deviceName, deviceIp)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        return START_STICKY
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
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ADB 调试助手运行中")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
