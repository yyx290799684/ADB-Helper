package com.yangyx.adbhelper.ui.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrightnessConverterTest {

    @Test
    fun testKeyBrightnessCheckpoints() {
        // 0% -> 底层亮度 1
        assertEquals(0, BrightnessConverter.rawBrightnessToPercent(1))
        assertEquals(1, BrightnessConverter.percentToRawBrightness(0f))

        // 1/3 (33%) -> 底层亮度 15
        assertEquals(33, BrightnessConverter.rawBrightnessToPercent(15))
        assertEquals(15, BrightnessConverter.percentToRawBrightness(33f))

        // 50% -> 底层亮度 35
        assertEquals(50, BrightnessConverter.rawBrightnessToPercent(35))
        assertEquals(35, BrightnessConverter.percentToRawBrightness(50f))

        // 100% -> 底层亮度 255
        assertEquals(100, BrightnessConverter.rawBrightnessToPercent(255))
        assertEquals(255, BrightnessConverter.percentToRawBrightness(100f))
    }

    @Test
    fun testMonotonicity() {
        // 确保 1..255 亮度单调非递减
        var prevPercent = -1
        for (b in 1..255) {
            val p = BrightnessConverter.rawBrightnessToPercent(b)
            assertTrue("Percent should be >= prev at b=$b", p >= prevPercent)
            prevPercent = p
        }

        // 确保 0..100 百分比转换回系统亮度单调非递减
        var prevRaw = 0
        for (p in 0..100) {
            val raw = BrightnessConverter.percentToRawBrightness(p.toFloat())
            assertTrue("Raw should be >= prev at p=$p", raw >= prevRaw)
            prevRaw = raw
        }
    }
}
