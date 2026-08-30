package com.yangyx.adbhelper.touch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Represents a single touch point in the multi-touch tracking system.
 */
data class RemoteTouchPoint(
    val pointerId: Long,
    val localX: Float,
    val localY: Float,
    val remoteX: Int,
    val remoteY: Int,
    val pressure: Float = 1.0f,
    val action: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Real-time statistics for multi-touch transmission.
 */
data class MultiTouchStats(
    val activePointersCount: Int = 0,
    val totalEventsSent: Long = 0L,
    val eventsPerSecond: Float = 0f,
    val lastLatencyMs: Long = 0L,
    val isOverlayVisible: Boolean = false,
    val isTrailsEnabled: Boolean = false,
    val isCoordinatesVisible: Boolean = false
)

/**
 * Predefined multi-touch finger color palette for visual distinction.
 */
object TouchColors {
    val PALETTE = listOf(
        Color(0xFF00E5FF), // Pointer 0: Vivid Cyan
        Color(0xFFFF9100), // Pointer 1: Vibrant Amber / Orange
        Color(0xFFFF007F), // Pointer 2: Neon Rose / Pink
        Color(0xFF00E676), // Pointer 3: Bright Emerald Green
        Color(0xFF7C4DFF), // Pointer 4: Electric Purple
        Color(0xFFFFD600), // Pointer 5: Bright Yellow
        Color(0xFFFF3D00), // Pointer 6: Deep Orange
        Color(0xFF00B0FF), // Pointer 7: Light Blue
        Color(0xFFE040FB), // Pointer 8: Magenta Accent
        Color(0xFF76FF03)  // Pointer 9: Lime Accent
    )

    fun getColorForPointer(pointerId: Long): Color {
        val idx = (pointerId.toInt() and 0x7FFFFFFF) % PALETTE.size
        return PALETTE[idx]
    }
}

/**
 * Multi-touch gesture types.
 */
enum class MultiTouchGestureType(val displayName: String, val description: String) {
    PINCH_ZOOM_IN("双指放大", "双指中心向外扩张捏合手势"),
    PINCH_ZOOM_OUT("双指缩小", "双指外部向中心收拢捏合手势"),
    TWO_FINGER_SCROLL_UP("双指向上滑动", "双指同步向上滚动浏览"),
    TWO_FINGER_SCROLL_DOWN("双指向下滑动", "双指同步向下滚动浏览"),
    TWO_FINGER_SCROLL_LEFT("双指向左滑动", "双指同步向左滑动"),
    TWO_FINGER_SCROLL_RIGHT("双指向右滑动", "双指同步向右滑动"),
    TWO_FINGER_TAP("双指轻触", "模拟双指同时快速点击"),
    THREE_FINGER_SWIPE_DOWN("三指下滑 (截屏/通知)", "三指自上向下滑动唤起通知或截屏"),
    THREE_FINGER_SWIPE_UP("三指上滑 (多任务)", "三指自下向上滑动切换任务"),
    FOUR_FINGER_SWIPE("四指滑动手势", "四指横向滑动快速切换应用")
}
