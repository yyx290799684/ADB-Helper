package com.yangyx.adbhelper.touch

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Dedicated Remote Multi-Touchpad, Gesture Workspace & Diagnostic Screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun RemoteTouchpadScreen(
    multiTouchController: MultiTouchController,
    remoteWidth: Int,
    remoteHeight: Int,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("多点触控板", "快捷手势宏", "双摇杆控制", "多点监控与诊断")

    val stats by multiTouchController.stats.collectAsState()
    val activePoints by multiTouchController.activePoints.collectAsState()

    val actualRemoteW = if (remoteWidth > 0) remoteWidth else 1080
    val actualRemoteH = if (remoteHeight > 0) remoteHeight else 2400

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "远程多点触控工作区",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "远端分辨率: ${actualRemoteW}x${actualRemoteH} | 活动触点: ${activePoints.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                }
            }
        }

        // Tab Selector
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> TouchpadCanvasView(
                    controller = multiTouchController,
                    remoteWidth = actualRemoteW,
                    remoteHeight = actualRemoteH
                )
                1 -> GestureMacrosView(
                    controller = multiTouchController,
                    remoteWidth = actualRemoteW,
                    remoteHeight = actualRemoteH
                )
                2 -> VirtualDualStickView(
                    controller = multiTouchController,
                    remoteWidth = actualRemoteW,
                    remoteHeight = actualRemoteH
                )
                3 -> MultiTouchDiagnosticsView(
                    controller = multiTouchController,
                    remoteWidth = actualRemoteW,
                    remoteHeight = actualRemoteH
                )
            }
        }
    }
}

/**
 * High-sensitivity native multi-touch pad with visual feedback.
 */
@Composable
fun TouchpadCanvasView(
    controller: MultiTouchController,
    remoteWidth: Int,
    remoteHeight: Int,
    modifier: Modifier = Modifier
) {
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }
    val stats by controller.stats.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Quick control chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = stats.isOverlayVisible,
                onClick = { controller.toggleOverlayVisibility() },
                label = { Text("触点光圈", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = stats.isTrailsEnabled,
                onClick = { controller.toggleTrails() },
                label = { Text("运动轨迹", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = stats.isCoordinatesVisible,
                onClick = { controller.toggleCoordinates() },
                label = { Text("坐标详情", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Multi-touch active surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1C23),
                            Color(0xFF101217)
                        )
                    )
                )
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .onSizeChanged {
                    canvasWidth = it.width
                    canvasHeight = it.height
                }
        ) {
            // Native Android View with low-latency MotionEvent listener
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    android.view.View(context).apply {
                        setOnTouchListener { v, event ->
                            val w = v.width
                            val h = v.height
                            controller.onNativeMotionEvent(
                                event = event,
                                viewWidth = w,
                                viewHeight = h,
                                remoteWidth = remoteWidth,
                                remoteHeight = remoteHeight
                            )
                            true
                        }
                    }
                }
            )

            // Visual overlay rendering above native view
            MultiTouchVisualOverlay(
                controller = controller,
                modifier = Modifier.fillMaxSize()
            )

            // Center hint
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.PanTool,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "在此区域直接多点触摸 / 双指缩放 / 多指手势",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "支持 1~10 指并发低延迟映射至远端屏幕",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Preset gesture macro execution panel.
 */
@Composable
fun GestureMacrosView(
    controller: MultiTouchController,
    remoteWidth: Int,
    remoteHeight: Int,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "多点触控手势宏指令",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "一键向被控端设备注入标准多点触控轨迹，实现缩放、滚动与系统级手势控制。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Zoom Gestures Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("双指缩放手势 (Pinch to Zoom)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            controller.executePinchGesture(
                                centerX = remoteWidth / 2,
                                centerY = remoteHeight / 2,
                                startDistance = 200f,
                                endDistance = (remoteWidth * 0.75f),
                                remoteWidth = remoteWidth,
                                remoteHeight = remoteHeight
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("双指放大")
                    }

                    Button(
                        onClick = {
                            controller.executePinchGesture(
                                centerX = remoteWidth / 2,
                                centerY = remoteHeight / 2,
                                startDistance = (remoteWidth * 0.75f),
                                endDistance = 150f,
                                remoteWidth = remoteWidth,
                                remoteHeight = remoteHeight
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("双指缩小")
                    }
                }
            }
        }

        // 2. Two-finger Scrolling Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OpenWith, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("双指同步滚动浏览 (Two-Finger Scroll)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            controller.executeTwoFingerScroll(
                                startX = remoteWidth / 2,
                                startY = (remoteHeight * 0.7f).toInt(),
                                endX = remoteWidth / 2,
                                endY = (remoteHeight * 0.3f).toInt(),
                                remoteWidth = remoteWidth,
                                remoteHeight = remoteHeight
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("双指向上翻页")
                    }

                    OutlinedButton(
                        onClick = {
                            controller.executeTwoFingerScroll(
                                startX = remoteWidth / 2,
                                startY = (remoteHeight * 0.3f).toInt(),
                                endX = remoteWidth / 2,
                                endY = (remoteHeight * 0.7f).toInt(),
                                remoteWidth = remoteWidth,
                                remoteHeight = remoteHeight
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("双指向下翻页")
                    }
                }
            }
        }

        // 3. Multi-finger System Gestures Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("三指 / 四指系统手势 (System Gestures)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            controller.executeThreeFingerSwipe(
                                startY = (remoteHeight * 0.15f).toInt(),
                                endY = (remoteHeight * 0.75f).toInt(),
                                centerX = remoteWidth / 2,
                                remoteWidth = remoteWidth,
                                remoteHeight = remoteHeight
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("三指下滑 (截屏/通知)")
                    }

                    Button(
                        onClick = {
                            controller.executeThreeFingerSwipe(
                                startY = (remoteHeight * 0.85f).toInt(),
                                endY = (remoteHeight * 0.3f).toInt(),
                                centerX = remoteWidth / 2,
                                remoteWidth = remoteWidth,
                                remoteHeight = remoteHeight
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("三指上滑 (多任务)")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val x1 = (remoteWidth * 0.4f).toInt()
                        val y1 = (remoteHeight * 0.5f).toInt()
                        val x2 = (remoteWidth * 0.6f).toInt()
                        val y2 = (remoteHeight * 0.5f).toInt()
                        controller.executeTwoFingerTap(x1, y1, x2, y2, remoteWidth, remoteHeight)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("双指并发轻触 (Two-Finger Tap)")
                }
            }
        }
    }
}

/**
 * Virtual Dual Joystick & Gamepad with simultaneous multi-finger support.
 */
@Composable
fun VirtualDualStickView(
    controller: MultiTouchController,
    remoteWidth: Int,
    remoteHeight: Int,
    modifier: Modifier = Modifier
) {
    var stickOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 120f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status banner
        Surface(
            color = Color(0xFF1E222D),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "虚拟双摇杆与多键并发控制器",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "多点独立信道",
                    color = Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Dual controls row
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Joystick
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2330))
                    .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val diff = offset - center
                                val dist = diff.getDistance()
                                val clamped = if (dist > maxRadius) diff * (maxRadius / dist) else diff
                                stickOffset = clamped

                                // Map joystick to remote screen left joystick area (20% x, 75% y)
                                val baseRx = (remoteWidth * 0.25f).toInt()
                                val baseRy = (remoteHeight * 0.75f).toInt()
                                val normX = (stickOffset.x / maxRadius) * 200f
                                val normY = (stickOffset.y / maxRadius) * 200f
                                controller.dispatchTouch(
                                    action = 0, // DOWN
                                    pointerId = 8L,
                                    x = (baseRx + normX).toInt(),
                                    y = (baseRy + normY).toInt(),
                                    screenWidth = remoteWidth,
                                    screenHeight = remoteHeight,
                                    pressure = 1.0f
                                )
                            },
                            onDrag = { change, dragAmount ->
                                val newOffset = stickOffset + dragAmount
                                val dist = newOffset.getDistance()
                                stickOffset = if (dist > maxRadius) newOffset * (maxRadius / dist) else newOffset

                                val baseRx = (remoteWidth * 0.25f).toInt()
                                val baseRy = (remoteHeight * 0.75f).toInt()
                                val normX = (stickOffset.x / maxRadius) * 200f
                                val normY = (stickOffset.y / maxRadius) * 200f
                                controller.dispatchTouch(
                                    action = 2, // MOVE
                                    pointerId = 8L,
                                    x = (baseRx + normX).toInt(),
                                    y = (baseRy + normY).toInt(),
                                    screenWidth = remoteWidth,
                                    screenHeight = remoteHeight,
                                    pressure = 1.0f
                                )
                            },
                            onDragEnd = {
                                stickOffset = Offset.Zero
                                val baseRx = (remoteWidth * 0.25f).toInt()
                                val baseRy = (remoteHeight * 0.75f).toInt()
                                controller.dispatchTouch(
                                    action = 1, // UP
                                    pointerId = 8L,
                                    x = baseRx,
                                    y = baseRy,
                                    screenWidth = remoteWidth,
                                    screenHeight = remoteHeight,
                                    pressure = 0.0f
                                )
                            },
                            onDragCancel = {
                                stickOffset = Offset.Zero
                                val baseRx = (remoteWidth * 0.25f).toInt()
                                val baseRy = (remoteHeight * 0.75f).toInt()
                                controller.dispatchTouch(
                                    action = 1, // UP
                                    pointerId = 8L,
                                    x = baseRx,
                                    y = baseRy,
                                    screenWidth = remoteWidth,
                                    screenHeight = remoteHeight,
                                    pressure = 0.0f
                                )
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Direction indicators
                Text("▲", color = Color.White.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
                Text("▼", color = Color.White.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
                Text("◀", color = Color.White.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.CenterStart).padding(8.dp))
                Text("▶", color = Color.White.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp))

                // Thumb stick knob
                Box(
                    modifier = Modifier
                        .offset { IntOffset(stickOffset.x.toInt(), stickOffset.y.toInt()) }
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF0091EA)
                                )
                            )
                        )
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(6.dp, CircleShape)
                )
            }

            // Right Multi-Action Pad (Supports simultaneous multi-touch)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2330))
                    .border(2.dp, Color(0xFFFF007F).copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Button Y (Top)
                GamepadButton(
                    label = "Y",
                    color = Color(0xFFFFD600),
                    pointerId = 9L,
                    controller = controller,
                    remoteX = (remoteWidth * 0.85f).toInt(),
                    remoteY = (remoteHeight * 0.68f).toInt(),
                    remoteWidth = remoteWidth,
                    remoteHeight = remoteHeight,
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                )

                // Button A (Bottom)
                GamepadButton(
                    label = "A",
                    color = Color(0xFF00E676),
                    pointerId = 9L,
                    controller = controller,
                    remoteX = (remoteWidth * 0.85f).toInt(),
                    remoteY = (remoteHeight * 0.82f).toInt(),
                    remoteWidth = remoteWidth,
                    remoteHeight = remoteHeight,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                )

                // Button X (Left)
                GamepadButton(
                    label = "X",
                    color = Color(0xFF00B0FF),
                    pointerId = 9L,
                    controller = controller,
                    remoteX = (remoteWidth * 0.75f).toInt(),
                    remoteY = (remoteHeight * 0.75f).toInt(),
                    remoteWidth = remoteWidth,
                    remoteHeight = remoteHeight,
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp)
                )

                // Button B (Right)
                GamepadButton(
                    label = "B",
                    color = Color(0xFFFF1744),
                    pointerId = 9L,
                    controller = controller,
                    remoteX = (remoteWidth * 0.95f).toInt(),
                    remoteY = (remoteHeight * 0.75f).toInt(),
                    remoteWidth = remoteWidth,
                    remoteHeight = remoteHeight,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp)
                )
            }
        }

        // Bottom system keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    controller.executeTwoFingerTap(
                        (remoteWidth * 0.3f).toInt(), (remoteHeight * 0.9f).toInt(),
                        (remoteWidth * 0.7f).toInt(), (remoteHeight * 0.9f).toInt(),
                        remoteWidth, remoteHeight
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("双指返回 (Back)")
            }

            Button(
                onClick = {
                    controller.executePinchGesture(
                        remoteWidth / 2, remoteHeight / 2,
                        100f, 400f,
                        remoteWidth, remoteHeight
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("快速瞄准/缩放")
            }
        }
    }
}

@Composable
fun GamepadButton(
    label: String,
    color: Color,
    pointerId: Long,
    controller: MultiTouchController,
    remoteX: Int,
    remoteY: Int,
    remoteWidth: Int,
    remoteHeight: Int,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (isPressed) color else color.copy(alpha = 0.25f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                        controller.dispatchTouch(0, pointerId, remoteX, remoteY, remoteWidth, remoteHeight, 1.0f)
                    },
                    onDrag = { _, _ -> },
                    onDragEnd = {
                        isPressed = false
                        controller.dispatchTouch(1, pointerId, remoteX, remoteY, remoteWidth, remoteHeight, 0.0f)
                    },
                    onDragCancel = {
                        isPressed = false
                        controller.dispatchTouch(1, pointerId, remoteX, remoteY, remoteWidth, remoteHeight, 0.0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

/**
 * Diagnostics & performance telemetry for multi-touch stream.
 */
@Composable
fun MultiTouchDiagnosticsView(
    controller: MultiTouchController,
    remoteWidth: Int,
    remoteHeight: Int,
    modifier: Modifier = Modifier
) {
    val stats by controller.stats.collectAsState()
    val activePoints by controller.activePoints.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "多点触控实时遥测与数据流",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "当前活动触点",
                value = "${stats.activePointersCount} / 10",
                subtitle = "并发触控信道",
                color = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "数据发送速率",
                value = "${stats.eventsPerSecond.toInt()} msg/s",
                subtitle = "平均延迟: ${stats.lastLatencyMs} ms",
                color = Color(0xFF00E5FF),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "累计触控数据包",
                value = "${stats.totalEventsSent}",
                subtitle = "Scrcpy Opcode 0x02",
                color = Color(0xFFFF9100),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "远端屏幕映射",
                value = "${remoteWidth}x${remoteHeight}",
                subtitle = "等比坐标系",
                color = Color(0xFF7C4DFF),
                modifier = Modifier.weight(1f)
            )
        }

        // Active Pointers Table
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "活跃触点明细表 (Active Pointers)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (activePoints.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "当前无活动触点，请在触控板或屏幕区域触摸",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    activePoints.values.forEach { pt ->
                        val color = TouchColors.getColorForPointer(pt.pointerId)
                        Surface(
                            color = color.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pointer #${pt.pointerId}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "本地: (${pt.localX.toInt()}, ${pt.localY.toInt()}) → 远端: (${pt.remoteX}, ${pt.remoteY})",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "压力: ${(pt.pressure * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
