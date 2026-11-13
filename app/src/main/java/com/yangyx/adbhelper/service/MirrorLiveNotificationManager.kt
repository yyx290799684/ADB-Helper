package com.yangyx.adbhelper.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yangyx.adbhelper.MainActivity
import com.yangyx.adbhelper.R
import kotlin.math.roundToInt

data class MirrorLiveNotificationInfo(
    val sessionId: String,
    val deviceName: String,
    val endpoint: String,
    val modeLabel: String,
    val shortCriticalText: String,
    val isConnecting: Boolean = false,
    val isPausedResumable: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Float = 0f,
    val latencyMs: Long = 0L,
    val videoCodec: String = "h264",
    val isAudioEnabled: Boolean = false,
    val virtualPackage: String = "",
    val startedAtMs: Long = System.currentTimeMillis()
)

object MirrorLiveNotificationManager {
    const val CHANNEL_ID = "mirror_live_update_channel"
    const val NOTIFICATION_ID = 1002

    const val ACTION_RESTORE_MIRROR_SESSION = "com.yangyx.adbhelper.action.RESTORE_MIRROR_SESSION"
    const val ACTION_STOP_MIRROR_FROM_NOTIFICATION = "com.yangyx.adbhelper.action.STOP_MIRROR_FROM_NOTIFICATION"
    const val ACTION_DISMISS_MIRROR_NOTIFICATION = "com.yangyx.adbhelper.action.DISMISS_MIRROR_NOTIFICATION"
    const val EXTRA_SESSION_ID = "extra_mirror_session_id"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null || existing.importance == NotificationManager.IMPORTANCE_MIN) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "投屏实时动态通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "在应用最小化期间展示投屏会话连接状态并支持一键恢复"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showOrUpdate(context: Context, info: MirrorLiveNotificationInfo) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val restoreIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_RESTORE_MIRROR_SESSION
            putExtra(EXTRA_SESSION_ID, info.sessionId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val restoreFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val restorePendingIntent = PendingIntent.getActivity(
            context,
            2001,
            restoreIntent,
            restoreFlags
        )

        val stopIntent = Intent(context, MirrorNotificationActionReceiver::class.java).apply {
            action = ACTION_STOP_MIRROR_FROM_NOTIFICATION
            putExtra(EXTRA_SESSION_ID, info.sessionId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            stopIntent,
            restoreFlags
        )

        val dismissIntent = Intent(context, MirrorNotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_MIRROR_NOTIFICATION
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            2003,
            dismissIntent,
            restoreFlags
        )

        val safeDevice = info.deviceName.ifBlank { "ADB 设备" }
        val codecUpper = info.videoCodec.uppercase()
        val audioTag = if (info.isAudioEnabled) "音频开" else "静音"
        val fpsText = if (info.fps > 0f) "${info.fps.roundToInt()}fps" else "--fps"
        val resText = if (info.width > 0 && info.height > 0) "${info.width}×${info.height}" else "分辨率协商中"

        val title = when {
            info.isConnecting -> "正在建立投屏 · $safeDevice"
            info.isPausedResumable -> "投屏会话待恢复 · $safeDevice"
            info.modeLabel.startsWith("虚拟副屏") -> "虚拟副屏投屏中 · $safeDevice"
            info.modeLabel.startsWith("摄像头") -> "摄像头投屏中 · $safeDevice"
            else -> "屏幕投屏中 · $safeDevice"
        }

        val summaryText = when {
            info.isConnecting -> "正在握手视频通道 (${info.endpoint}) · 点击查看"
            info.isPausedResumable -> "后台超时已暂停画面 · 点击恢复投屏连接"
            else -> "${info.modeLabel} · $resText · $fpsText · ${info.latencyMs}ms"
        }

        val expandedLines = buildList {
            add("设备：$safeDevice (${info.endpoint})")
            add("模式：${info.modeLabel}")
            if (info.virtualPackage.isNotBlank()) {
                add("副屏应用：${info.virtualPackage}")
            }
            when {
                info.isConnecting -> add("状态：正在初始化 scrcpy 服务端与视频解码器")
                info.isPausedResumable -> add("状态：ADB 会话仍在线，画面流已暂停以节省电量与带宽")
                else -> add("参数：$resText · $fpsText · 延迟 ${info.latencyMs}ms · $codecUpper · $audioTag")
            }
            add("操作：点击通知即可回到应用并恢复实时投屏")
        }.joinToString("\n")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mirror_live)
            .setLargeIcon(getAppIconBitmap(context))
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(expandedLines)
            )
            .setContentIntent(restorePendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(info.shortCriticalText)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(info.startedAtMs)
            .setUsesChronometer(!info.isConnecting && !info.isPausedResumable)
            .addAction(
                R.drawable.ic_mirror_live,
                if (info.isPausedResumable) "恢复投屏" else "恢复画面",
                restorePendingIntent
            )

        if (!info.isPausedResumable) {
            builder.addAction(
                R.drawable.ic_widget_disconnect,
                "停止投屏",
                stopPendingIntent
            )
        }

        val notification = builder.build()
        val activeService = AdbSessionService.activeInstance
        if (activeService != null) {
            activeService.showMirrorLiveForeground(notification)
        } else {
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
    }

    @Volatile
    private var cachedAppIconBitmap: Bitmap? = null

    private fun getAppIconBitmap(context: Context): Bitmap? {
        cachedAppIconBitmap?.let { if (!it.isRecycled) return it }
        return try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
                ?: ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                ?: context.packageManager.getApplicationIcon(context.applicationInfo)
            val size = 128
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            cachedAppIconBitmap = bitmap
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun cancel(context: Context, restoreSessionForeground: Boolean = false) {
        if (restoreSessionForeground) {
            try {
                AdbSessionService.activeInstance?.restoreAdbSessionForeground()
            } catch (_: Exception) {}
        }
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }
}
