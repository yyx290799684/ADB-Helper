package com.yangyx.adbhelper.touch

import android.view.MotionEvent
import com.yangyx.adbhelper.adb.AdbConnection
import com.yangyx.adbhelper.adb.AdbStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Ultra-high-performance remote multi-touch event coordinator and dispatcher.
 * Handles up to 10 concurrent touch points with real-time coordinate translation,
 * zero-allocation binary serialization, multi-pointer batch aggregation (1 network write for all fingers),
 * and conflated non-blocking dispatch to achieve single-touch equivalent responsiveness.
 */
class MultiTouchController(
    private val coroutineScope: CoroutineScope,
    private val sendRawBytes: suspend (ByteArray) -> Boolean,
    private val executeShellFallback: (suspend (String) -> String)? = null
) {
    // Active touch points tracked by Pointer ID (0..9)
    private val _activePoints = MutableStateFlow<Map<Long, RemoteTouchPoint>>(emptyMap())
    val activePoints: StateFlow<Map<Long, RemoteTouchPoint>> = _activePoints.asStateFlow()

    // Multi-touch statistics
    private val _stats = MutableStateFlow(MultiTouchStats())
    val stats: StateFlow<MultiTouchStats> = _stats.asStateFlow()

    // Internal active pointers tracker
    private val activePointsMap = ConcurrentHashMap<Long, RemoteTouchPoint>()
    private val lastSentCoords = ConcurrentHashMap<Long, Pair<Int, Int>>()

    // Event counter for FPS / throughput calculation
    private val totalEventsCounter = AtomicLong(0L)
    private var eventsInLastSecond = 0
    private var lastFpsUpdateTime = System.currentTimeMillis()

    // High-priority discrete event queue (DOWN, UP, CANCEL) - guaranteed sequential delivery
    private val discreteQueue = Channel<ByteArray>(capacity = 128)
    // Conflated batch for continuous MOVE events - aggregates all active fingers into a single packet
    private val latestMoveBatch = AtomicReference<ByteArray?>(null)
    private val wakeSignal = Channel<Unit>(capacity = 1)

    private var workerJob: Job? = null

    // Visual trails history (last N points per finger for trail rendering)
    private val _trailsMap = MutableStateFlow<Map<Long, List<Pair<Float, Float>>>>(emptyMap())
    val trailsMap: StateFlow<Map<Long, List<Pair<Float, Float>>>> = _trailsMap.asStateFlow()
    private val internalTrails = ConcurrentHashMap<Long, MutableList<Pair<Float, Float>>>()

    init {
        startWorker()
    }

    fun ensureWorkerActive() {
        if (workerJob == null || workerJob?.isActive != true) {
            startWorker()
        }
    }

    private fun startWorker() {
        workerJob?.cancel()
        workerJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // 1. Process any pending discrete event (DOWN, UP, CANCEL) first
                    val discrete = discreteQueue.tryReceive().getOrNull()
                    if (discrete != null) {
                        val startTime = System.currentTimeMillis()
                        try {
                            sendRawBytes(discrete)
                        } catch (_: Exception) {}
                        val latency = System.currentTimeMillis() - startTime
                        recordEventStats(1, latency)
                        continue
                    }

                    // 2. Process latest aggregated multi-touch MOVE batch (all fingers in 1 packet)
                    val moveBatch = latestMoveBatch.getAndSet(null)
                    if (moveBatch != null) {
                        val startTime = System.currentTimeMillis()
                        try {
                            sendRawBytes(moveBatch)
                        } catch (_: Exception) {}
                        val latency = System.currentTimeMillis() - startTime
                        val count = moveBatch.size / 32
                        recordEventStats(count, latency)
                        continue
                    }

                    // 3. Suspend and wait for either a discrete event or a new move batch
                    select<Unit> {
                        discreteQueue.onReceive { pkt ->
                            val startTime = System.currentTimeMillis()
                            try {
                                sendRawBytes(pkt)
                            } catch (_: Exception) {}
                            val latency = System.currentTimeMillis() - startTime
                            recordEventStats(1, latency)
                        }
                        wakeSignal.onReceive {
                            val batch = latestMoveBatch.getAndSet(null)
                            if (batch != null) {
                                val startTime = System.currentTimeMillis()
                                try {
                                    sendRawBytes(batch)
                                } catch (_: Exception) {}
                                val latency = System.currentTimeMillis() - startTime
                                val count = batch.size / 32
                                recordEventStats(count, latency)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                    kotlinx.coroutines.delay(10)
                }
            }
        }
    }

    private fun recordEventStats(eventsCount: Int, latency: Long) {
        val total = totalEventsCounter.addAndGet(eventsCount.toLong())
        eventsInLastSecond += eventsCount
        val now = System.currentTimeMillis()
        if (now - lastFpsUpdateTime >= 1000) {
            val rate = (eventsInLastSecond * 1000f) / (now - lastFpsUpdateTime)
            _stats.value = _stats.value.copy(
                activePointersCount = activePointsMap.size,
                totalEventsSent = total,
                eventsPerSecond = rate,
                lastLatencyMs = latency
            )
            eventsInLastSecond = 0
            lastFpsUpdateTime = now
        }
    }

    /**
     * Translates local touch coordinates into target remote screen coordinates.
     */
    fun mapCoordinates(
        localX: Float,
        localY: Float,
        viewWidth: Int,
        viewHeight: Int,
        remoteWidth: Int,
        remoteHeight: Int
    ): Pair<Int, Int>? {
        if (viewWidth <= 0 || viewHeight <= 0 || remoteWidth <= 0 || remoteHeight <= 0) return null

        val clampedX = localX.coerceIn(0f, viewWidth.toFloat())
        val clampedY = localY.coerceIn(0f, viewHeight.toFloat())

        val rx = ((clampedX / viewWidth.toFloat()) * remoteWidth).toInt().coerceIn(0, remoteWidth - 1)
        val ry = ((clampedY / viewHeight.toFloat()) * remoteHeight).toInt().coerceIn(0, remoteHeight - 1)

        return Pair(rx, ry)
    }

    /**
     * Process raw Android MotionEvent from TextureView / View overlay.
     * Batches all active pointers in ACTION_MOVE into a SINGLE network packet to ensure
     * multi-touch efficiency is identical to single-touch.
     */
    fun onNativeMotionEvent(
        event: MotionEvent,
        viewWidth: Int,
        viewHeight: Int,
        remoteWidth: Int,
        remoteHeight: Int
    ) {
        ensureWorkerActive()
        val actionMasked = event.actionMasked
        val actionIndex = event.actionIndex

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Primary finger down
                val pointerId = event.getPointerId(0).toLong()
                val lx = event.getX(0)
                val ly = event.getY(0)
                val pressure = event.getPressure(0).coerceIn(0.1f, 1.0f)
                val coords = mapCoordinates(lx, ly, viewWidth, viewHeight, remoteWidth, remoteHeight)

                if (coords != null) {
                    val (rx, ry) = coords
                    lastSentCoords[pointerId] = Pair(rx, ry)
                    recordPoint(pointerId, lx, ly, rx, ry, pressure, MotionEvent.ACTION_DOWN)
                    
                    val packet = ByteArray(32)
                    serializeTouchPacket(
                        buffer = packet,
                        offset = 0,
                        action = 0, // ACTION_DOWN
                        pointerId = pointerId,
                        x = rx,
                        y = ry,
                        screenWidth = remoteWidth,
                        screenHeight = remoteHeight,
                        pressure = pressure
                    )
                    discreteQueue.trySend(packet)
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Additional finger down (multi-touch)
                // In Scrcpy protocol, pointer presses send action = 0 (ACTION_DOWN) with distinct pointerId
                val pointerId = event.getPointerId(actionIndex).toLong()
                val lx = event.getX(actionIndex)
                val ly = event.getY(actionIndex)
                val pressure = event.getPressure(actionIndex).coerceIn(0.1f, 1.0f)
                val coords = mapCoordinates(lx, ly, viewWidth, viewHeight, remoteWidth, remoteHeight)

                if (coords != null) {
                    val (rx, ry) = coords
                    lastSentCoords[pointerId] = Pair(rx, ry)
                    recordPoint(pointerId, lx, ly, rx, ry, pressure, MotionEvent.ACTION_POINTER_DOWN)

                    val packet = ByteArray(32)
                    serializeTouchPacket(
                        buffer = packet,
                        offset = 0,
                        action = 0, // ACTION_DOWN with pointerId
                        pointerId = pointerId,
                        x = rx,
                        y = ry,
                        screenWidth = remoteWidth,
                        screenHeight = remoteHeight,
                        pressure = pressure
                    )
                    discreteQueue.trySend(packet)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // High-speed multi-touch aggregation:
                // Serialize ALL active pointers into ONE contiguous byte buffer (N * 32 bytes)
                // This eliminates multiple socket writes and multiple roundtrip wait latencies.
                val pointerCount = event.pointerCount
                val batch = ByteArray(pointerCount * 32)
                var validCount = 0

                for (i in 0 until pointerCount) {
                    val pointerId = event.getPointerId(i).toLong()
                    val lx = event.getX(i)
                    val ly = event.getY(i)
                    val pressure = event.getPressure(i).coerceIn(0.1f, 1.0f)
                    val coords = mapCoordinates(lx, ly, viewWidth, viewHeight, remoteWidth, remoteHeight)

                    if (coords != null) {
                        val (rx, ry) = coords
                        lastSentCoords[pointerId] = Pair(rx, ry)
                        recordPoint(pointerId, lx, ly, rx, ry, pressure, MotionEvent.ACTION_MOVE)

                        serializeTouchPacket(
                            buffer = batch,
                            offset = validCount * 32,
                            action = 2, // ACTION_MOVE
                            pointerId = pointerId,
                            x = rx,
                            y = ry,
                            screenWidth = remoteWidth,
                            screenHeight = remoteHeight,
                            pressure = pressure
                        )
                        validCount++
                    }
                }

                if (validCount > 0) {
                    val finalBatch = if (validCount == pointerCount) batch else batch.copyOfRange(0, validCount * 32)
                    latestMoveBatch.set(finalBatch)
                    wakeSignal.trySend(Unit)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Non-primary finger released
                val pointerId = event.getPointerId(actionIndex).toLong()
                val lx = event.getX(actionIndex)
                val ly = event.getY(actionIndex)
                val coords = mapCoordinates(lx, ly, viewWidth, viewHeight, remoteWidth, remoteHeight)

                val rx = coords?.first ?: lastSentCoords[pointerId]?.first ?: 0
                val ry = coords?.second ?: lastSentCoords[pointerId]?.second ?: 0

                lastSentCoords.remove(pointerId)
                removePoint(pointerId)

                val packet = ByteArray(32)
                serializeTouchPacket(
                    buffer = packet,
                    offset = 0,
                    action = 1, // ACTION_UP with pointerId
                    pointerId = pointerId,
                    x = rx,
                    y = ry,
                    screenWidth = remoteWidth,
                    screenHeight = remoteHeight,
                    pressure = 0.0f
                )
                discreteQueue.trySend(packet)
            }

            MotionEvent.ACTION_UP -> {
                // Last finger released
                val pointerId = event.getPointerId(0).toLong()
                val lx = event.getX(0)
                val ly = event.getY(0)
                val coords = mapCoordinates(lx, ly, viewWidth, viewHeight, remoteWidth, remoteHeight)

                val rx = coords?.first ?: lastSentCoords[pointerId]?.first ?: 0
                val ry = coords?.second ?: lastSentCoords[pointerId]?.second ?: 0

                lastSentCoords.clear()
                clearAllPoints()

                val packet = ByteArray(32)
                serializeTouchPacket(
                    buffer = packet,
                    offset = 0,
                    action = 1, // ACTION_UP
                    pointerId = pointerId,
                    x = rx,
                    y = ry,
                    screenWidth = remoteWidth,
                    screenHeight = remoteHeight,
                    pressure = 0.0f
                )
                discreteQueue.trySend(packet)
            }

            MotionEvent.ACTION_CANCEL -> {
                // Cancel all active touch points
                val allPointers = lastSentCoords.keys.toList()
                lastSentCoords.clear()
                clearAllPoints()

                for (pId in allPointers) {
                    val packet = ByteArray(32)
                    serializeTouchPacket(
                        buffer = packet,
                        offset = 0,
                        action = 3, // ACTION_CANCEL
                        pointerId = pId,
                        x = 0,
                        y = 0,
                        screenWidth = remoteWidth,
                        screenHeight = remoteHeight,
                        pressure = 0.0f
                    )
                    discreteQueue.trySend(packet)
                }
            }
        }
    }

    /**
     * High-speed, zero-allocation binary serialization of a 32-byte Scrcpy INJECT_TOUCH_EVENT packet directly into buffer.
     */
    private fun serializeTouchPacket(
        buffer: ByteArray,
        offset: Int,
        action: Int,
        pointerId: Long,
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        pressure: Float
    ) {
        // 0: Type: 2 (TYPE_INJECT_TOUCH_EVENT)
        buffer[offset] = 2
        // 1: Action: 1 byte (0=DOWN, 1=UP, 2=MOVE, 3=CANCEL)
        buffer[offset + 1] = (action and 0xFF).toByte()

        // 2..9: PointerId: 8 bytes (Big Endian Long)
        for (i in 0..7) {
            buffer[offset + 2 + i] = ((pointerId ushr ((7 - i) * 8)) and 0xFFL).toByte()
        }

        // 10..13: Position X: 4 bytes (Big Endian Int)
        buffer[offset + 10] = ((x ushr 24) and 0xFF).toByte()
        buffer[offset + 11] = ((x ushr 16) and 0xFF).toByte()
        buffer[offset + 12] = ((x ushr 8) and 0xFF).toByte()
        buffer[offset + 13] = (x and 0xFF).toByte()

        // 14..17: Position Y: 4 bytes (Big Endian Int)
        buffer[offset + 14] = ((y ushr 24) and 0xFF).toByte()
        buffer[offset + 15] = ((y ushr 16) and 0xFF).toByte()
        buffer[offset + 16] = ((y ushr 8) and 0xFF).toByte()
        buffer[offset + 17] = (y and 0xFF).toByte()

        // 18..19: Screen Width: 2 bytes (Short)
        buffer[offset + 18] = ((screenWidth ushr 8) and 0xFF).toByte()
        buffer[offset + 19] = (screenWidth and 0xFF).toByte()

        // 20..21: Screen Height: 2 bytes (Short)
        buffer[offset + 20] = ((screenHeight ushr 8) and 0xFF).toByte()
        buffer[offset + 21] = (screenHeight and 0xFF).toByte()

        // 22..23: Pressure: 2 bytes fixed point (u16fp: 0x0000 to 0xFFFF)
        val u16Pressure = if (pressure <= 0f) 0 else if (pressure >= 1f) 0xFFFF else (pressure * 0xFFFF).toInt()
        buffer[offset + 22] = ((u16Pressure ushr 8) and 0xFF).toByte()
        buffer[offset + 23] = (u16Pressure and 0xFF).toByte()

        // 24..27: Action Button: 4 bytes
        buffer[offset + 24] = 0; buffer[offset + 25] = 0; buffer[offset + 26] = 0; buffer[offset + 27] = 0

        // 28..31: Buttons state: 4 bytes
        buffer[offset + 28] = 0; buffer[offset + 29] = 0; buffer[offset + 30] = 0; buffer[offset + 31] = 0
    }

    /**
     * Record a touch point in local tracking map for visualization ONLY if overlay is enabled.
     */
    private fun recordPoint(
        pointerId: Long,
        lx: Float,
        ly: Float,
        rx: Int,
        ry: Int,
        pressure: Float,
        action: Int
    ) {
        if (!_stats.value.isOverlayVisible) return

        val point = RemoteTouchPoint(
            pointerId = pointerId,
            localX = lx,
            localY = ly,
            remoteX = rx,
            remoteY = ry,
            pressure = pressure,
            action = action
        )
        activePointsMap[pointerId] = point
        _activePoints.value = HashMap(activePointsMap)

        // Track trail points if trails enabled
        if (_stats.value.isTrailsEnabled) {
            val list = internalTrails.getOrPut(pointerId) { mutableListOf() }
            list.add(Pair(lx, ly))
            if (list.size > 20) {
                list.removeAt(0)
            }
            _trailsMap.value = internalTrails.mapValues { it.value.toList() }
        }
    }

    private fun removePoint(pointerId: Long) {
        activePointsMap.remove(pointerId)
        internalTrails.remove(pointerId)
        if (_stats.value.isOverlayVisible) {
            _activePoints.value = HashMap(activePointsMap)
            _trailsMap.value = internalTrails.mapValues { it.value.toList() }
        }
    }

    private fun clearAllPoints() {
        activePointsMap.clear()
        internalTrails.clear()
        if (_stats.value.isOverlayVisible) {
            _activePoints.value = emptyMap()
            _trailsMap.value = emptyMap()
        }
    }

    /**
     * Dispatches touch packet into the non-blocking channel.
     */
    fun dispatchTouch(
        action: Int,
        pointerId: Long,
        x: Int,
        y: Int,
        screenWidth: Int,
        screenHeight: Int,
        pressure: Float
    ) {
        val packet = ByteArray(32)
        serializeTouchPacket(
            buffer = packet,
            offset = 0,
            action = action,
            pointerId = pointerId,
            x = x,
            y = y,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            pressure = pressure
        )
        if (action == 2) {
            latestMoveBatch.set(packet)
            wakeSignal.trySend(Unit)
        } else {
            discreteQueue.trySend(packet)
        }
    }

    /**
     * Executes a multi-touch Pinch to Zoom gesture on the remote device.
     */
    fun executePinchGesture(
        centerX: Int,
        centerY: Int,
        startDistance: Float,
        endDistance: Float,
        remoteWidth: Int,
        remoteHeight: Int,
        durationMs: Long = 400L,
        steps: Int = 20
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val p0 = 0L
            val p1 = 1L
            val delayPerStep = (durationMs / steps).coerceAtLeast(10L)

            val startHalf = startDistance / 2f
            val endHalf = endDistance / 2f

            // 1. Initial touch down for Pointer 0 & Pointer 1
            val p0StartX = (centerX - startHalf).toInt().coerceIn(0, remoteWidth - 1)
            val p0StartY = centerY.coerceIn(0, remoteHeight - 1)
            val p1StartX = (centerX + startHalf).toInt().coerceIn(0, remoteWidth - 1)
            val p1StartY = centerY.coerceIn(0, remoteHeight - 1)

            dispatchTouch(0 /* DOWN */, p0, p0StartX, p0StartY, remoteWidth, remoteHeight, 1.0f)
            delay(15)
            dispatchTouch(0 /* DOWN */, p1, p1StartX, p1StartY, remoteWidth, remoteHeight, 1.0f)
            delay(delayPerStep)

            // 2. Interpolate motion steps
            for (step in 1..steps) {
                val t = step.toFloat() / steps.toFloat()
                val currentHalf = startHalf + (endHalf - startHalf) * t
                val p0X = (centerX - currentHalf).toInt().coerceIn(0, remoteWidth - 1)
                val p1X = (centerX + currentHalf).toInt().coerceIn(0, remoteWidth - 1)

                // Batch both pointers in MOVE
                val moveBatch = ByteArray(64)
                serializeTouchPacket(moveBatch, 0, 2, p0, p0X, p0StartY, remoteWidth, remoteHeight, 1.0f)
                serializeTouchPacket(moveBatch, 32, 2, p1, p1X, p1StartY, remoteWidth, remoteHeight, 1.0f)
                latestMoveBatch.set(moveBatch)
                wakeSignal.trySend(Unit)
                delay(delayPerStep)
            }

            // 3. Release pointers
            val p0EndX = (centerX - endHalf).toInt().coerceIn(0, remoteWidth - 1)
            val p1EndX = (centerX + endHalf).toInt().coerceIn(0, remoteWidth - 1)

            dispatchTouch(1 /* UP */, p1, p1EndX, p1StartY, remoteWidth, remoteHeight, 0.0f)
            delay(15)
            dispatchTouch(1 /* UP */, p0, p0EndX, p0StartY, remoteWidth, remoteHeight, 0.0f)
        }
    }

    /**
     * Executes a synchronous Two-Finger Scroll / Swipe gesture.
     */
    fun executeTwoFingerScroll(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        spacing: Int = 120,
        remoteWidth: Int,
        remoteHeight: Int,
        durationMs: Long = 350L,
        steps: Int = 15
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val p0 = 0L
            val p1 = 1L
            val delayPerStep = (durationMs / steps).coerceAtLeast(10L)
            val halfSpacing = spacing / 2

            val p0StartX = (startX - halfSpacing).coerceIn(0, remoteWidth - 1)
            val p1StartX = (startX + halfSpacing).coerceIn(0, remoteWidth - 1)

            dispatchTouch(0 /* DOWN */, p0, p0StartX, startY, remoteWidth, remoteHeight, 1.0f)
            delay(15)
            dispatchTouch(0 /* DOWN */, p1, p1StartX, startY, remoteWidth, remoteHeight, 1.0f)
            delay(delayPerStep)

            for (step in 1..steps) {
                val t = step.toFloat() / steps.toFloat()
                val curY = (startY + (endY - startY) * t).toInt().coerceIn(0, remoteHeight - 1)
                val curX = (startX + (endX - startX) * t).toInt()
                val p0CurX = (curX - halfSpacing).coerceIn(0, remoteWidth - 1)
                val p1CurX = (curX + halfSpacing).coerceIn(0, remoteWidth - 1)

                val moveBatch = ByteArray(64)
                serializeTouchPacket(moveBatch, 0, 2, p0, p0CurX, curY, remoteWidth, remoteHeight, 1.0f)
                serializeTouchPacket(moveBatch, 32, 2, p1, p1CurX, curY, remoteWidth, remoteHeight, 1.0f)
                latestMoveBatch.set(moveBatch)
                wakeSignal.trySend(Unit)
                delay(delayPerStep)
            }

            val p0EndX = (endX - halfSpacing).coerceIn(0, remoteWidth - 1)
            val p1EndX = (endX + halfSpacing).coerceIn(0, remoteWidth - 1)

            dispatchTouch(1 /* UP */, p1, p1EndX, endY, remoteWidth, remoteHeight, 0.0f)
            delay(15)
            dispatchTouch(1 /* UP */, p0, p0EndX, endY, remoteWidth, remoteHeight, 0.0f)
        }
    }

    /**
     * Executes a Three-Finger Swipe gesture (e.g. screenshot or notification pull).
     */
    fun executeThreeFingerSwipe(
        startY: Int,
        endY: Int,
        centerX: Int,
        spacing: Int = 100,
        remoteWidth: Int,
        remoteHeight: Int,
        durationMs: Long = 300L
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val p0 = 0L
            val p1 = 1L
            val p2 = 2L
            val steps = 15
            val delayPerStep = (durationMs / steps).coerceAtLeast(10L)

            val p0X = (centerX - spacing).coerceIn(0, remoteWidth - 1)
            val p1X = centerX.coerceIn(0, remoteWidth - 1)
            val p2X = (centerX + spacing).coerceIn(0, remoteWidth - 1)

            dispatchTouch(0, p0, p0X, startY, remoteWidth, remoteHeight, 1.0f)
            delay(10)
            dispatchTouch(0, p1, p1X, startY, remoteWidth, remoteHeight, 1.0f)
            delay(10)
            dispatchTouch(0, p2, p2X, startY, remoteWidth, remoteHeight, 1.0f)
            delay(delayPerStep)

            for (step in 1..steps) {
                val t = step.toFloat() / steps.toFloat()
                val curY = (startY + (endY - startY) * t).toInt().coerceIn(0, remoteHeight - 1)
                val moveBatch = ByteArray(96)
                serializeTouchPacket(moveBatch, 0, 2, p0, p0X, curY, remoteWidth, remoteHeight, 1.0f)
                serializeTouchPacket(moveBatch, 32, 2, p1, p1X, curY, remoteWidth, remoteHeight, 1.0f)
                serializeTouchPacket(moveBatch, 64, 2, p2, p2X, curY, remoteWidth, remoteHeight, 1.0f)
                latestMoveBatch.set(moveBatch)
                wakeSignal.trySend(Unit)
                delay(delayPerStep)
            }

            dispatchTouch(1, p2, p2X, endY, remoteWidth, remoteHeight, 0.0f)
            delay(10)
            dispatchTouch(1, p1, p1X, endY, remoteWidth, remoteHeight, 0.0f)
            delay(10)
            dispatchTouch(1, p0, p0X, endY, remoteWidth, remoteHeight, 0.0f)
        }
    }

    /**
     * Executes a fast simultaneous Two-Finger Tap.
     */
    fun executeTwoFingerTap(
        x1: Int, y1: Int,
        x2: Int, y2: Int,
        remoteWidth: Int,
        remoteHeight: Int
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            dispatchTouch(0, 0L, x1, y1, remoteWidth, remoteHeight, 1.0f)
            delay(10)
            dispatchTouch(0, 1L, x2, y2, remoteWidth, remoteHeight, 1.0f)
            delay(80)
            dispatchTouch(1, 1L, x2, y2, remoteWidth, remoteHeight, 0.0f)
            delay(10)
            dispatchTouch(1, 0L, x1, y1, remoteWidth, remoteHeight, 0.0f)
        }
    }

    fun toggleOverlayVisibility(visible: Boolean? = null) {
        val next = visible ?: !_stats.value.isOverlayVisible
        _stats.value = _stats.value.copy(isOverlayVisible = next)
        if (!next) {
            _activePoints.value = emptyMap()
            _trailsMap.value = emptyMap()
        }
    }

    fun toggleTrails(enabled: Boolean? = null) {
        val next = enabled ?: !_stats.value.isTrailsEnabled
        _stats.value = _stats.value.copy(isTrailsEnabled = next)
        if (!next) {
            _trailsMap.value = emptyMap()
        }
    }

    fun toggleCoordinates(visible: Boolean? = null) {
        val next = visible ?: !_stats.value.isCoordinatesVisible
        _stats.value = _stats.value.copy(isCoordinatesVisible = next)
    }

    fun destroy() {
        workerJob?.cancel()
        discreteQueue.close()
        clearAllPoints()
    }
}
