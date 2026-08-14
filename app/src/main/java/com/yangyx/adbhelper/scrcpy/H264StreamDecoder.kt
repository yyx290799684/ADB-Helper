package com.yangyx.adbhelper.scrcpy

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class H264StreamDecoder(
    private var surface: Surface?,
    private val width: Int,
    private val height: Int,
    private val onFrameRendered: (() -> Unit)? = null
) {
    data class FramePacket(
        val data: ByteArray,
        val isConfig: Boolean,
        val isKeyFrame: Boolean
    )

    private var codec: MediaCodec? = null
    private var activeSurface: Surface? = surface
    @Volatile
    private var isRunning = false
    private var renderThread: Thread? = null
    private var decodeThread: Thread? = null

    private val packetQueue = LinkedBlockingQueue<FramePacket>(30)

    fun start() {
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 2 * 1024 * 1024)
            format.setInteger(MediaFormat.KEY_PRIORITY, 0) // Realtime priority
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            } else {
                format.setInteger("low-latency", 1)
            }
            format.setInteger(MediaFormat.KEY_OPERATING_RATE, 120)

            codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                configure(format, surface, null, 0)
                start()
            }
            isRunning = true

            decodeThread = thread(name = "H264DecodeWorker") {
                decodeLoop()
            }

            renderThread = thread(name = "H264RenderLoop") {
                renderLoop()
            }
            Log.i("H264Decoder", "MediaCodec hardware low-latency decoder started (${width}x${height}) targeting Surface")
        } catch (e: Exception) {
            Log.e("H264Decoder", "Failed to start MediaCodec decoder", e)
        }
    }

    private fun decodeLoop() {
        while (isRunning) {
            val packet = try {
                packetQueue.poll(10, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                break
            } ?: continue

            val currentCodec = codec ?: break
            try {
                var inputIndex = currentCodec.dequeueInputBuffer(0)
                if (inputIndex < 0) {
                    inputIndex = currentCodec.dequeueInputBuffer(2000) // 2ms timeout
                }

                if (inputIndex >= 0) {
                    val inputBuffer = currentCodec.getInputBuffer(inputIndex) ?: continue
                    inputBuffer.clear()
                    inputBuffer.put(packet.data)
                    val flags = if (packet.isConfig) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else 0
                    currentCodec.queueInputBuffer(
                        inputIndex, 0, packet.data.size,
                        System.nanoTime() / 1000, flags
                    )
                }
            } catch (e: IllegalStateException) {
                break
            } catch (e: Exception) {
                Log.e("H264Decoder", "Error in decode loop", e)
            }
        }
    }

    private fun renderLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRunning) {
            val currentCodec = codec ?: break
            try {
                val outputIndex = currentCodec.dequeueOutputBuffer(bufferInfo, 2000) // 2ms timeout
                if (outputIndex >= 0) {
                    val shouldRender = activeSurface != null && activeSurface?.isValid == true
                    val render = shouldRender && bufferInfo.size > 0
                    currentCodec.releaseOutputBuffer(outputIndex, render)
                    if (render) {
                        onFrameRendered?.invoke()
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i("H264Decoder", "Output format changed: ${currentCodec.outputFormat}")
                }
            } catch (e: IllegalStateException) {
                break
            } catch (e: Exception) {
                Log.e("H264Decoder", "Error in render loop", e)
                break
            }
        }
    }

    fun setSurface(newSurface: Surface?) {
        activeSurface = newSurface
        if (newSurface != null && newSurface.isValid) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    codec?.setOutputSurface(newSurface)
                    Log.i("H264Decoder", "Successfully updated MediaCodec output surface dynamically")
                }
            } catch (e: Exception) {
                Log.e("H264Decoder", "Failed to update MediaCodec output surface", e)
            }
        }
    }

    fun decodeChunk(data: ByteArray, offset: Int, length: Int, isConfig: Boolean = false, isKeyFrame: Boolean = false) {
        if (!isRunning || length <= 0) return
        val chunk = if (offset == 0 && length == data.size) data else data.copyOfRange(offset, offset + length)
        
        // Zero-Latency Catch-up: If queue is backing up, drop stale P-frames
        val currentSize = packetQueue.size
        if (currentSize > 2 && !isConfig && !isKeyFrame) {
            // Drop normal P-frame when lagging
            return
        }

        if (currentSize > 4) {
            // Drain queue up to keyframe/config
            val temp = mutableListOf<FramePacket>()
            packetQueue.drainTo(temp)
            for (p in temp) {
                if (p.isConfig || p.isKeyFrame) {
                    packetQueue.offer(p)
                }
            }
        }

        packetQueue.offer(FramePacket(chunk, isConfig, isKeyFrame))
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
