package com.yangyx.adbhelper.scrcpy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.yangyx.adbhelper.adb.AdbConnection
import com.yangyx.adbhelper.adb.AdbStream
import com.yangyx.adbhelper.adb.AdbSyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { INFO, SUCCESS, WARN, ERROR }

data class ScrcpyLog(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

sealed class ScreenState {
    object Idle : ScreenState()
    object Connecting : ScreenState()
    data class Streaming(
        val width: Int,
        val height: Int,
        val fps: Float = 0f,
        val latencyMs: Long = 0L,
        val bitmap: Bitmap? = null
    ) : ScreenState()
    data class Error(val message: String) : ScreenState()
}

class ScrcpyController(
    private val connection: AdbConnection,
    private val scope: CoroutineScope,
    private val context: Context? = null
) {
    var activeSurface: Surface? = null
        private set
    private var currentDecoder: H264StreamDecoder? = null
    private var controlStream: AdbStream? = null

    fun setSurface(surface: Surface?) {
        activeSurface = surface
        if (surface != null && surface.isValid) {
            currentDecoder?.setSurface(surface)
        }
    }
    val isFullscreen = MutableStateFlow(false)
    val isVirtualDisplayActive = MutableStateFlow(false)
    var activeVirtualWidth: Int = 1080
    var activeVirtualHeight: Int = 1920
    var activeVirtualPackage: String = ""

    fun createVirtualDisplayAndLaunch(width: Int, height: Int, packageName: String) {
        scope.launch(Dispatchers.IO) {
            try {
                addLog("用户主动创建独立虚拟副屏 (${width}x${height})${if (packageName.isNotBlank()) "，启动应用: $packageName" else ""}...", LogLevel.INFO)
                
                activeVirtualWidth = width
                activeVirtualHeight = height
                activeVirtualPackage = packageName
                isVirtualDisplayActive.value = true

                stopMirroring()
                // Wait for cleanup
                kotlinx.coroutines.delay(500)
                startMirroring()
                
                // wait for scrcpy-server to start streaming and create the display
                kotlinx.coroutines.delay(2500)
                
                val vId = findLatestVirtualDisplayId()
                virtualDisplayId = vId
                
                if (packageName.isNotBlank()) {
                    launchPackageOnDisplay(packageName, vId)
                }

                addLog("已成功关联并激活系统虚拟屏 ID: $vId (${width}x${height})", LogLevel.SUCCESS)
            } catch (e: Exception) {
                addLog("创建虚拟副屏发生异常: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    fun switchToMainDisplay() {
        scope.launch(Dispatchers.IO) {
            try {
                addLog("正在切回被控端物理主屏...", LogLevel.INFO)
                isVirtualDisplayActive.value = false
                virtualDisplayId = -1
                activeVirtualPackage = ""
                stopMirroring()
                kotlinx.coroutines.delay(500)
                startMirroring()
            } catch (e: Exception) {
                addLog("切回物理主屏发生异常: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    fun toggleFullscreen() {
        isFullscreen.value = !isFullscreen.value
    }

    fun setFullscreen(enabled: Boolean) {
        isFullscreen.value = enabled
    }

    private val PREFS_NAME = "scrcpy_settings_prefs"

    private fun loadConfig(): ScrcpyConfig {
        if (context == null) return ScrcpyConfig()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ScrcpyConfig(
            bitrate = prefs.getInt("bitrate", 4000000),
            maxResolution = prefs.getInt("maxResolution", 1080),
            maxFps = prefs.getInt("maxFps", 60),
            videoCodec = prefs.getString("videoCodec", "h264") ?: "h264",
            isScreenOff = prefs.getBoolean("isScreenOff", false),
            isStayAwake = prefs.getBoolean("isStayAwake", true),
            isAudioEnabled = prefs.getBoolean("isAudioEnabled", false),
            virtualDisplayWidth = prefs.getInt("virtualDisplayWidth", 0),
            virtualDisplayHeight = prefs.getInt("virtualDisplayHeight", 0),
            launchPackageOnVirtualDisplay = prefs.getString("launchPackageOnVirtualDisplay", "") ?: "",
            videoSource = prefs.getString("videoSource", "display") ?: "display",
            cameraFacing = prefs.getString("cameraFacing", "back") ?: "back",
            cameraId = prefs.getString("cameraId", "") ?: "",
            cameraSize = prefs.getString("cameraSize", "") ?: "",
            cameraAspectRatio = prefs.getString("cameraAspectRatio", "") ?: "",
            cameraFps = prefs.getInt("cameraFps", 0),
            cameraHighSpeed = prefs.getBoolean("cameraHighSpeed", false),
            cameraTorch = prefs.getBoolean("cameraTorch", false),
            cameraZoom = prefs.getFloat("cameraZoom", 1.0f)
        )
    }

    private fun saveConfig(cfg: ScrcpyConfig) {
        if (context == null) return
        scope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("bitrate", cfg.bitrate)
                .putInt("maxResolution", cfg.maxResolution)
                .putInt("maxFps", cfg.maxFps)
                .putString("videoCodec", cfg.videoCodec)
                .putBoolean("isScreenOff", cfg.isScreenOff)
                .putBoolean("isStayAwake", cfg.isStayAwake)
                .putBoolean("isAudioEnabled", cfg.isAudioEnabled)
                .putInt("virtualDisplayWidth", cfg.virtualDisplayWidth)
                .putInt("virtualDisplayHeight", cfg.virtualDisplayHeight)
                .putString("launchPackageOnVirtualDisplay", cfg.launchPackageOnVirtualDisplay)
                .putString("videoSource", cfg.videoSource)
                .putString("cameraFacing", cfg.cameraFacing)
                .putString("cameraId", cfg.cameraId)
                .putString("cameraSize", cfg.cameraSize)
                .putString("cameraAspectRatio", cfg.cameraAspectRatio)
                .putInt("cameraFps", cfg.cameraFps)
                .putBoolean("cameraHighSpeed", cfg.cameraHighSpeed)
                .putBoolean("cameraTorch", cfg.cameraTorch)
                .putFloat("cameraZoom", cfg.cameraZoom)
                .apply()
        }
    }

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Idle)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ScrcpyConfig> = _config.asStateFlow()

    private val _logs = MutableStateFlow<List<ScrcpyLog>>(emptyList())
    val logs: StateFlow<List<ScrcpyLog>> = _logs.asStateFlow()

    private var streamingJob: Job? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    var nativeWidth: Int = 1080
        private set
    @Volatile
    var nativeHeight: Int = 2400
        private set

    @Volatile
    var remoteWidth: Int = 1080
        private set
    @Volatile
    var remoteHeight: Int = 2400
        private set

    @Volatile
    var virtualDisplayId: Int = -1
        private set

    fun addLog(message: String, level: LogLevel = LogLevel.INFO, tag: String = "Scrcpy") {
        val timeStr = dateFormat.format(Date())
        val logItem = ScrcpyLog(timeStr, level, tag, message)
        Log.d("ScrcpyController", "[$timeStr][$level] $message")
        _logs.value = (_logs.value + logItem).takeLast(200)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun copyLogsToClipboard(context: Context) {
        val text = _logs.value.joinToString("\n") { "[${it.timestamp}][${it.level}] ${it.message}" }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("scrcpy_logs", text)
        clipboard?.setPrimaryClip(clip)
        addLog("已复制运行日志到剪贴板", LogLevel.SUCCESS)
    }

    fun updateConfig(newConfig: ScrcpyConfig) {
        val oldConfig = _config.value
        _config.value = newConfig
        saveConfig(newConfig)
        val sourceDesc = if (newConfig.videoSource == "camera") "摄像头(${newConfig.cameraFacing})" else "屏幕"
        addLog("配置已更新: 源=$sourceDesc, 分辨率=${if(newConfig.maxResolution==0)"原生" else "${newConfig.maxResolution}p"}, 帧率=${newConfig.maxFps}FPS, 码率=${newConfig.bitrate/1000000}Mbps, 息屏=${newConfig.isScreenOff}, 阻止休眠=${newConfig.isStayAwake}, 音频转发=${newConfig.isAudioEnabled}", LogLevel.INFO)
        if (_screenState.value is ScreenState.Streaming || _screenState.value is ScreenState.Connecting) {
            val streamRestartNeeded = oldConfig.isAudioEnabled != newConfig.isAudioEnabled ||
                    oldConfig.videoSource != newConfig.videoSource ||
                    oldConfig.cameraFacing != newConfig.cameraFacing ||
                    oldConfig.cameraId != newConfig.cameraId ||
                    oldConfig.cameraSize != newConfig.cameraSize ||
                    oldConfig.cameraAspectRatio != newConfig.cameraAspectRatio ||
                    oldConfig.cameraFps != newConfig.cameraFps ||
                    oldConfig.cameraHighSpeed != newConfig.cameraHighSpeed ||
                    oldConfig.cameraTorch != newConfig.cameraTorch ||
                    oldConfig.videoCodec != newConfig.videoCodec

            if (streamRestartNeeded) {
                scope.launch(Dispatchers.IO) {
                    stopMirroring()
                    startMirroring()
                }
            } else {
                applyRemoteSettings(newConfig, oldConfig)
            }
        }
    }

    fun wakeUpRemoteScreen() {
        scope.launch(Dispatchers.IO) {
            wakeUpScreenInternal()
        }
    }

    private suspend fun wakeUpScreenInternal() {
        addLog("正在唤醒被控端屏幕并尝试解锁...", LogLevel.INFO)
        try {
            // 1. KEYCODE_WAKEUP (224)
            connection.executeShell("input keyevent 224")
            addLog("已发送 KEYCODE_WAKEUP (224) 唤醒指令", LogLevel.SUCCESS)

            // 2. Dismiss keyguard
            connection.executeShell("wm dismiss-keyguard")
            addLog("已发送 wm dismiss-keyguard 移除锁屏保护", LogLevel.SUCCESS)
        } catch (e: Exception) {
            addLog("唤醒被控端屏幕时遇到警告: ${e.message}", LogLevel.WARN)
        }
    }

    fun listAvailableCameras(onResult: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                ensureScrcpyServerPushed()
                val serverVersion = ScrcpyServerManager.state.value.localVersion?.removePrefix("v")?.trim()?.ifEmpty { "3.1" } ?: "3.1"
                addLog("正在查询被控端所有摄像头信息 (list_cameras=true)...", LogLevel.INFO)
                val output = connection.executeShell("CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server $serverVersion list_cameras=true")
                addLog("被控端摄像头列表:\n$output", LogLevel.SUCCESS)
                withContext(Dispatchers.Main) {
                    onResult(if (output.isNotBlank()) output.trim() else "未获取到摄像头信息或设备不支持")
                }
            } catch (e: Exception) {
                addLog("查询摄像头信息失败: ${e.message}", LogLevel.ERROR)
                withContext(Dispatchers.Main) {
                    onResult("查询失败: ${e.message}")
                }
            }
        }
    }

    fun listAvailableCameraSizes(cameraId: String = "", onResult: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                ensureScrcpyServerPushed()
                val serverVersion = ScrcpyServerManager.state.value.localVersion?.removePrefix("v")?.trim()?.ifEmpty { "3.1" } ?: "3.1"
                val camArg = if (cameraId.isNotBlank()) " camera_id=$cameraId" else ""
                addLog("正在查询被控端摄像头支持的分辨率尺寸 (list_camera_sizes=true$camArg)...", LogLevel.INFO)
                val output = connection.executeShell("CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server $serverVersion list_camera_sizes=true$camArg")
                addLog("被控端摄像头分辨率列表:\n$output", LogLevel.SUCCESS)
                withContext(Dispatchers.Main) {
                    onResult(if (output.isNotBlank()) output.trim() else "未获取到相机尺寸信息或设备不支持")
                }
            } catch (e: Exception) {
                addLog("查询摄像头尺寸失败: ${e.message}", LogLevel.ERROR)
                withContext(Dispatchers.Main) {
                    onResult("查询失败: ${e.message}")
                }
            }
        }
    }

    fun startMirroring() {
        if (streamingJob?.isActive == true) return
        _screenState.value = ScreenState.Connecting
        val cfg = _config.value
        val isCamera = cfg.videoSource.equals("camera", ignoreCase = true)
        addLog(if (isCamera) "开始启动摄像头捕获与画面串流流程..." else "开始启动屏幕投屏流程...", LogLevel.INFO)

        streamingJob = scope.launch(Dispatchers.IO) {
            try {
                if (!isCamera) {
                    // 1. Wake up remote device screen if locked
                    wakeUpScreenInternal()

                    // 2. Get remote native screen resolution
                    fetchRemoteResolution()
                }

                // 3. Push official Genymobile scrcpy-server.jar
                ensureScrcpyServerPushed()

                // 4. Apply initial settings
                applyRemoteSettingsInternal(_config.value, null)

                // Delete any residual overlay_display_devices to avoid floating windows on target device
                try {
                    connection.executeShell("settings delete global overlay_display_devices")
                } catch (_: Exception) {}

                // 5. Calculate stream dimensions
                val maxRes = if (cfg.maxResolution > 0) cfg.maxResolution else 0
                val isPortrait = nativeHeight >= nativeWidth
                val aspect = if (nativeHeight > 0 && nativeWidth > 0) {
                    if (isPortrait) nativeHeight.toFloat() / nativeWidth.toFloat() else nativeWidth.toFloat() / nativeHeight.toFloat()
                } else 2.2f

                val (renderW, renderH) = when {
                    maxRes == 0 -> {
                        val w = if (nativeWidth > 0) nativeWidth else 720
                        val h = if (nativeHeight > 0) nativeHeight else 1600
                        (w and 0xFFFE) to (h and 0xFFFE)
                    }
                    isPortrait -> {
                        val h = maxRes and 0xFFFE
                        val w = (h / aspect).toInt() and 0xFFFE
                        w to h
                    }
                    else -> {
                        val w = maxRes and 0xFFFE
                        val h = (w / aspect).toInt() and 0xFFFE
                        w to h
                    }
                }

                val cameraParams = if (isCamera) {
                    val sb = StringBuilder(" video_source=camera")
                    if (cfg.cameraId.isNotBlank()) {
                        sb.append(" camera_id=${cfg.cameraId.trim()}")
                    } else if (cfg.cameraFacing.isNotBlank() && cfg.cameraFacing != "any") {
                        sb.append(" camera_facing=${cfg.cameraFacing.lowercase()}")
                    }
                    if (cfg.cameraSize.isNotBlank()) {
                        sb.append(" camera_size=${cfg.cameraSize.trim()}")
                    } else if (cfg.cameraAspectRatio.isNotBlank()) {
                        sb.append(" camera_ar=${cfg.cameraAspectRatio.trim()}")
                    }
                    if (cfg.cameraFps > 0) {
                        sb.append(" camera_fps=${cfg.cameraFps}")
                    }
                    if (cfg.cameraHighSpeed) {
                        sb.append(" camera_high_speed=true")
                    }
                    if (cfg.cameraTorch) {
                        sb.append(" camera_torch=true")
                    }
                    if (cfg.cameraZoom > 1.0f) {
                        sb.append(" camera_zoom=${cfg.cameraZoom}")
                    }
                    sb.toString()
                } else {
                    " video_source=display"
                }

                val stayAwakeParam = if (cfg.isStayAwake) " stay_awake=true" else ""
                val turnScreenOffParam = if (cfg.isScreenOff && !isCamera) " turn_screen_off=true" else ""
                val powerOnParam = if (!cfg.isPowerOn) " power_on=false" else ""
                val powerOffOnCloseParam = if (cfg.isPowerOffOnClose) " power_off_on_close=true" else ""

                val audioParam = if (cfg.isAudioEnabled) {
                    if (isCamera) " audio=true audio_codec=raw audio_source=mic" else " audio=true audio_codec=raw audio_source=output"
                } else {
                    " audio=false"
                }
                val displayParam = if (!isCamera) {
                    if (isVirtualDisplayActive.value) " new_display=${activeVirtualWidth}x${activeVirtualHeight}/240 vd_system_decorations=false" else " display_id=0"
                } else ""

                val codecParam = when (cfg.videoCodec.lowercase()) {
                    "h265", "hevc" -> " video_codec=h265"
                    "av1", "av01" -> " video_codec=av1"
                    else -> " video_codec=h264"
                }
                val serverVersion = ScrcpyServerManager.state.value.localVersion?.removePrefix("v")?.trim()?.ifEmpty { "3.1" } ?: "3.1"
                // scrcpy 2.0+ official parameters:
                val serverCmd = "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server $serverVersion log_level=info max_size=$maxRes video_bit_rate=${cfg.bitrate} max_fps=${cfg.maxFps}$codecParam$cameraParams$stayAwakeParam$turnScreenOffParam$powerOnParam$powerOffOnCloseParam video_codec_options=i-frame-interval=1 tunnel_forward=true control=true send_dummy_byte=true send_device_meta=true send_frame_meta=true$audioParam cleanup=false$displayParam"
                addLog("启动官方 scrcpy-server (v$serverVersion, ${cfg.videoCodec}, ${if (isCamera) "Camera" else "Display"}) 进程: $serverCmd", LogLevel.INFO)

                var serverProcessStream: AdbStream? = null
                try {
                    serverProcessStream = connection.openStream("shell:$serverCmd")
                    addLog("scrcpy-server 进程启动命令已发送", LogLevel.SUCCESS)

                    val procStream = serverProcessStream
                    scope.launch(Dispatchers.IO) {
                        try {
                            val reader = procStream.asInputStream().bufferedReader(Charsets.UTF_8)
                            while (streamingJob?.isActive == true && !procStream.isClosed) {
                                val line = reader.readLine() ?: break
                                if (line.isNotBlank()) {
                                    addLog("[scrcpy-server] $line", LogLevel.INFO)
                                }
                            }
                        } catch (e: Exception) {
                            addLog("[scrcpy-server] 进程输出流关闭: ${e.message}", LogLevel.INFO)
                        }
                    }
                } catch (e: Exception) {
                    addLog("启动 scrcpy-server 命令失败: ${e.message}", LogLevel.ERROR)
                    _screenState.value = ScreenState.Error("启动 scrcpy-server 进程失败: ${e.message}")
                    return@launch
                }

                addLog("正在初始化 GPU 硬件 H.264 解码器 (${renderW}x${renderH})...", LogLevel.INFO)

                var videoStream: AdbStream? = null
                addLog("正在尝试连接 scrcpy 专属视频通道 (localabstract:scrcpy)...", LogLevel.INFO)
                for (attempt in 1..20) {
                    if (streamingJob?.isActive != true) break
                    try {
                        videoStream = connection.openStream("localabstract:scrcpy")
                        addLog("成功连接 scrcpy-server 视频 Socket! (尝试第 $attempt 次)", LogLevel.SUCCESS)
                        break
                    } catch (e: Exception) {
                        addLog("等待 scrcpy Socket (尝试 $attempt/20: ${e.message})...", LogLevel.INFO)
                        kotlinx.coroutines.delay(250)
                    }
                }

                if (videoStream == null) {
                    addLog("无法连接到 scrcpy 视频 Socket！请查看上方 [scrcpy-server] 的调试输出", LogLevel.ERROR)
                    _screenState.value = ScreenState.Error("无法连接 scrcpy 视频通道，请点击【查看日志】排查")
                    try { serverProcessStream.close() } catch (_: Exception) {}
                    return@launch
                }

                var audioStream: AdbStream? = null
                if (cfg.isAudioEnabled) {
                    addLog("音频转发已启用，正在尝试连接 scrcpy 专属音频通道 (localabstract:scrcpy)...", LogLevel.INFO)
                    try {
                        audioStream = connection.openStream("localabstract:scrcpy")
                        addLog("成功连接 scrcpy-server 音频 Socket!", LogLevel.SUCCESS)

                        val aStream = audioStream
                        scope.launch(Dispatchers.IO) {
                            var audioTrack: AudioTrack? = null
                            var audioDecoder: MediaCodec? = null
                            try {
                                val codecIdBuf = ByteArray(4)
                                if (aStream.readFully(codecIdBuf, 0, 4)) {
                                    val codecFourCC = String(codecIdBuf, 0, 4, Charsets.US_ASCII)
                                    val codecClean = codecFourCC.replace("\u0000", "").trim()
                                    
                                    if (codecClean.isEmpty()) {
                                        addLog("被控端未能成功捕获音频，音频流已被禁用", LogLevel.WARN)
                                        return@launch
                                    }

                                    val actualSampleRate = 48000
                                    val actualChannels = 2

                                    addLog("音频流元数据: Codec='$codecClean', 采样率=$actualSampleRate Hz, 声道数=$actualChannels", LogLevel.SUCCESS)
                                    val channelConfig = if (actualChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
                                    val minBufSize = AudioTrack.getMinBufferSize(actualSampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)

                                    audioTrack = AudioTrack.Builder()
                                        .setAudioAttributes(
                                            AudioAttributes.Builder()
                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                .build()
                                        )
                                        .setAudioFormat(
                                            AudioFormat.Builder()
                                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                                .setSampleRate(actualSampleRate)
                                                .setChannelMask(channelConfig)
                                                .build()
                                        )
                                        .setBufferSizeInBytes(minBufSize.coerceAtLeast(8192) * 4)
                                        .setTransferMode(AudioTrack.MODE_STREAM)
                                        .build()

                                    audioTrack.play()

                                    val isRawPcm = codecClean.equals("RAW", ignoreCase = true) || codecClean.equals("PCM", ignoreCase = true)

                                    if (isRawPcm) {
                                        addLog("AudioTrack 实时音频播放器已初始化 (PCM 16Bit Direct, ${actualSampleRate}Hz)", LogLevel.INFO)
                                        val packetHeaderBuf = ByteArray(12)
                                        while (streamingJob?.isActive == true && !aStream.isClosed) {
                                            if (!aStream.readFully(packetHeaderBuf, 0, 12)) break

                                            val isConfig = (packetHeaderBuf[0].toInt() and 0x80) != 0
                                            var packetSize = 0
                                            for (i in 8..11) {
                                                packetSize = (packetSize shl 8) or (packetHeaderBuf[i].toInt() and 0xFF)
                                            }

                                            if (packetSize in 1..512000) {
                                                val packetBuf = ByteArray(packetSize)
                                                if (!aStream.readFully(packetBuf, 0, packetSize)) break

                                                if (!isConfig) {
                                                    audioTrack.write(packetBuf, 0, packetSize)
                                                }
                                            }
                                        }
                                    } else {
                                        val mimeType = if (codecClean.contains("OPUS", ignoreCase = true)) "audio/opus" else "audio/mp4a-latm"
                                        addLog("初始化 MediaCodec 音频解码器 ($mimeType, ${actualSampleRate}Hz)...", LogLevel.INFO)

                                        val format = MediaFormat.createAudioFormat(mimeType, actualSampleRate, actualChannels)
                                        audioDecoder = MediaCodec.createDecoderByType(mimeType)
                                        audioDecoder.configure(format, null, null, 0)
                                        audioDecoder.start()

                                        val packetHeaderBuf = ByteArray(12)
                                        val bufferInfo = MediaCodec.BufferInfo()

                                        while (streamingJob?.isActive == true && !aStream.isClosed) {
                                            if (!aStream.readFully(packetHeaderBuf, 0, 12)) break

                                            val isConfig = (packetHeaderBuf[0].toInt() and 0x80) != 0
                                            var packetSize = 0
                                            for (i in 8..11) {
                                                packetSize = (packetSize shl 8) or (packetHeaderBuf[i].toInt() and 0xFF)
                                            }

                                            if (packetSize in 1..512000) {
                                                val packetBuf = ByteArray(packetSize)
                                                if (!aStream.readFully(packetBuf, 0, packetSize)) break

                                                val inIndex = audioDecoder.dequeueInputBuffer(10000)
                                                if (inIndex >= 0) {
                                                    val inBuffer = audioDecoder.getInputBuffer(inIndex)
                                                    if (inBuffer != null) {
                                                        inBuffer.clear()
                                                        inBuffer.put(packetBuf)
                                                        val flags = if (isConfig) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else 0
                                                        audioDecoder.queueInputBuffer(inIndex, 0, packetSize, 0, flags)
                                                    }
                                                }

                                                var outIndex = audioDecoder.dequeueOutputBuffer(bufferInfo, 0)
                                                while (outIndex >= 0) {
                                                    val outBuffer = audioDecoder.getOutputBuffer(outIndex)
                                                    if (outBuffer != null && bufferInfo.size > 0) {
                                                        outBuffer.position(bufferInfo.offset)
                                                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                                        val chunk = ByteArray(bufferInfo.size)
                                                        outBuffer.get(chunk)
                                                        audioTrack.write(chunk, 0, chunk.size)
                                                    }
                                                    audioDecoder.releaseOutputBuffer(outIndex, false)
                                                    outIndex = audioDecoder.dequeueOutputBuffer(bufferInfo, 0)
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                addLog("音频流播放发生错误: ${e.message}", LogLevel.WARN)
                            } finally {
                                try {
                                    audioDecoder?.stop()
                                    audioDecoder?.release()
                                } catch (_: Exception) {}
                                try {
                                    audioTrack?.stop()
                                    audioTrack?.release()
                                } catch (_: Exception) {}
                                try { aStream.close() } catch (_: Exception) {}
                            }
                        }
                    } catch (e: Exception) {
                        addLog("连接 scrcpy 音频 Socket 异常: ${e.message}", LogLevel.WARN)
                    }
                }

                try {
                    val ctrl = connection.openStream("localabstract:scrcpy")
                    controlStream = ctrl
                    addLog("成功连接 scrcpy-server 控制 Socket!", LogLevel.SUCCESS)
                    if (cfg.isScreenOff && !isCamera) {
                        try {
                            ctrl.write(byteArrayOf(10, 0)) // 10 = SET_SCREEN_POWER_MODE, 0 = POWER_MODE_OFF
                            addLog("已通过控制通道发送熄屏指令 (SET_SCREEN_POWER_MODE=0)", LogLevel.INFO)
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    addLog("连接 scrcpy 控制 Socket 异常: ${e.message}", LogLevel.WARN)
                }

                val stream = videoStream
                addLog("开始对齐 scrcpy Socket 握手头...", LogLevel.INFO)

                // 1. Read 1 dummy connection byte sent by scrcpy server
                val dummyBuf = ByteArray(1)
                if (!stream.readFully(dummyBuf, 0, 1)) {
                    addLog("读取 scrcpy Dummy 握手字节失败", LogLevel.ERROR)
                    _screenState.value = ScreenState.Error("连接 scrcpy 失败: 握手中断")
                    try { stream.close() } catch (_: Exception) {}
                    try { serverProcessStream?.close() } catch (_: Exception) {}
                    return@launch
                }
                addLog("已成功对齐 Socket 握手字节: 0x${String.format("%02X", dummyBuf[0])}", LogLevel.INFO)

                // 2. Read 64 bytes device name
                addLog("开始读取 scrcpy 64 字节设备头...", LogLevel.INFO)
                val deviceHeaderBuf = ByteArray(64)
                if (!stream.readFully(deviceHeaderBuf, 0, 64)) {
                    addLog("读取 scrcpy 设备头失败 (Socket 已关闭)", LogLevel.ERROR)
                    _screenState.value = ScreenState.Error("读取 scrcpy 设备头失败")
                    try { stream.close() } catch (_: Exception) {}
                    try { serverProcessStream?.close() } catch (_: Exception) {}
                    return@launch
                }

                val deviceName = String(deviceHeaderBuf, Charsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
                addLog("读取设备头成功! 设备名称: $deviceName", LogLevel.SUCCESS)

                // 3. Read 4 bytes video codec FourCC (scrcpy v2.x / v3.x / v4.x sends 4-byte FourCC: "h264", "h265", "av01")
                addLog("开始读取 scrcpy 视频流 Codec 元数据 (4 字节 FourCC)...", LogLevel.INFO)
                val codecHeaderBuf = ByteArray(4)
                if (!stream.readFully(codecHeaderBuf, 0, 4)) {
                    addLog("读取 scrcpy 视频元数据失败", LogLevel.ERROR)
                    _screenState.value = ScreenState.Error("读取 scrcpy 视频元数据失败")
                    try { stream.close() } catch (_: Exception) {}
                    try { serverProcessStream?.close() } catch (_: Exception) {}
                    return@launch
                }

                val codecId = ((codecHeaderBuf[0].toInt() and 0xFF) shl 24) or
                              ((codecHeaderBuf[1].toInt() and 0xFF) shl 16) or
                              ((codecHeaderBuf[2].toInt() and 0xFF) shl 8) or
                              (codecHeaderBuf[3].toInt() and 0xFF)

                val codecFourCC = String(codecHeaderBuf, 0, 4, Charsets.US_ASCII)
                val codecHex = String.format("%08X", codecId)
                addLog("视频元数据解析成功: Codec=$codecFourCC (0x$codecHex)", LogLevel.SUCCESS)

                val mimeType = when {
                    codecFourCC.contains("h265", ignoreCase = true) || codecFourCC.contains("hevc", ignoreCase = true) -> MediaFormat.MIMETYPE_VIDEO_HEVC
                    codecFourCC.contains("av01", ignoreCase = true) || codecFourCC.contains("av1", ignoreCase = true) -> "video/av01"
                    else -> MediaFormat.MIMETYPE_VIDEO_AVC
                }

                val actualW = renderW and 0xFFFE
                val actualH = renderH and 0xFFFE

                remoteWidth = actualW
                remoteHeight = actualH

                _screenState.value = ScreenState.Streaming(
                    width = actualW,
                    height = actualH,
                    fps = 0f,
                    latencyMs = 12L
                )

                addLog("视频解码器准备就绪 (${actualW}x${actualH}, $mimeType)，等待 UI 渲染 Surface...", LogLevel.INFO)

                var waitCount = 0
                while (activeSurface == null && streamingJob?.isActive == true && waitCount < 50) {
                    kotlinx.coroutines.delay(100)
                    waitCount++
                }

                val surface = activeSurface
                if (surface == null || !surface.isValid) {
                    addLog("未能获取到有效的 UI 渲染 Surface", LogLevel.ERROR)
                    _screenState.value = ScreenState.Error("UI 渲染 Surface 准备超时")
                    try { stream.close() } catch (_: Exception) {}
                    try { serverProcessStream?.close() } catch (_: Exception) {}
                    return@launch
                }

                var frameCount = 0
                var lastFpsTime = System.currentTimeMillis()
                var currentFps = 30f

                val decoder = H264StreamDecoder(
                    surface = surface,
                    width = actualW,
                    height = actualH,
                    mimeType = mimeType,
                    onVideoSizeChanged = { newW, newH ->
                        val fixedW = newW and 0xFFFE
                        val fixedH = newH and 0xFFFE
                        if (fixedW > 0 && fixedH > 0 && (fixedW != remoteWidth || fixedH != remoteHeight)) {
                            addLog("检测到视频流动态分辨率变更: ${fixedW}x${fixedH}", LogLevel.INFO)
                            remoteWidth = fixedW
                            remoteHeight = fixedH
                            _screenState.value = ScreenState.Streaming(
                                width = fixedW,
                                height = fixedH,
                                fps = currentFps,
                                latencyMs = 12L
                            )
                        }
                    },
                    onFrameRendered = {
                        frameCount++
                        val now = System.currentTimeMillis()
                        val timeDiff = now - lastFpsTime
                        if (timeDiff >= 1000) {
                            currentFps = (frameCount * 1000f) / timeDiff
                            frameCount = 0
                            lastFpsTime = now
                        }

                        _screenState.value = ScreenState.Streaming(
                            width = remoteWidth,
                            height = remoteHeight,
                            fps = currentFps,
                            latencyMs = 12L
                        )
                    },
                    logger = { msg, lvl ->
                        addLog(msg, lvl)
                    }
                )
                currentDecoder = decoder
                decoder.start()

                addLog("GPU 硬件解码器已成功绑定 Surface (${actualW}x${actualH}, $mimeType)，开始画面实时渲染！", LogLevel.SUCCESS)

                val packetHeaderBuf = ByteArray(12)
                var packetCount = 0

                try {
                    while (connection.isConnected && streamingJob?.isActive == true && !stream.isClosed) {
                        if (!stream.readFully(packetHeaderBuf, 0, 12)) break

                        // Check if MSB (Bit 63) is 1 -> Session Packet (Dimensions change/announcement, NO payload data!)
                        val isSession = (packetHeaderBuf[0].toInt() and 0x80) != 0
                        if (isSession) {
                            var sWidth = 0
                            for (i in 4..7) {
                                sWidth = (sWidth shl 8) or (packetHeaderBuf[i].toInt() and 0xFF)
                            }
                            var sHeight = 0
                            for (i in 8..11) {
                                sHeight = (sHeight shl 8) or (packetHeaderBuf[i].toInt() and 0xFF)
                            }
                            if (sWidth > 0 && sHeight > 0) {
                                val adjW = sWidth and 0xFFFE
                                val adjH = sHeight and 0xFFFE
                                remoteWidth = adjW
                                remoteHeight = adjH
                                _screenState.value = ScreenState.Streaming(
                                    width = adjW,
                                    height = adjH,
                                    fps = currentFps,
                                    latencyMs = 12L
                                )
                                addLog("收到 scrcpy Session 配置帧: 远程视频流动态分辨率更新为 ${adjW}x${adjH}", LogLevel.SUCCESS)
                            }
                            // Session packet has NO payload data, continue to next 12-byte header!
                            continue
                        }

                        // Media packet:
                        // SC_PACKET_FLAG_CONFIG = (1ULL << 62)
                        // SC_PACKET_FLAG_KEY_FRAME = (1ULL << 61)
                        // SC_PACKET_PTS_MASK = low 61 bits
                        var ptsAndFlags = 0L
                        for (i in 0..7) {
                            ptsAndFlags = (ptsAndFlags shl 8) or (packetHeaderBuf[i].toLong() and 0xFFL)
                        }

                        var packetSize = 0
                        for (i in 8..11) {
                            packetSize = (packetSize shl 8) or (packetHeaderBuf[i].toInt() and 0xFF)
                        }

                        val isConfig = (ptsAndFlags and (1L shl 62)) != 0L
                        val isKeyFrame = (ptsAndFlags and (1L shl 61)) != 0L
                        val rawPts = ptsAndFlags and ((1L shl 61) - 1)

                        if (packetSize > 0 && packetSize < 10 * 1024 * 1024) {
                            val packetBuf = ByteArray(packetSize)
                            if (!stream.readFully(packetBuf, 0, packetSize)) break

                            packetCount++
                            if (packetCount <= 5 || packetCount % 60 == 0) {
                                val prefixHex = packetBuf.take(8).joinToString(" ") { String.format("%02X", it) }
                                val ptsHex = String.format("%016X", ptsAndFlags)
                                addLog("解包视频帧 #$packetCount: 长度=$packetSize, Config=$isConfig, KeyFrame=$isKeyFrame, PTS=$rawPts (0x$ptsHex), 前缀=[$prefixHex]", LogLevel.INFO)
                            }

                            decoder.decodeChunk(packetBuf, 0, packetSize, isConfig, isKeyFrame, rawPts)
                        }
                    }
                } catch (e: Exception) {
                    addLog("视频传输中断: ${e.message}", LogLevel.WARN)
                } finally {
                    currentDecoder = null
                    decoder.stop()
                    try { videoStream.close() } catch (_: Exception) {}
                    try { audioStream?.close() } catch (_: Exception) {}
                    try { serverProcessStream?.close() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                addLog("投屏启动出现严重错误: ${e.message}", LogLevel.ERROR)
                _screenState.value = ScreenState.Error(e.message ?: "Screen mirroring failed")
            }
        }
    }

    private suspend fun ensureScrcpyServerPushed() {
        if (context == null) {
            addLog("未传递 Context，跳过 scrcpy-server.jar 部署检查", LogLevel.WARN)
            return
        }
        val localFile = ScrcpyServerManager.getServerFile(context)
        if (!localFile.exists() || localFile.length() <= 0) {
            addLog("未检测到本地 scrcpy-server.jar，请先在投屏设置面板中下载组件", LogLevel.ERROR)
            throw Exception("本地尚未下载 scrcpy-server 组件，请在下方点击【立即下载】")
        }

        val localSize = localFile.length()
        val localSizeStr = localSize.toString()
        val sizeFormatted = ScrcpyServerManager.formatSize(localSize)

        addLog("检查被控端 /data/local/tmp/scrcpy-server.jar 状态 (本地文件大小: $sizeFormatted, $localSize 字节)...", LogLevel.INFO)
        try {
            val check = connection.executeShell("ls -l /data/local/tmp/scrcpy-server.jar").trim()
            if (check.contains("scrcpy-server.jar") && check.contains(localSizeStr)) {
                addLog("被控端已存在 scrcpy-server.jar (校验通过: $sizeFormatted)", LogLevel.SUCCESS)
                return
            }
        } catch (_: Exception) {}

        try {
            addLog("开始使用 ADB SYNC 原生协议高速推送 scrcpy-server.jar ($sizeFormatted)...", LogLevel.INFO)
            val syncClient = AdbSyncClient(connection)
            localFile.inputStream().use { input ->
                syncClient.pushFile(input, "/data/local/tmp/scrcpy-server.jar") { sent ->
                    val pct = if (localSize > 0) ((sent.toFloat() / localSize) * 100).toInt().coerceIn(0, 100) else 0
                    if (pct % 25 == 0 || sent == localSize) {
                        addLog("AdbSync 推送进度: $pct% ($sent / $localSize 字节)", LogLevel.INFO)
                    }
                }
            }

            connection.executeShell("chmod 755 /data/local/tmp/scrcpy-server.jar")

            val checkResult = connection.executeShell("ls -l /data/local/tmp/scrcpy-server.jar").trim()
            if (checkResult.contains("scrcpy-server.jar") && checkResult.contains(localSizeStr)) {
                addLog("成功完成 scrcpy-server.jar 部署并校验通过 (大小: $sizeFormatted)！", LogLevel.SUCCESS)
            } else {
                addLog("部署完成，文件检查返回: $checkResult", LogLevel.WARN)
            }
        } catch (e: Exception) {
            addLog("推送 scrcpy-server.jar 发生异常: ${e.message}", LogLevel.ERROR)
            throw e
        }
    }

    private fun fetchRemoteResolution() {
        try {
            addLog("正在查询被控端原生物理屏幕分辨率...", LogLevel.INFO)
            val output = connection.executeShell("wm size")
            val regex = Regex("(\\d+)x(\\d+)")
            val match = regex.find(output)
            if (match != null) {
                val (w, h) = match.destructured
                nativeWidth = w.toInt()
                nativeHeight = h.toInt()
                remoteWidth = nativeWidth
                remoteHeight = nativeHeight
                addLog("成功识别被控端原生物理屏幕分辨率: ${nativeWidth}x${nativeHeight}", LogLevel.SUCCESS)
            }
        } catch (e: Exception) {
            nativeWidth = 1080
            nativeHeight = 2400
            remoteWidth = 1080
            remoteHeight = 2400
            addLog("识别被控端分辨率失败，使用默认 1080x2400", LogLevel.WARN)
        }
    }

    private suspend fun findLatestVirtualDisplayId(): Int {
        try {
            val dumpsysOut = connection.executeShell("dumpsys display")
            val ids = Regex("""(?:Display id|displayId)=(\d+)""").findAll(dumpsysOut)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .filter { it > 0 }
                .toList()
            if (ids.isNotEmpty()) {
                val maxId = ids.maxOrNull() ?: 2
                addLog("获取到新创虚拟屏幕 ID: $maxId", LogLevel.INFO)
                return maxId
            }
        } catch (_: Exception) {}
        return 2
    }

    private suspend fun launchPackageOnDisplay(pkg: String, displayId: Int) {
        if (pkg.isBlank()) return
        try {
            addLog("正在虚拟屏幕启动目标应用 (通过 scrcpy 控制通道): $pkg ...", LogLevel.INFO)
            val stream = controlStream
            if (stream != null && isVirtualDisplayActive.value) {
                val pkgBytes = pkg.toByteArray(Charsets.UTF_8)
                if (pkgBytes.size > 255) {
                    addLog("包名太长，无法通过 control 启动", LogLevel.WARN)
                    return
                }
                val packet = ByteArray(pkgBytes.size + 2)
                packet[0] = 16 // TYPE_START_APP
                packet[1] = pkgBytes.size.toByte()
                System.arraycopy(pkgBytes, 0, packet, 2, pkgBytes.size)
                stream.write(packet)
                addLog("成功发送应用启动指令!", LogLevel.SUCCESS)
            } else {
                addLog("scrcpy 控制通道未就绪，将尝试 am start", LogLevel.WARN)
                val resolveOut = connection.executeShell("cmd package resolve-activity --brief $pkg")
                val mainActivity = resolveOut.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.contains("/") && !it.startsWith("No activity") }
                if (mainActivity != null) {
                    val startRes = connection.executeShell("am start --display $displayId -n $mainActivity")
                    addLog("在屏幕 $displayId 启动 Activity: $mainActivity ($startRes)", LogLevel.INFO)
                }
            }
        } catch (e: Exception) {
            addLog("启动应用发生异常: ${e.message}", LogLevel.WARN)
        }
    }

    private suspend fun applyRemoteSettingsInternal(config: ScrcpyConfig, oldConfig: ScrcpyConfig?) {
        try {
            if (oldConfig == null) {
                connection.executeShell("wm size reset")
            }

            // Handle Stay Awake (阻止休眠 --stay-awake)
            if (config.isStayAwake && (oldConfig == null || !oldConfig.isStayAwake)) {
                addLog("启用被控端阻止休眠 (保持常亮/在线状态)...", LogLevel.INFO)
                connection.executeShell("svc power stayon true")
            } else if (!config.isStayAwake && oldConfig?.isStayAwake == true && !config.isScreenOff) {
                addLog("关闭被控端阻止休眠...", LogLevel.INFO)
                connection.executeShell("svc power stayon false")
            }

            // Handle Screen Off (熄屏控制 --turn-screen-off)
            if (config.isScreenOff && (oldConfig == null || !oldConfig.isScreenOff)) {
                addLog("开启被控端熄屏控制 (关闭屏幕亮屏但保持投屏连接)...", LogLevel.INFO)
                // 1. Send scrcpy control message SET_SCREEN_POWER_MODE (0 = POWER_MODE_OFF)
                val stream = controlStream
                if (stream != null && !stream.isClosed) {
                    try {
                        stream.write(byteArrayOf(10, 0))
                    } catch (_: Exception) {}
                }
                addLog("已熄灭被控端屏幕", LogLevel.SUCCESS)
            } else if (!config.isScreenOff && oldConfig?.isScreenOff == true) {
                addLog("恢复被控端屏幕亮屏...", LogLevel.INFO)
                val stream = controlStream
                if (stream != null && !stream.isClosed) {
                    try {
                        stream.write(byteArrayOf(10, 2)) // 2 = POWER_MODE_NORMAL
                    } catch (_: Exception) {}
                }
                if (!config.isStayAwake) {
                    connection.executeShell("svc power stayon false")
                }
                addLog("已恢复被控端亮屏", LogLevel.SUCCESS)
            }
        } catch (e: Exception) {
            addLog("应用远端设置异常: ${e.message}", LogLevel.WARN)
        }
    }

    private fun applyRemoteSettings(config: ScrcpyConfig, oldConfig: ScrcpyConfig?) {
        scope.launch(Dispatchers.IO) {
            applyRemoteSettingsInternal(config, oldConfig)
        }
    }

    fun stopMirroring() {
        addLog("停止屏幕投屏...", LogLevel.INFO)
        val wasScreenOff = _config.value.isScreenOff
        val stream = controlStream
        if (wasScreenOff && stream != null && !stream.isClosed) {
            try {
                stream.write(byteArrayOf(10, 2)) // restore screen power
            } catch (_: Exception) {}
        }

        streamingJob?.cancel()
        streamingJob = null
        try { controlStream?.close() } catch (_: Exception) {}
        controlStream = null
        isFullscreen.value = false
        _screenState.value = ScreenState.Idle

        scope.launch(Dispatchers.IO) {
            try {
                connection.executeShell("wm size reset")
                if (virtualDisplayId > 0) {
                    virtualDisplayId = -1
                }
                if (_config.value.isStayAwake || wasScreenOff) {
                    connection.executeShell("svc power stayon false")
                }
                if (_config.value.isPowerOffOnClose) {
                    addLog("退出投屏，执行关闭屏幕 (--power-off-on-close)...", LogLevel.INFO)
                    connection.executeShell("input keyevent 26")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendTap(x: Int, y: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val realX: Int
                val realY: Int
                if (remoteWidth > 0 && remoteHeight > 0 && nativeWidth > 0 && nativeHeight > 0) {
                    realX = ((x.toFloat() / remoteWidth.toFloat()) * nativeWidth).toInt().coerceIn(0, nativeWidth - 1)
                    realY = ((y.toFloat() / remoteHeight.toFloat()) * nativeHeight).toInt().coerceIn(0, nativeHeight - 1)
                } else {
                    realX = x
                    realY = y
                }
                val displayArg = if (virtualDisplayId > 0) " -d $virtualDisplayId" else ""
                connection.executeShell("input$displayArg tap $realX $realY")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var touchStream: AdbStream? = null

    fun sendTouchEvent(action: String, x: Int, y: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val realX = if (remoteWidth > 0 && remoteHeight > 0 && nativeWidth > 0 && nativeHeight > 0) {
                    ((x.toFloat() / remoteWidth.toFloat()) * nativeWidth).toInt().coerceIn(0, nativeWidth - 1)
                } else x
                val realY = if (remoteHeight > 0 && nativeHeight > 0 && remoteWidth > 0 && nativeWidth > 0) {
                    ((y.toFloat() / remoteHeight.toFloat()) * nativeHeight).toInt().coerceIn(0, nativeHeight - 1)
                } else y

                val displayArg = if (virtualDisplayId > 0) " -d $virtualDisplayId" else ""
                val cmd = "input$displayArg motionevent $action $realX $realY\n"

                var stream = touchStream
                if (stream == null || stream.isClosed) {
                    try {
                        stream = connection.openStream("shell:")
                        touchStream = stream
                    } catch (_: Exception) {}
                }

                if (stream != null && !stream.isClosed) {
                    stream.write(cmd.toByteArray(Charsets.UTF_8))
                } else {
                    connection.executeShell("input$displayArg motionevent $action $realX $realY")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendTouchPath(path: List<Pair<Int, Int>>) {
        if (path.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val nativePath = path.map { (x, y) ->
                    val realX = if (remoteWidth > 0 && nativeWidth > 0 && remoteHeight > 0 && nativeHeight > 0) {
                        ((x.toFloat() / remoteWidth.toFloat()) * nativeWidth).toInt().coerceIn(0, nativeWidth - 1)
                    } else x
                    val realY = if (remoteHeight > 0 && nativeHeight > 0 && remoteWidth > 0 && nativeWidth > 0) {
                        ((y.toFloat() / remoteHeight.toFloat()) * nativeHeight).toInt().coerceIn(0, nativeHeight - 1)
                    } else y
                    Pair(realX, realY)
                }

                if (nativePath.size == 1) {
                    val (rx, ry) = nativePath[0]
                    val displayArg = if (virtualDisplayId > 0) " -d $virtualDisplayId" else ""
                    connection.executeShell("input$displayArg tap $rx $ry")
                    return@launch
                }

                val displayArg = if (virtualDisplayId > 0) " -d $virtualDisplayId" else ""

                // Subsample path if very long to prevent exceeding shell argument limits
                val maxPoints = 50
                val sampled = if (nativePath.size > maxPoints) {
                    val step = nativePath.size.toFloat() / maxPoints.toFloat()
                    val list = mutableListOf<Pair<Int, Int>>()
                    for (i in 0 until maxPoints) {
                        val idx = (i * step).toInt().coerceIn(0, nativePath.size - 1)
                        list.add(nativePath[idx])
                    }
                    if (list.last() != nativePath.last()) {
                        list.add(nativePath.last())
                    }
                    list
                } else {
                    nativePath
                }

                val sb = StringBuilder()
                sb.append("input$displayArg motionevent DOWN ${sampled[0].first} ${sampled[0].second}; ")
                for (i in 1 until sampled.size - 1) {
                    sb.append("input$displayArg motionevent MOVE ${sampled[i].first} ${sampled[i].second}; ")
                }
                sb.append("input$displayArg motionevent UP ${sampled.last().first} ${sampled.last().second}")

                val output = connection.executeShell(sb.toString())
                if (output.contains("invalid") || output.contains("Unknown command") || output.contains("Error")) {
                    // Fallback to chained swipe segments if motionevent command is not supported on target OS
                    val fallbackSb = StringBuilder()
                    for (i in 0 until sampled.size - 1) {
                        val p1 = sampled[i]
                        val p2 = sampled[i + 1]
                        fallbackSb.append("input$displayArg swipe ${p1.first} ${p1.second} ${p2.first} ${p2.second} 15; ")
                    }
                    connection.executeShell(fallbackSb.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendSwipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Int = 200) {
        scope.launch(Dispatchers.IO) {
            try {
                val realStartX: Int
                val realStartY: Int
                val realEndX: Int
                val realEndY: Int
                if (remoteWidth > 0 && remoteHeight > 0 && nativeWidth > 0 && nativeHeight > 0) {
                    realStartX = ((startX.toFloat() / remoteWidth.toFloat()) * nativeWidth).toInt().coerceIn(0, nativeWidth - 1)
                    realStartY = ((startY.toFloat() / remoteHeight.toFloat()) * nativeHeight).toInt().coerceIn(0, nativeHeight - 1)
                    realEndX = ((endX.toFloat() / remoteWidth.toFloat()) * nativeWidth).toInt().coerceIn(0, nativeWidth - 1)
                    realEndY = ((endY.toFloat() / remoteHeight.toFloat()) * nativeHeight).toInt().coerceIn(0, nativeHeight - 1)
                } else {
                    realStartX = startX
                    realStartY = startY
                    realEndX = endX
                    realEndY = endY
                }
                val displayArg = if (virtualDisplayId > 0) " -d $virtualDisplayId" else ""
                connection.executeShell("input$displayArg swipe $realStartX $realStartY $realEndX $realEndY $durationMs")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendKeyEvent(keyCode: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                connection.executeShell("input keyevent $keyCode")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendText(text: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val escaped = text.replace(" ", "%s").replace("'", "\\'")
                connection.executeShell("input text '$escaped'")
                addLog("发送远程按键文字: '$text'", LogLevel.INFO)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendHome() = sendKeyEvent(3)
    fun sendBack() = sendKeyEvent(4)
    fun sendRecents() = sendKeyEvent(187)
    fun sendPower() = sendKeyEvent(26)
    fun sendVolumeUp() = sendKeyEvent(24)
    fun sendVolumeDown() = sendKeyEvent(25)
}

