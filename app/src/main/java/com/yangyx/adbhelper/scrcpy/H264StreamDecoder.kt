package com.yangyx.adbhelper.scrcpy

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class H264StreamDecoder(
    private var surface: Surface?,
    private val width: Int,
    private val height: Int,
    private val mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    private val videoBufferMs: Int = 0,
    private val onVideoSizeChanged: ((Int, Int) -> Unit)? = null,
    private val onFrameRendered: (() -> Unit)? = null,
    private val logger: ((String, LogLevel) -> Unit)? = null
) {
    data class FramePacket(
        val data: ByteArray,
        val isConfig: Boolean,
        val isKeyFrame: Boolean,
        val ptsUs: Long,
        val arrivalTimeNs: Long = System.nanoTime()
    )

    private var codec: MediaCodec? = null
    @Volatile
    private var activeSurface: Surface? = surface
    @Volatile
    private var isRunning = false
    @Volatile
    private var bufferDelayMs: Int = videoBufferMs.coerceAtLeast(0)

    @Volatile
    private var latestConfigPacket: FramePacket? = null
    @Volatile
    private var latestKeyFramePacket: FramePacket? = null

    private var decodeThread: Thread? = null
    private var renderThread: Thread? = null

    private val packetQueue = LinkedBlockingQueue<FramePacket>(180)
    private var inputFeedCount = 0
    private var renderSuccessCount = 0

    fun setVideoBufferMs(ms: Int) {
        val newMs = ms.coerceAtLeast(0)
        bufferDelayMs = newMs
        logger?.invoke("视频缓冲时长动态更新为: ${newMs}ms", LogLevel.INFO)
    }

    fun start(): Boolean {
        val targetSurface = surface ?: activeSurface
        if (targetSurface == null || !targetSurface.isValid) {
            logger?.invoke("无法启动解码器: UI 渲染 Surface 无效或未就绪", LogLevel.ERROR)
            return false
        }
        activeSurface = targetSurface

        val started = createAndStartCodec(targetSurface)
        if (!started) {
            logger?.invoke("MediaCodec 所有解码模式均启动失败 ($mimeType, ${width}x${height})", LogLevel.ERROR)
            return false
        }

        decodeThread = thread(name = "H264DecodeWorker") {
            decodeLoop()
        }

        renderThread = thread(name = "H264RenderWorker") {
            renderLoop()
        }

        logger?.invoke("MediaCodec 视频解码器工作线程已就绪，等待输入流...", LogLevel.INFO)
        return true
    }

    private fun createAndStartCodec(targetSurface: Surface): Boolean {
        var lastError: Exception? = null

        // Attempt 1: Hardware decoder with dynamically validated adaptive dimensions and low-latency flags
        try {
            val hwCodec = MediaCodec.createDecoderByType(mimeType)
            val caps = try { hwCodec.codecInfo.getCapabilitiesForType(mimeType) } catch (_: Exception) { null }
            val vCaps = caps?.videoCapabilities

            val format = MediaFormat.createVideoFormat(mimeType, width, height)
            val maxInputSize = maxOf(width * height, 2 * 1024 * 1024)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize)

            // Validate adaptive playback capabilities safely:
            // Do NOT exceed what the hardware decoder actually supports!
            val isAdaptive = caps?.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_AdaptivePlayback) == true
            if (isAdaptive && vCaps != null) {
                val maxDim = maxOf(width, height)
                val maxAllowedW = vCaps.supportedWidths.upper
                val maxAllowedH = vCaps.supportedHeights.upper
                val candidateW = minOf(maxDim, maxAllowedW)
                val candidateH = minOf(maxDim, maxAllowedH)
                if (vCaps.isSizeSupported(candidateW, candidateH)) {
                    format.setInteger(MediaFormat.KEY_MAX_WIDTH, candidateW)
                    format.setInteger(MediaFormat.KEY_MAX_HEIGHT, candidateH)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && caps?.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency) == true) {
                try {
                    format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
                } catch (_: Exception) {}
            }

            logger?.invoke("正在配置硬件解码器 (${hwCodec.name}): $mimeType (${width}x${height}), 绑定Surface有效性=${targetSurface.isValid}", LogLevel.INFO)
            hwCodec.configure(format, targetSurface, null, 0)
            hwCodec.start()
            codec = hwCodec
            isRunning = true
            logger?.invoke("MediaCodec 硬件解码器 (${hwCodec.name}, $mimeType) 启动成功", LogLevel.SUCCESS)
            return true
        } catch (e: Exception) {
            lastError = e
            logger?.invoke("硬件解码器增强配置失败 (${e.message})，正在尝试标准兼容配置...", LogLevel.WARN)
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            codec = null
        }

        // Attempt 2: Hardware decoder with basic minimal format (no aggressive optional keys)
        try {
            val hwCodec = MediaCodec.createDecoderByType(mimeType)
            val basicFormat = MediaFormat.createVideoFormat(mimeType, width, height)
            basicFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxOf(width * height, 1024 * 1024))
            logger?.invoke("正在以标准基础参数配置硬件解码器 (${hwCodec.name})...", LogLevel.INFO)
            hwCodec.configure(basicFormat, targetSurface, null, 0)
            hwCodec.start()
            codec = hwCodec
            isRunning = true
            logger?.invoke("MediaCodec 硬件解码器 (${hwCodec.name}) 标准兼容模式启动成功", LogLevel.SUCCESS)
            return true
        } catch (e: Exception) {
            lastError = e
            logger?.invoke("硬件解码器标准模式仍失败 (${e.message})，正在搜索可用软件解码器...", LogLevel.WARN)
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            codec = null
        }

        // Attempt 3: Software decoder fallback
        val swCodecName = findSoftwareDecoder(mimeType)
        if (swCodecName != null) {
            try {
                logger?.invoke("正在尝试回退至兼容软件解码器 ($swCodecName)...", LogLevel.INFO)
                val swCodec = MediaCodec.createByCodecName(swCodecName)
                val basicFormat = MediaFormat.createVideoFormat(mimeType, width, height)
                basicFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxOf(width * height, 1024 * 1024))
                swCodec.configure(basicFormat, targetSurface, null, 0)
                swCodec.start()
                codec = swCodec
                isRunning = true
                logger?.invoke("兼容软件解码器 ($swCodecName, $mimeType) 启动成功！", LogLevel.SUCCESS)
                return true
            } catch (e: Exception) {
                lastError = e
                logger?.invoke("软件解码器启动失败 (${e.message})", LogLevel.ERROR)
                try { codec?.stop() } catch (_: Exception) {}
                try { codec?.release() } catch (_: Exception) {}
                codec = null
            }
        } else {
            logger?.invoke("系统未找到可用的软件解码器 ($mimeType)", LogLevel.WARN)
        }

        Log.e("H264Decoder", "Failed to start any MediaCodec decoder for $mimeType", lastError)
        return false
    }

    private fun findSoftwareDecoder(targetMime: String): String? {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in codecList.codecInfos) {
                if (info.isEncoder) continue
                val types = info.supportedTypes
                val supportsType = types.any { it.equals(targetMime, ignoreCase = true) }
                if (!supportsType) continue

                val name = info.name.lowercase()
                val isSoftware = if (Build.VERSION.SDK_INT >= 29) {
                    !info.isHardwareAccelerated || info.isSoftwareOnly
                } else {
                    name.startsWith("omx.google.") || name.startsWith("c2.android.") || name.contains("sw") || (!name.startsWith("omx.qcom.") && !name.startsWith("omx.exynos.") && !name.startsWith("omx.mtk."))
                }
                if (isSoftware) {
                    return info.name
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeLoop() {
        while (isRunning) {
            val currentCodec = codec ?: break
            val packet = try {
                packetQueue.poll(50, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                break
            } ?: continue

            // Video buffering delay compensation: delay decoding if videoBufferMs > 0
            val bufDelay = bufferDelayMs
            if (bufDelay > 0 && !packet.isConfig) {
                val targetTimeNs = packet.arrivalTimeNs + (bufDelay * 1_000_000L)
                val waitNs = targetTimeNs - System.nanoTime()
                if (waitNs > 1_000_000L) { // wait if > 1ms
                    try {
                        val sleepMs = waitNs / 1_000_000L
                        val sleepNs = (waitNs % 1_000_000L).toInt()
                        Thread.sleep(sleepMs, sleepNs)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }

            try {
                var inIndex = currentCodec.dequeueInputBuffer(10000) // 10ms wait
                while (inIndex < 0 && isRunning) {
                    inIndex = currentCodec.dequeueInputBuffer(5000)
                }

                if (inIndex >= 0) {
                    val inBuffer: ByteBuffer? = currentCodec.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        inBuffer.clear()
                        if (packet.data.size <= inBuffer.remaining()) {
                            inBuffer.put(packet.data)
                            val flags = if (packet.isConfig) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else (if (packet.isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                            val pts = if (packet.isConfig) 0L else packet.ptsUs
                            currentCodec.queueInputBuffer(
                                inIndex,
                                0,
                                packet.data.size,
                                pts,
                                flags
                            )
                            inputFeedCount++
                            if (inputFeedCount <= 8 || inputFeedCount % 60 == 0) {
                                val hex8 = packet.data.take(8).joinToString(" ") { String.format("%02X", it) }
                                logger?.invoke("MediaCodec 输入喂帧 #$inputFeedCount: 字节=${packet.data.size}, flags=$flags, pts=$pts, 前缀=[$hex8]", LogLevel.INFO)
                            }
                        } else {
                            logger?.invoke("警告: 帧数据大小 (${packet.data.size}) 超过解码器输入缓冲 (${inBuffer.remaining()})，丢弃此帧", LogLevel.WARN)
                            currentCodec.queueInputBuffer(inIndex, 0, 0, 0, 0)
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                logger?.invoke("MediaCodec 解码输入通道异常中断 (${e.message})，等待 Surface 重建", LogLevel.WARN)
                isRunning = false
                break
            } catch (e: Exception) {
                logger?.invoke("MediaCodec 解码输入异常: ${e.message}", LogLevel.WARN)
                Log.e("H264Decoder", "Error in decode loop", e)
            }
        }
    }

    private fun renderLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        var timeoutCount = 0
        while (isRunning) {
            val currentCodec = codec ?: break
            try {
                val outIndex = currentCodec.dequeueOutputBuffer(bufferInfo, 10000) // 10ms wait
                if (outIndex >= 0) {
                    val surf = activeSurface
                    val shouldRender = surf != null && surf.isValid
                    val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                    val render = shouldRender && !isConfig

                    try {
                        currentCodec.releaseOutputBuffer(outIndex, render)
                    } catch (e: Exception) {
                        if (render) {
                            try { currentCodec.releaseOutputBuffer(outIndex, false) } catch (_: Exception) {}
                        }
                    }
                    if (render) {
                        renderSuccessCount++
                        if (renderSuccessCount <= 8 || renderSuccessCount % 60 == 0) {
                            logger?.invoke("MediaCodec 成功出帧渲染 #$renderSuccessCount (flags=${bufferInfo.flags}, pts=${bufferInfo.presentationTimeUs}, SurfaceValid=$shouldRender)", LogLevel.SUCCESS)
                        }
                        onFrameRendered?.invoke()
                    }
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = currentCodec.outputFormat
                    val cropW = if (newFormat.containsKey("crop-right") && newFormat.containsKey("crop-left")) {
                        newFormat.getInteger("crop-right") - newFormat.getInteger("crop-left") + 1
                    } else 0
                    val cropH = if (newFormat.containsKey("crop-bottom") && newFormat.containsKey("crop-top")) {
                        newFormat.getInteger("crop-bottom") - newFormat.getInteger("crop-top") + 1
                    } else 0
                    val outW = if (cropW > 0) cropW else (if (newFormat.containsKey(MediaFormat.KEY_WIDTH)) newFormat.getInteger(MediaFormat.KEY_WIDTH) else 0)
                    val outH = if (cropH > 0) cropH else (if (newFormat.containsKey(MediaFormat.KEY_HEIGHT)) newFormat.getInteger(MediaFormat.KEY_HEIGHT) else 0)
                    logger?.invoke("MediaCodec 输出格式变更: ${outW}x${outH} ($newFormat)", LogLevel.INFO)
                    if (outW > 0 && outH > 0) {
                        onVideoSizeChanged?.invoke(outW, outH)
                    }
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    timeoutCount++
                    if (timeoutCount % 200 == 0 && renderSuccessCount == 0 && inputFeedCount > 0) {
                        logger?.invoke("MediaCodec 正在解码中 (已喂入 $inputFeedCount 帧，等待首帧解码输出...)", LogLevel.INFO)
                    }
                }
            } catch (e: IllegalStateException) {
                logger?.invoke("MediaCodec 渲染输出通道异常中断 (${e.message})，等待 Surface 重建", LogLevel.WARN)
                isRunning = false
                break
            } catch (e: Exception) {
                logger?.invoke("MediaCodec 渲染出帧异常: ${e.message}", LogLevel.WARN)
                Log.e("H264Decoder", "Error in render loop", e)
            }
        }
    }

    @Synchronized
    fun setSurface(newSurface: Surface?) {
        val oldSurface = activeSurface
        activeSurface = newSurface
        val isValid = newSurface != null && newSurface.isValid
        logger?.invoke("更新解码器渲染目标 Surface: 有效性=$isValid, Hash=${newSurface?.hashCode()}", LogLevel.INFO)
        if (newSurface != null && newSurface.isValid) {
            var needRestart = false
            val currentCodec = codec
            val isWorkerAlive = isRunning && (decodeThread?.isAlive == true) && (renderThread?.isAlive == true)

            if (currentCodec == null || !isWorkerAlive) {
                needRestart = true
            } else {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        currentCodec.setOutputSurface(newSurface)
                        logger?.invoke("MediaCodec 成功动态切换输出 Surface", LogLevel.SUCCESS)
                        Log.i("H264Decoder", "Successfully updated MediaCodec output surface dynamically")
                        // 重新注入关键配置帧与最新关键帧，并喂入推帧消除硬件解码器管线排队延迟，使新 Surface 瞬间出图
                        latestConfigPacket?.let { packetQueue.offer(it) }
                        latestKeyFramePacket?.let { kf ->
                            packetQueue.offer(kf)
                            val dup = FramePacket(kf.data, false, false, kf.ptsUs + 33_333L, System.nanoTime())
                            packetQueue.offer(dup)
                        }
                    } else {
                        needRestart = true
                    }
                } catch (e: Exception) {
                    logger?.invoke("动态切换 MediaCodec 输出 Surface 失败 (${e.message})，正在使用新 Surface 重建解码器...", LogLevel.WARN)
                    needRestart = true
                }
            }

            if (needRestart) {
                restartCodec(newSurface)
            }
        }
    }

    @Synchronized
    fun restartCodec(targetSurface: Surface): Boolean {
        logger?.invoke("正在使用新 Surface 重建硬件解码器 ($mimeType, ${width}x${height})...", LogLevel.INFO)
        isRunning = false
        decodeThread?.interrupt()
        renderThread?.interrupt()
        try { decodeThread?.join(250) } catch (_: Exception) {}
        try { renderThread?.join(250) } catch (_: Exception) {}
        decodeThread = null
        renderThread = null

        try { codec?.stop() } catch (_: Exception) {}
        try { codec?.release() } catch (_: Exception) {}
        codec = null

        activeSurface = targetSurface

        // 清理老旧积压的中间帧，重灌配置头与首帧
        packetQueue.clear()
        latestConfigPacket?.let { packetQueue.offer(it) }
        latestKeyFramePacket?.let { kf ->
            packetQueue.offer(kf)
            val dup = FramePacket(kf.data, false, false, kf.ptsUs + 33_333L, System.nanoTime())
            packetQueue.offer(dup)
        }

        val started = createAndStartCodec(targetSurface)
        if (!started) {
            logger?.invoke("重建硬件解码器失败 ($mimeType, ${width}x${height})", LogLevel.ERROR)
            return false
        }

        isRunning = true
        decodeThread = thread(name = "H264DecodeWorker") {
            decodeLoop()
        }
        renderThread = thread(name = "H264RenderWorker") {
            renderLoop()
        }

        logger?.invoke("硬件解码器已成功在新 Surface 上重建并恢复工作，画面恢复实时渲染！", LogLevel.SUCCESS)
        return true
    }

    fun decodeChunk(data: ByteArray, offset: Int, length: Int, isConfig: Boolean = false, isKeyFrame: Boolean = false, ptsUs: Long = 0L) {
        if (length <= 0) return
        val chunk = if (offset == 0 && length == data.size) data else data.copyOfRange(offset, offset + length)
        val effectivePts = if (ptsUs > 0) ptsUs else (System.nanoTime() / 1000)
        val packet = FramePacket(chunk, isConfig, isKeyFrame, effectivePts, System.nanoTime())

        if (isConfig) {
            latestConfigPacket = packet
        }
        if (isKeyFrame) {
            latestKeyFramePacket = packet
        }

        if (!isRunning) return

        // Drop non-keyframe if queue is backing up to guarantee low latency or buffer window
        val currentSize = packetQueue.size
        val maxAllowed = if (bufferDelayMs > 0) maxOf(25, (bufferDelayMs * 60 / 1000) + 12) else 4
        if (currentSize > maxAllowed && !isConfig && !isKeyFrame) {
            return
        }

        packetQueue.offer(packet)
    }

    fun stop() {
        isRunning = false
        packetQueue.clear()
        latestConfigPacket = null
        latestKeyFramePacket = null
        decodeThread?.interrupt()
        renderThread?.interrupt()
        decodeThread = null
        renderThread = null
        try {
            codec?.stop()
        } catch (_: Exception) {}
        try {
            codec?.release()
        } catch (_: Exception) {}
        codec = null
    }
}


