package com.yangyx.adbhelper.ui.models

import kotlin.math.roundToInt

/**
 * 屏幕亮度与人眼感知百分比转换工具类
 *
 * 背景与原理：
 * Android 系统底层存储于 `settings get/put system screen_brightness` 的数值为 1 ~ 255（线性发光强度/背光硬件 PWM），
 * 但人眼对暗部光线强度的变化远比高亮区敏感（遵循 Fechner's Law / 韦伯-费希纳定律）。
 * 因此 Android 系统的下拉通知栏状态栏滑块与设置中心，从 Android 9+ 起均采用非线性感知曲线（ITU-R BT.2100 HLG / Perceptual Gamma）：
 * - 当系统滑块居中处于 50% 时，底层实际亮度数值通常为 35；
 * - 当系统滑块处于 1/3 (约 33%) 时，底层实际亮度数值通常为 15；
 * - 当系统滑块拉满处于 100% 时，底层实际亮度数值为 255；
 * - 当系统滑块最低处于 0% 时，底层实际亮度数值为 1 (最低背光，防止黑屏)。
 *
 * 若直接使用线性的 (brightness / 255f) 计算百分比，会导致 50% 亮度显示为 13%(35)，1/3 亮度显示为 5%(15)，
 * 且滑动控制端滑块时对端亮度变化幅度与位置严重失准。
 * 本工具类提供基于拟合的感知校准曲线，实现双向精准对应。
 */
object BrightnessConverter {

    // 经由实测数据点与人眼感知曲线拟合的三次平滑单调多项式系数
    // 对应标准点：P=0% -> B=1, P=33.3%(1/3) -> B=15, P=50% -> B=35, P=100% -> B=255
    private const val COEFF_A = 1.2756
    private const val COEFF_B = -0.4488
    private const val COEFF_C = 0.1732

    /**
     * 将系统底层亮度值 (1 ~ 255) 转换为符合人眼感知的系统 UI 滑块百分比 (0% ~ 100%)。
     *
     * @param rawBrightness 底层设置数值 (1 ~ 255)
     * @param maxBrightness 系统最大亮度，通常为 255
     * @return 对应的感知百分比 (0 ~ 100)
     */
    fun rawBrightnessToPercent(rawBrightness: Int, maxBrightness: Int = 255): Int {
        if (rawBrightness <= 1) return 0
        if (rawBrightness >= maxBrightness) return 100

        val targetY = (rawBrightness - 1).toDouble() / (maxBrightness - 1)
        var low = 0.0
        var high = 1.0

        // 利用二分法精确反解三次单调递增函数 x in [0, 1]
        for (i in 0 until 18) {
            val mid = (low + high) / 2.0
            val y = COEFF_A * mid * mid * mid + COEFF_B * mid * mid + COEFF_C * mid
            if (y < targetY) {
                low = mid
            } else {
                high = mid
            }
        }

        val percent = ((low + high) / 2.0 * 100.0).roundToInt()
        return percent.coerceIn(0, 100)
    }

    /**
     * 将感知百分比 (0% ~ 100%) 转换为发送给系统底层的亮度数值 (1 ~ 255)。
     *
     * @param percent 感知百分比 (0.0 ~ 100.0)
     * @param maxBrightness 系统最大亮度，通常为 255
     * @return 对应的系统底层亮度设置数值 (1 ~ 255)
     */
    fun percentToRawBrightness(percent: Float, maxBrightness: Int = 255): Int {
        if (percent <= 0f) return 1
        if (percent >= 100f) return maxBrightness

        val x = (percent / 100f).coerceIn(0f, 1f).toDouble()
        val y = (COEFF_A * x * x * x + COEFF_B * x * x + COEFF_C * x).coerceIn(0.0, 1.0)
        val raw = (1 + y * (maxBrightness - 1)).roundToInt()
        return raw.coerceIn(1, maxBrightness)
    }
}
