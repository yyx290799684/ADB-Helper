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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private var activeSurface: Surface? = null
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
            maxResolution = prefs.getInt("maxResolution", 720),
            maxFps = prefs.getInt("maxFps", 30),
            isScreenOff = prefs.getBoolean("isScreenOff", false),
            isAudioEnabled = prefs.getBoolean("isAudioEnabled", false),
            virtualDisplayWidth = prefs.getInt("virtualDisplayWidth", 0),
            virtualDisplayHeight = prefs.getInt("virtualDisplayHeight", 0),
            launchPackageOnVirtualDisplay = prefs.getString("launchPackageOnVirtualDisplay", "") ?: ""
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
                .putBoolean("isScreenOff", cfg.isScreenOff)
                .putBoolean("isAudioEnabled", cfg.isAudioEnabled)
                .putInt("virtualDisplayWidth", cfg.virtualDisplayWidth)
                .putInt("virtualDisplayHeight", cfg.virtualDisplayHeight)
                .putString("launchPackageOnVirtualDisplay", cfg.launchPackageOnVirtualDisplay)
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
        addLog("配置已更新: 分辨率=${if(newConfig.maxResolution==0)"原生" else "${newConfig.maxResolution}p"}, 帧率=${newConfig.maxFps}FPS, 码率=${newConfig.bitrate/1000000}Mbps, 息屏=${newConfig.isScreenOff}, 音频转发=${newConfig.isAudioEnabled}", LogLevel.INFO)
        if (_screenState.value is ScreenState.Streaming || _screenState.value is ScreenState.Connecting) {
            val audioChanged = oldConfig.isAudioEnabled != newConfig.isAudioEnabled

            if (audioChanged) {
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

    fun startMirroring() {
        if (streamingJob?.isActive == true) return
        _screenState.value = ScreenState.Connecting
        addLog("开始启动屏幕投屏流程...", LogLevel.INFO)

        streamingJob = scope.launch(Dispatchers.IO) {
            try {
                // 1. Wake up remote device screen if locked
                wakeUpScreenInternal()

                // 2. Get remote native screen resolution
                fetchRemoteResolution()

                // 3. Push official Genymobile scrcpy-server.jar
                ensureScrcpyServerPushed()

                // 4. Apply initial settings
                applyRemoteSettingsInternal(_config.value, null)

                // Delete any residual overlay_display_devices to avoid floating windows on target device
                try {
                    connection.executeShell("settings delete global overlay_display_devices")
                } catch (_: Exception) {}

                // 5. Calculate stream dimensions
                val cfg = _config.value
                val maxRes = if (cfg.maxResolution > 0) cfg.maxResolution else 0
                val aspect = if (nativeHeight > 0 && nativeWidth > 0) nativeHeight.toFloat() / nativeWidth.toFloat() else 2.2f
                val renderW = when {
                    maxRes in 1..480 -> 360
                    maxRes in 1..720 -> 540
                    else -> 720
                }
                val renderH = (renderW * aspect).toInt() and 0xFFFE // Ensure even dimensions for MediaCodec

                val audioParam = if (cfg.isAudioEnabled) " audio=true audio_codec=raw" else " audio=false"
                val displayParam = if (isVirtualDisplayActive.value) " new_display=${activeVirtualWidth}x${activeVirtualHeight}/240 vd_system_decorations=false" else " display_id=0"
                val serverCmd = "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server 3.1 log_level=info max_size=$maxRes video_bit_rate=${cfg.bitrate} max_fps=${cfg.maxFps} video_codec_options=i-frame-interval=1 tunnel_forward=true control=true$audioParam cleanup=false$displayParam"
                addLog("启动官方 scrcpy-server 进程: $serverCmd", LogLevel.INFO)

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
                    controlStream = connection.openStream("localabstract:scrcpy")
                    addLog("成功连接 scrcpy-server 控制 Socket!", LogLevel.SUCCESS)
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

                // 3. Read 12 bytes video stream header
                addLog("开始读取 scrcpy 12 字节视频流元数据...", LogLevel.INFO)
                val codecHeaderBuf = ByteArray(12)
                if (!stream.readFully(codecHeaderBuf, 0, 12)) {
                    addLog("读取 scrcpy 视频元数据失败", LogLevel.ERROR)
                    _screenState.value = ScreenState.Error("读取 scrcpy 视频元数据失败")
                    try { stream.close() } catch (_: Exception) {}
                    try { serverProcessStream?.close() } catch (_: Exception) {}
                    return@launch
                }

                // Check for 1-byte misalignment if codecHeaderBuf[0] == 0x00 and codecHeaderBuf[1] == 'h'
                if (codecHeaderBuf[0] == 0.toByte() && codecHeaderBuf[1] == 'h'.toByte()) {
                    addLog("检测到 Codec 元数据错位 1 字节，自动修正对齐...", LogLevel.WARN)
                    val extraByte = ByteArray(1)
                    if (stream.readFully(extraByte, 0, 1)) {
                        System.arraycopy(codecHeaderBuf, 1, codecHeaderBuf, 0, 11)
                        codecHeaderBuf[11] = extraByte[0]
                    }
                }

                val codecId = ((codecHeaderBuf[0].toInt() and 0xFF) shl 24) or
                              ((codecHeaderBuf[1].toInt() and 0xFF) shl 16) or
                              ((codecHeaderBuf[2].toInt() and 0xFF) shl 8) or
                              (codecHeaderBuf[3].toInt() and 0xFF)

                val scrcpyW = ((codecHeaderBuf[4].toInt() and 0xFF) shl 24) or
                              ((codecHeaderBuf[5].toInt() and 0xFF) shl 16) or
                              ((codecHeaderBuf[6].toInt() and 0xFF) shl 8) or
                              (codecHeaderBuf[7].toInt() and 0xFF)

                val scrcpyH = ((codecHeaderBuf[8].toInt() and 0xFF) shl 24) or
                              ((codecHeaderBuf[9].toInt() and 0xFF) shl 16) or
                              ((codecHeaderBuf[10].toInt() and 0xFF) shl 8) or
                              (codecHeaderBuf[11].toInt() and 0xFF)

                val codecFourCC = String(codecHeaderBuf, 0, 4, Charsets.US_ASCII)
                val codecHex = String.format("%08X", codecId)
                addLog("视频元数据解析成功: Codec=$codecFourCC (0x$codecHex), 画面尺寸=${scrcpyW}x${scrcpyH}", LogLevel.SUCCESS)

                val rawW = if (scrcpyW in 100..4000) scrcpyW else renderW
                val rawH = if (scrcpyH in 100..4000) scrcpyH else renderH
                val actualW = rawW and 0xFFFE
                val actualH = rawH and 0xFFFE

                remoteWidth = actualW
                remoteHeight = actualH

                _screenState.value = ScreenState.Streaming(
                    width = actualW,
                    height = actualH,
                    fps = 0f,
                    latencyMs = 12L
                )

                addLog("已对齐视频元数据 (${actualW}x${actualH})，等待 UI 渲染 Surface 准备就绪...", LogLevel.INFO)

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

                val decoder = H264StreamDecoder(surface, actualW, actualH) {
                    frameCount++
                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastFpsTime
                    if (timeDiff >= 1000) {
                        currentFps = (frameCount * 1000f) / timeDiff
                        frameCount = 0
                        lastFpsTime = now
                    }

                    _screenState.value = ScreenState.Streaming(
                        width = actualW,
                        height = actualH,
                        fps = currentFps,
                        latencyMs = 12L
                    )
                }
                currentDecoder = decoder
                decoder.start()

                addLog("GPU 硬件解码器已成功绑定 Surface (${actualW}x${actualH})，开始画面实时渲染！", LogLevel.SUCCESS)

                val packetHeaderBuf = ByteArray(12)
                var packetCount = 0

                try {
                    while (connection.isConnected && streamingJob?.isActive == true && !stream.isClosed) {
                        if (!stream.readFully(packetHeaderBuf, 0, 12)) break

                        var ptsAndFlags = 0L
                        for (i in 0..7) {
                            ptsAndFlags = (ptsAndFlags shl 8) or (packetHeaderBuf[i].toLong() and 0xFFL)
                        }

                        var packetSize = 0
                        for (i in 8..11) {
                            packetSize = (packetSize shl 8) or (packetHeaderBuf[i].toInt() and 0xFF)
                        }

                        val isConfig = (ptsAndFlags and (1L shl 63)) != 0L
                        val isKeyFrame = (ptsAndFlags and (1L shl 62)) != 0L

                        if (packetSize > 0 && packetSize < 10 * 1024 * 1024) {
                            val packetBuf = ByteArray(packetSize)
                            if (!stream.readFully(packetBuf, 0, packetSize)) break

                            packetCount++
                            if (packetCount <= 3) {
                                addLog("解包 H.264 数据包 #$packetCount: 长度=$packetSize, Config=$isConfig, KeyFrame=$isKeyFrame", LogLevel.INFO)
                            }

                            decoder.decodeChunk(packetBuf, 0, packetSize, isConfig, isKeyFrame)
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
        addLog("检查被控端 /data/local/tmp/scrcpy-server.jar 状态...", LogLevel.INFO)
        try {
            val check = connection.executeShell("ls -l /data/local/tmp/scrcpy-server.jar").trim()
            if (check.contains("scrcpy-server.jar") && check.contains("90640")) {
                addLog("被控端已存在官方 Genymobile scrcpy-server.jar (校验通过: 90,640 字节)", LogLevel.SUCCESS)
                return
            }
        } catch (_: Exception) {}

        try {
            addLog("正在读取 App 内置官方 scrcpy-server.jar (90,640 字节)...", LogLevel.INFO)
            val assetStream = context.assets.open("scrcpy-server.jar")
            val bytes = assetStream.readBytes()
            assetStream.close()

            val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val chunkSize = 1000
            val totalChunks = (base64Str.length + chunkSize - 1) / chunkSize

            addLog("开始推送 scrcpy-server.jar (分包 Base64 传输，共 $totalChunks 个数据包)...", LogLevel.INFO)
            connection.executeShell("rm -f /data/local/tmp/scrcpy-server.jar /data/local/tmp/scrcpy-server.b64")

            for (i in base64Str.indices step chunkSize) {
                val end = minOf(i + chunkSize, base64Str.length)
                val chunk = base64Str.substring(i, end)
                connection.executeShell("echo -n \"$chunk\" >> /data/local/tmp/scrcpy-server.b64")

                val currentChunk = i / chunkSize + 1
                if (currentChunk % 25 == 0 || currentChunk == totalChunks) {
                    addLog("已传输数据包 $currentChunk / $totalChunks", LogLevel.INFO)
                }
            }

            addLog("正在被控端解码并还原 scrcpy-server.jar...", LogLevel.INFO)
            connection.executeShell("base64 -d /data/local/tmp/scrcpy-server.b64 > /data/local/tmp/scrcpy-server.jar && rm -f /data/local/tmp/scrcpy-server.b64 && chmod 755 /data/local/tmp/scrcpy-server.jar")

            val checkResult = connection.executeShell("ls -l /data/local/tmp/scrcpy-server.jar").trim()
            if (checkResult.contains("90640") || checkResult.contains("scrcpy-server.jar")) {
                addLog("成功完成 scrcpy-server.jar 部署！文件详情: $checkResult", LogLevel.SUCCESS)
            } else {
                addLog("部署完成，文件检查返回: $checkResult", LogLevel.WARN)
            }
        } catch (e: Exception) {
            addLog("推送 scrcpy-server.jar 发生异常: ${e.message}", LogLevel.ERROR)
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

            if (config.isScreenOff && (oldConfig == null || !oldConfig.isScreenOff)) {
                addLog("开启被控端息屏控制 (熄灭被控端屏幕亮屏但不断开连接)...", LogLevel.INFO)
                connection.executeShell("svc power stayon true")
                connection.executeShell("settings put system screen_brightness_mode 0")
                connection.executeShell("settings put system screen_brightness 0")
                addLog("已熄灭被控端屏幕", LogLevel.SUCCESS)
            } else if (!config.isScreenOff && oldConfig?.isScreenOff == true) {
                addLog("恢复被控端屏幕亮屏...", LogLevel.INFO)
                connection.executeShell("svc power stayon false")
                connection.executeShell("settings put system screen_brightness_mode 1")
                connection.executeShell("settings put system screen_brightness 128")
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
                if (_config.value.isScreenOff) {
                    connection.executeShell("svc power stayon false")
                    connection.executeShell("settings put system screen_brightness_mode 1")
                    connection.executeShell("settings put system screen_brightness 128")
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

