package com.yangyx.adbhelper.scrcpy

data class ScrcpyConfig(
    val bitrate: Int = 4000000,           // Default 4 Mbps
    val maxResolution: Int = 1080,        // Default 1080p
    val maxFps: Int = 60,                 // Default 60 FPS
    val isScreenOff: Boolean = false,     // Screen off control (息屏控制)
    val isAudioEnabled: Boolean = false,  // Audio forwarding toggle
    val virtualDisplayWidth: Int = 0,     // Virtual display width (0 = disabled)
    val virtualDisplayHeight: Int = 0,    // Virtual display height
    val launchPackageOnVirtualDisplay: String = "" // Package to launch on virtual display
)
