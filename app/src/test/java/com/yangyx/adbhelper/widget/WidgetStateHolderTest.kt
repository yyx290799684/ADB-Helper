package com.yangyx.adbhelper.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetStateHolderTest {

    @Test
    fun testWidgetStateTransitions() {
        WidgetStateHolder.onDisconnected()
        assertEquals(WidgetStateHolder.State.DISCONNECTED, WidgetStateHolder.currentState)
        assertNull(WidgetStateHolder.activeSession)
        assertNull(WidgetStateHolder.activeConnection)

        WidgetStateHolder.setFallbackDeviceInfo("Xiaomi 13 Pro", "192.168.1.100:5555")
        assertEquals("Xiaomi 13 Pro", WidgetStateHolder.deviceName)
        assertEquals("192.168.1.100:5555", WidgetStateHolder.deviceIp)

        WidgetStateHolder.onConnecting("Pixel 7", "192.168.1.101:5555")
        assertEquals(WidgetStateHolder.State.CONNECTING, WidgetStateHolder.currentState)
        assertEquals("Pixel 7", WidgetStateHolder.deviceName)
        assertEquals("192.168.1.101:5555", WidgetStateHolder.deviceIp)

        WidgetStateHolder.onConnected(null, "Pixel 7 Pro", "192.168.1.102:5555")
        assertEquals(WidgetStateHolder.State.CONNECTED, WidgetStateHolder.currentState)
        assertEquals("Pixel 7 Pro", WidgetStateHolder.deviceName)
        assertEquals("192.168.1.102:5555", WidgetStateHolder.deviceIp)

        WidgetStateHolder.onDisconnected()
        assertEquals(WidgetStateHolder.State.DISCONNECTED, WidgetStateHolder.currentState)
    }
}
