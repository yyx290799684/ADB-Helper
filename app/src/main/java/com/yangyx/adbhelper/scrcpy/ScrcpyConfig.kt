package com.yangyx.adbhelper.scrcpy

enum class HostOrientationMode(val label: String, val shortDesc: String) {
    AUTO("自动适应", "主控端自动匹配被控端横竖屏"),
    SENSOR("重力感应", "主控端跟随本机陀螺仪自由旋转"),
    PORTRAIT("锁定竖屏", "主控端保持竖屏显示"),
    LANDSCAPE("锁定横屏", "主控端保持横屏全景显示")
}

data class ScrcpyConfig(
    val bitrate: Int = 4000000,           // Default 4 Mbps
    val maxResolution: Int = 1080,        // Default 1080p (0 = native)
    val maxFps: Int = 60,                 // Default 60 FPS
    val videoCodec: String = "h264",      // Video codec: "h264", "h265", "av1"
    val isScreenOff: Boolean = false,     // Screen off control (熄屏控制 --turn-screen-off)
    val isStayAwake: Boolean = true,      // Stay awake (阻止休眠 --stay-awake)
    val isPowerOn: Boolean = true,        // Power on at start (启动时点亮屏幕 / false = --no-power-on)
    val isPowerOffOnClose: Boolean = false, // Power off on close (关闭时息屏 --power-off-on-close)
    val isAudioEnabled: Boolean = false,  // Audio forwarding toggle
    val autoRotateHost: Boolean = true,   // Auto rotate host screen to follow remote orientation
    val hostOrientationMode: HostOrientationMode = HostOrientationMode.AUTO, // Orientation strategy
    val virtualDisplayWidth: Int = 0,     // Virtual display width (0 = disabled)
    val virtualDisplayHeight: Int = 0,    // Virtual display height
    val launchPackageOnVirtualDisplay: String = "", // Package to launch on virtual display

    // Video Source & Camera Mirroring Options (Android 12+)
    val videoSource: String = "display",   // "display" or "camera"
    val cameraFacing: String = "back",     // "front", "back", "external", "any"
    val cameraId: String = "",             // Explicit camera ID (e.g. "0", "1", "2")
    val cameraSize: String = "",           // Explicit camera size (e.g. "1920x1080", "1280x720")
    val cameraAspectRatio: String = "",    // "16:9", "4:3", "sensor"
    val cameraFps: Int = 0,                // Camera FPS (0 = default/auto)
    val cameraHighSpeed: Boolean = false,  // High speed capture mode
    val cameraTorch: Boolean = false,      // Camera torch at startup
    val cameraZoom: Float = 1.0f           // Camera zoom level (1.0f - 5.0f)
)

