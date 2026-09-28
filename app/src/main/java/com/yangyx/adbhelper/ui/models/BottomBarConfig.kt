package com.yangyx.adbhelper.ui.models

/**
 * 底部导航栏模式枚举
 */
enum class BottomBarMode {
    CLASSIC_M3,   // 经典 Material 3 沉浸底栏 (NavigationBar)
    LIQUID_GLASS  // 拟态透明流体玻璃底栏 (参考 Kyant0 AndroidLiquidGlass)
}

/**
 * 玻璃半透明与磨砂度
 */
enum class GlassTranslucency(val label: String, val darkAlpha: Float, val lightAlpha: Float) {
    HIGH("微透薄霜 (High)", 0.65f, 0.78f),
    BALANCED("浅浅毛玻璃 (Balanced)", 0.78f, 0.88f),
    DEEP("深邃磨砂 (Deep)", 0.88f, 0.94f)
}

/**
 * 玻璃边缘物理折射高光强度
 */
enum class SpecularGlow(val label: String, val intensity: Float) {
    SUBTLE("柔和微光", 0.45f),
    VIBRANT("璀璨折射 (推荐)", 0.85f),
    MAXIMUM("炫彩高光", 1.0f)
}

/**
 * 流体游标果冻动画弹性
 */
enum class JellySpring(val label: String, val dampingRatio: Float, val stiffness: Float) {
    GENTLE("沉稳流畅", 0.85f, 400f),
    BOUNCY("灵动果冻 (推荐)", 0.66f, 320f),
    HYPER("动感回弹", 0.52f, 260f)
}

/**
 * 导航项数据模型
 */
data class NavItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * 拟态透明流体玻璃配置参数
 */
data class LiquidGlassConfig(
    val translucency: GlassTranslucency = GlassTranslucency.BALANCED,
    val specularGlow: SpecularGlow = SpecularGlow.VIBRANT,
    val jellySpring: JellySpring = JellySpring.BOUNCY
)
