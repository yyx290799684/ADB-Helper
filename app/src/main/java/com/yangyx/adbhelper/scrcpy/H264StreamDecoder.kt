package com.yangyx.adbhelper.scrcpy

import android.media.MediaCodec
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
    private val onVideoSizeChanged: ((Int, Int) -> Unit)? = null,
    private val onFrameRendered: (() -> Unit)? = null,
    private val logger: ((String, LogLevel) -> Unit)? = null
) {
    data class FramePacket(
        val data: ByteArray,
        val isConfig: Boolean,
        val isKeyFrame: Boolean,
        val ptsUs: Long
    )

    private var codec: MediaCodec? = null
    @Volatile
    private var activeSurface: Surface? = surface
    @Volatile
    private var isRunning = false

    private var decodeThread: Thread? = null
    private var renderThread: Thread? = null

    private val packetQueue = LinkedBlockingQueue<FramePacket>(60)
    private var inputFeedCount = 0
    private var renderSuccessCount = 0

    fun start() {
        try {
            val format = MediaFormat.createVideoFormat(mimeType, width, height)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4 * 1024 * 1024)
            format.setInteger(MediaFormat.KEY_PRIORITY, 0) // Realtime priority
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
                } catch (_: Exception) {}
            }
            try {
                format.setInteger(MediaFormat.KEY_OPERATING_RATE, 120)
            } catch (_: Exception) {}

            val newCodec = MediaCodec.createDecoderByType(mimeType)
            val surfValid = surface != null && surface?.isValid == true
            logger?.invoke("正在配置硬件解码器: $mimeType (${width}x${height}), 绑定Surface有效性=$surfValid", LogLevel.INFO)

            newCodec.configure(format, surface, null, 0)
            newCodec.start()
            codec = newCodec
            isRunning = true

            decodeThread = thread(name = "H264DecodeWorker") {
                decodeLoop()
            }

            renderThread = thread(name = "H264RenderWorker") {
                renderLoop()
            }

            logger?.invoke("MediaCodec 硬件解码器 ($mimeType, ${width}x${height}) 启动成功，工作线程已就绪", LogLevel.SUCCESS)
        } catch (e: Exception) {
            logger?.invoke("MediaCodec 解码器启动失败: ${e.message}", LogLevel.ERROR)
            Log.e("H264Decoder", "Failed to start MediaCodec decoder", e)
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

            try {
                var inIndex = currentCodec.dequeueInputBuffer(10000) // 10ms wait
                while (inIndex < 0 && isRunning) {
                    inIndex = currentCodec.dequeueInputBuffer(5000)
                }

                if (inIndex >= 0) {
                    val inBuffer: ByteBuffer? = currentCodec.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        inBuffer.clear()
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
                    }
                }
            } catch (e: IllegalStateException) {
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

                    currentCodec.releaseOutputBuffer(outIndex, render)
                    if (render) {
                        renderSuccessCount++
                        if (renderSuccessCount <= 8 || renderSuccessCount % 60 == 0) {
                            logger?.invoke("MediaCodec 成功出帧渲染 #$renderSuccessCount (flags=${bufferInfo.flags}, pts=${bufferInfo.presentationTimeUs}, SurfaceValid=$shouldRender)", LogLevel.SUCCESS)
                        }
                        onFrameRendered?.invoke()
                    }
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = currentCodec.outputFormat
                    val outW = if (newFormat.containsKey(MediaFormat.KEY_WIDTH)) newFormat.getInteger(MediaFormat.KEY_WIDTH) else 0
                    val outH = if (newFormat.containsKey(MediaFormat.KEY_HEIGHT)) newFormat.getInteger(MediaFormat.KEY_HEIGHT) else 0
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
                break
            } catch (e: Exception) {
                logger?.invoke("MediaCodec 渲染出帧异常: ${e.message}", LogLevel.WARN)
                Log.e("H264Decoder", "Error in render loop", e)
            }
        }
    }

    fun setSurface(newSurface: Surface?) {
        activeSurface = newSurface
        val isValid = newSurface != null && newSurface.isValid
        logger?.invoke("更新解码器渲染目标 Surface: 有效性=$isValid, Hash=${newSurface?.hashCode()}", LogLevel.INFO)
        if (newSurface != null && newSurface.isValid) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    codec?.setOutputSurface(newSurface)
                    Log.i("H264Decoder", "Successfully updated MediaCodec output surface dynamically")
                }
            } catch (e: Exception) {
                logger?.invoke("动态切换 MediaCodec 输出 Surface 失败: ${e.message}", LogLevel.WARN)
                Log.e("H264Decoder", "Failed to update MediaCodec output surface", e)
            }
        }
    }

    fun decodeChunk(data: ByteArray, offset: Int, length: Int, isConfig: Boolean = false, isKeyFrame: Boolean = false, ptsUs: Long = 0L) {
        if (!isRunning || length <= 0) return
        val chunk = if (offset == 0 && length == data.size) data else data.copyOfRange(offset, offset + length)

        // Drop non-keyframe if queue is backing up to guarantee low latency
        val currentSize = packetQueue.size
        if (currentSize > 4 && !isConfig && !isKeyFrame) {
            return
        }

        val effectivePts = if (ptsUs > 0) ptsUs else (System.nanoTime() / 1000)
        packetQueue.offer(FramePacket(chunk, isConfig, isKeyFrame, effectivePts))
    }

    fun stop() {
        isRunning = false
        packetQueue.clear()
        decodeThread?.interrupt()
        renderThread?.interrupt()
        decodeThread = null
        renderThread = null
        try {
            codec?.stop()
            codec?.release()
        } catch (_: Exception) {}
        codec = null
    }
}


