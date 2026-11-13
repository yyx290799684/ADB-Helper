package com.yangyx.adbhelper.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.yangyx.adbhelper.adb.AdbConnection
import com.yangyx.adbhelper.adb.AdbSyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 远程设备应用图标按设备+包名拉取与本地持久化缓存管理器
 * 图标由对端 app_process 服务生成在 /data/local/tmp/adbhelper_icons/<pkg>.png，
 * 客户端通过 ADB Sync (pullFile) 按需拉取几 KB 的 PNG 并缓存到 cacheDir/remote_app_icons/<deviceId>/。
 */
object AppIconCacheManager {

    private const val CACHE_DIR_NAME = "remote_app_icons"
    private const val SOURCE_MARKER_FILE = ".source_adb_pull_v1"

    private val pullSemaphore = Semaphore(3)

    // 内存缓存：按 "deviceId::packageName" 缓存 ImageBitmap
    private val memoryCache = object : LruCache<String, ImageBitmap>(300) {}

    // 会话级负缓存：记录本次会话中确认无法拉取或无图标的应用，避免滚动时重复拉取
    private val negativeMemoryCache = ConcurrentHashMap<String, Boolean>()

    @Volatile
    private var legacyCleaned = false

    // 缓存版本号，清除或刷新缓存后递增以通知 UI 重新评估加载
    private val _cacheVersion = MutableStateFlow(0)
    val cacheVersion: StateFlow<Int> = _cacheVersion.asStateFlow()

    // 当前磁盘已缓存总字节数（用于在 UI 展示）
    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private fun getRootCacheDir(context: Context): File {
        val dir = File(context.applicationContext.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        if (!legacyCleaned) {
            synchronized(this) {
                if (!legacyCleaned) {
                    val marker = File(dir, SOURCE_MARKER_FILE)
                    if (!marker.exists()) {
                        // 清理旧版本联网抓取遗留的根目录缓存文件
                        dir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                file.delete()
                            }
                        }
                        try {
                            marker.writeText("adb_pull_v1")
                        } catch (_: Exception) {
                        }
                    }
                    legacyCleaned = true
                }
            }
        }
        return dir
    }

    private fun safeSegment(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "default" }
    }

    private fun deviceCacheDir(context: Context, deviceId: String): File {
        val dir = File(getRootCacheDir(context), safeSegment(deviceId))
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun iconFile(context: Context, deviceId: String, packageName: String): File {
        return File(deviceCacheDir(context, deviceId), "${safeSegment(packageName)}.png")
    }

    private fun memKey(deviceId: String, packageName: String): String {
        return "${safeSegment(deviceId)}::$packageName"
    }

    fun getCachedMemory(deviceId: String, packageName: String): ImageBitmap? {
        if (packageName.isBlank()) return null
        return memoryCache.get(memKey(deviceId, packageName))
    }

    fun refreshCacheSize(context: Context): Long {
        val root = getRootCacheDir(context)
        var total = 0L
        root.walkTopDown().forEach { file ->
            if (file.isFile && file.name.endsWith(".png")) {
                total += file.length()
            }
        }
        _cacheSizeBytes.value = total
        return total
    }

    /**
     * 清除指定设备的会话级负缓存，并通知 UI 尝试拉取新生成的图标
     */
    fun notifyDeviceIconsRefreshed(deviceId: String) {
        val prefix = "${safeSegment(deviceId)}::"
        negativeMemoryCache.keys.removeIf { it.startsWith(prefix) }
        _cacheVersion.value += 1
    }

    /**
     * 清除所有设备的内存与磁盘图标缓存
     */
    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        val root = getRootCacheDir(context)
        var clearedBytes = 0L
        root.walkBottomUp().forEach { file ->
            if (file == root || file.name == SOURCE_MARKER_FILE) return@forEach
            if (file.isFile) {
                if (file.name.endsWith(".png")) {
                    clearedBytes += file.length()
                }
                file.delete()
            } else if (file.isDirectory) {
                file.delete()
            }
        }
        memoryCache.evictAll()
        negativeMemoryCache.clear()
        _cacheSizeBytes.value = 0L
        _cacheVersion.value += 1
        clearedBytes
    }

    /**
     * 加载应用图标：
     * 1. 读内存缓存
     * 2. 读按设备隔离的本地磁盘缓存 (<deviceId>/<pkg>.png)
     * 3. 若本地无缓存且远端存在图标，则通过 AdbSyncClient.pullFile 拉取小 PNG 并缓存
     */
    suspend fun loadOrPullIcon(
        context: Context,
        deviceId: String,
        packageName: String,
        remoteIconPath: String,
        hasRemoteIcon: Boolean,
        connectionProvider: () -> AdbConnection?
    ): ImageBitmap? {
        if (packageName.isBlank()) return null

        val key = memKey(deviceId, packageName)
        memoryCache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            val localFile = iconFile(context, deviceId, packageName)
            if (localFile.exists() && localFile.length() > 0L) {
                val bmp = decodeSampledBitmap(localFile.readBytes())
                if (bmp != null) {
                    val img = bmp.asImageBitmap()
                    memoryCache.put(key, img)
                    return@withContext img
                } else {
                    localFile.delete()
                }
            }

            if (!hasRemoteIcon || negativeMemoryCache.containsKey(key)) {
                return@withContext null
            }

            pullSemaphore.withPermit {
                memoryCache.get(key)?.let { return@withPermit it }
                if (negativeMemoryCache.containsKey(key)) return@withPermit null
                if (localFile.exists() && localFile.length() > 0L) {
                    val bmp = decodeSampledBitmap(localFile.readBytes())
                    if (bmp != null) {
                        val img = bmp.asImageBitmap()
                        memoryCache.put(key, img)
                        return@withPermit img
                    }
                }

                val conn = connectionProvider() ?: return@withPermit null
                if (!conn.isConnected) return@withPermit null

                val targetRemote = remoteIconPath.ifBlank {
                    "${RemoteAppServerManager.REMOTE_ICONS_DIR}/${safeSegment(packageName)}.png"
                }

                try {
                    val baos = ByteArrayOutputStream(8192)
                    val syncClient = AdbSyncClient(conn)
                    syncClient.pullFile(targetRemote, baos)
                    val imgBytes = baos.toByteArray()
                    val bmp = if (imgBytes.isNotEmpty()) decodeSampledBitmap(imgBytes) else null
                    if (bmp != null) {
                        try {
                            val tmpFile = File(localFile.parentFile, "${localFile.name}.tmp")
                            tmpFile.writeBytes(imgBytes)
                            if (localFile.exists()) localFile.delete()
                            tmpFile.renameTo(localFile)
                            refreshCacheSize(context)
                        } catch (_: Exception) {
                        }
                        val img = bmp.asImageBitmap()
                        memoryCache.put(key, img)
                        return@withPermit img
                    } else {
                        negativeMemoryCache[key] = true
                        null
                    }
                } catch (_: Exception) {
                    if (conn.isConnected) {
                        negativeMemoryCache[key] = true
                    }
                    null
                }
            }
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var sampleSize = 1
            val targetMaxPx = 192
            while (boundsOpts.outWidth / sampleSize > targetMaxPx * 2 ||
                boundsOpts.outHeight / sampleSize > targetMaxPx * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        } catch (_: Exception) {
            null
        }
    }
}
