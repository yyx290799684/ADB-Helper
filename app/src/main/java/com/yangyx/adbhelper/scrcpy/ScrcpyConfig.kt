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
    val isKeepActive: Boolean = true,     // Keep active (阻止休眠/保持活跃 --keep-active，无需插电亦可防休眠)
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
    val cameraZoom: Float = 1.0f,          // Camera zoom level (1.0f - 5.0f)

    // Experimental Settings (实验性设置)
    val videoEncoder: String = "",         // Explicit video encoder, e.g. "OMX.qcom.video.encoder.avc" ("" = default)
    val isVideoBufferEnabled: Boolean = false, // Video buffer toggle (--video-buffer)
    val videoBufferMs: Int = 50,           // Video buffer duration in milliseconds (10..200 ms, default 50 ms)

    // Toolbar & UI State Persistence (工具栏排布、折叠与按键自定义)
    val isTopBarExpanded: Boolean = true,       // 顶部帧率与信息条展开状态
    val isToolbarExpanded: Boolean = true,      // 底部工具栏展开状态
    val isToolbarDoubleRow: Boolean = false,    // 底部工具栏双排显示 (双排上下滑动 / 单排左右滑动)
    val toolbarButtonOrder: String = "",        // 逗号分隔自定义按钮排序
    val toolbarHiddenButtons: Set<String> = emptySet() // 隐藏的按钮ID集合
) {
    val isStayAwake: Boolean get() = isKeepActive

    fun getEffectiveToolbarOrder(): List<String> {
        val defaultList = DEFAULT_TOOLBAR_BUTTON_IDS
        if (toolbarButtonOrder.isBlank()) return defaultList
        val customList = toolbarButtonOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .map { if (it == "stay_awake") "keep_active" else it }
        val result = customList.filter { it in defaultList }.toMutableList()
        defaultList.forEach { if (it !in result) result.add(it) }
        return result
    }

    fun isButtonVisible(buttonId: String): Boolean {
        val mappedId = if (buttonId == "stay_awake") "keep_active" else buttonId
        return mappedId !in toolbarHiddenButtons
    }
}

val DEFAULT_TOOLBAR_BUTTON_IDS = listOf(
    "back",
    "home",
    "recents",
    "wake_up",
    "orientation",
    "multi_touch",
    "keep_active",
    "text_input",
    "screen_off",
    "volume_down",
    "volume_up",
    "volume_panel",
    "audio_stream",
    "virtual_display",
    "logs",
    "settings",
    "fullscreen",
    "disconnect",
    "collapse"
)

data class ToolbarButtonMeta(
    val id: String,
    val name: String,
    val detail: String,
    val isDisplayOnly: Boolean = false
)

val TOOLBAR_BUTTON_METAS = listOf(
    ToolbarButtonMeta("back", "返回", "返回键 (Back)", isDisplayOnly = true),
    ToolbarButtonMeta("home", "主页", "主页键 (Home)", isDisplayOnly = true),
    ToolbarButtonMeta("recents", "多任务", "多任务 / 最近应用 (Recents)", isDisplayOnly = true),
    ToolbarButtonMeta("wake_up", "唤醒屏幕", "唤醒并点亮对端屏幕", isDisplayOnly = true),
    ToolbarButtonMeta("orientation", "屏幕方向", "屏幕方向自适应与旋转控制", isDisplayOnly = true),
    ToolbarButtonMeta("multi_touch", "多点触控", "多点触控与手势辅助设置", isDisplayOnly = true),
    ToolbarButtonMeta("keep_active", "保持活跃", "阻止设备休眠 (keep-active)", isDisplayOnly = true),
    ToolbarButtonMeta("stay_awake", "保持活跃", "阻止设备休眠 (keep-active)", isDisplayOnly = true),
    ToolbarButtonMeta("text_input", "文字输入", "发送文本/剪贴板内容到远端", isDisplayOnly = true),
    ToolbarButtonMeta("screen_off", "熄屏控制", "熄灭对端实体屏幕 (仅镜像)", isDisplayOnly = true),
    ToolbarButtonMeta("volume_down", "音量-", "降低对端系统硬件音量"),
    ToolbarButtonMeta("volume_up", "音量+", "增加对端系统硬件音量"),
    ToolbarButtonMeta("volume_panel", "远端音量", "远程系统音量控制面板"),
    ToolbarButtonMeta("audio_stream", "音频流转", "对端实时音频流转到本机"),
    ToolbarButtonMeta("virtual_display", "虚拟副屏", "开启独立虚拟副屏分屏", isDisplayOnly = true),
    ToolbarButtonMeta("logs", "运行日志", "查看 Scrcpy 调试日志"),
    ToolbarButtonMeta("settings", "投屏设置", "分辨率、码率、按钮排序与排布"),
    ToolbarButtonMeta("fullscreen", "全屏显示", "进入/退出沉浸式全屏模式"),
    ToolbarButtonMeta("disconnect", "断开投屏", "停止画面镜像并关闭连接"),
    ToolbarButtonMeta("collapse", "折叠工具栏", "收起工具栏为悬浮小球")
)

