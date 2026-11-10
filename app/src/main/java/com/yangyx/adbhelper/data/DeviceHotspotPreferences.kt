package com.yangyx.adbhelper.data

import android.content.Context
import org.json.JSONObject

/**
 * 被控端设备专属的便携式热点参数配置
 */
data class DeviceHotspotConfig(
    val ssid: String = "AndroidAP",
    val securityType: String = "wpa2", // open, wpa2, wpa3, wpa3_transition, owe, owe_transition
    val passphrase: String = "12345678",
    val band: String = "any", // 2, 5, 6, any, bridged, bridged_2_5, bridged_2_6, bridged_5_6
    val bandwidth: String = "auto" // auto, 20, 40, 80, 160, 320
) {
    val displayBand: String
        get() = when (band) {
            "2" -> "2.4 GHz"
            "5" -> "5 GHz"
            "6" -> "6 GHz"
            "any" -> "自动 / Any"
            "bridged" -> "双频并发 (2.4G+5G)"
            "bridged_2_5" -> "双频并发 (2.4G+5G)"
            "bridged_2_6" -> "双频并发 (2.4G+6G)"
            "bridged_5_6" -> "双频并发 (5G+6G)"
            else -> band
        }

    val displayBandwidth: String
        get() = if (bandwidth == "auto" || bandwidth == "-1" || bandwidth.isBlank()) "自动频宽" else "${bandwidth}MHz"

    val displaySecurity: String
        get() = when (securityType.lowercase()) {
            "open" -> "无密码 (开放)"
            "wpa2" -> "WPA2 PSK"
            "wpa3" -> "WPA3 SAE"
            "wpa3_transition" -> "WPA2/WPA3 混合"
            "owe" -> "OWE"
            "owe_transition" -> "OWE 混合"
            else -> securityType.uppercase()
        }

    val isPasswordRequired: Boolean
        get() = !securityType.equals("open", ignoreCase = true) && !securityType.startsWith("owe", ignoreCase = true)

    /**
     * 生成供 cmd wifi start-softap 执行的完整命令行 (包含 su -c)
     */
    fun buildStartSoftApCommand(): String {
        val bandArg = if (band.isNotBlank() && band != "default") " -b $band" else ""
        val widthArg = if (bandwidth.isNotBlank() && bandwidth != "auto" && bandwidth != "-1") " -w $bandwidth" else ""
        val safeSsid = ssid.replace("\"", "\\\"").replace("'", "\\'")
        val safePass = passphrase.replace("\"", "\\\"").replace("'", "\\'")

        return if (!isPasswordRequired) {
            "su -c \"cmd wifi start-softap '$safeSsid' $securityType$bandArg$widthArg\""
        } else {
            "su -c \"cmd wifi start-softap '$safeSsid' $securityType '$safePass'$bandArg$widthArg\""
        }
    }
}

/**
 * 热点参数在本地持久化管理（按设备隔离存储，不修改对端系统文件）
 */
object DeviceHotspotPreferences {
    private const val PREFS_NAME = "device_hotspot_preferences"
    private const val KEY_PREFIX = "hotspot_cfg_"

    fun getConfig(context: Context, deviceId: String): DeviceHotspotConfig? {
        if (deviceId.isBlank()) return null
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(KEY_PREFIX + deviceId, null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            DeviceHotspotConfig(
                ssid = json.optString("ssid", "AndroidAP"),
                securityType = json.optString("securityType", "wpa2"),
                passphrase = json.optString("passphrase", "12345678"),
                band = json.optString("band", "any"),
                bandwidth = json.optString("bandwidth", "auto")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveConfig(context: Context, deviceId: String, config: DeviceHotspotConfig) {
        if (deviceId.isBlank()) return
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val json = JSONObject().apply {
                put("ssid", config.ssid)
                put("securityType", config.securityType)
                put("passphrase", config.passphrase)
                put("band", config.band)
                put("bandwidth", config.bandwidth)
            }
            sp.edit().putString(KEY_PREFIX + deviceId, json.toString()).apply()
        } catch (_: Exception) {}
    }

    /**
     * 解析对端系统的 WifiConfigStoreSoftAp.xml 配置文件
     */
    fun parseSystemSoftApXml(xmlContent: String): DeviceHotspotConfig {
        if (xmlContent.isBlank()) return DeviceHotspotConfig()

        // 1. SSID
        val rawSsid = Regex("""<string name="WifiSsid">([^<]*)</string>""").find(xmlContent)?.groupValues?.get(1) ?: ""
        val unescapedSsid = rawSsid
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .removeSurrounding("\"")
            .trim()
        val ssid = if (unescapedSsid.isNotBlank()) unescapedSsid else "AndroidAP"

        // 2. Passphrase
        val rawPass = Regex("""<string name="Passphrase">([^<]*)</string>""").find(xmlContent)?.groupValues?.get(1) ?: ""
        val passphrase = rawPass
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()

        // 3. SecurityType
        val secInt = Regex("""<int name="SecurityType" value="(\d+)"""").find(xmlContent)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val securityType = when (secInt) {
            0 -> "open"
            1 -> "wpa2"
            2 -> "wpa3_transition"
            3 -> "wpa3"
            4 -> "owe_transition"
            5 -> "owe"
            else -> "wpa2"
        }

        // 4. Band
        val bandInt = Regex("""<int name="Band" value="(\d+)"""").find(xmlContent)?.groupValues?.get(1)?.toIntOrNull() ?: 3
        val band = when (bandInt) {
            1 -> "2"
            2 -> "5"
            3 -> "any"
            4 -> "6"
            else -> "any"
        }

        // 5. MaxChannelWidth
        val widthInt = Regex("""<int name="MaxChannelWidth" value="(-?\d+)"""").find(xmlContent)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val bandwidth = when (widthInt) {
            20 -> "20"
            40 -> "40"
            80 -> "80"
            160 -> "160"
            320 -> "320"
            else -> "auto"
        }

        return DeviceHotspotConfig(
            ssid = ssid,
            securityType = securityType,
            passphrase = passphrase.ifBlank { if (securityType == "open") "" else "12345678" },
            band = band,
            bandwidth = bandwidth
        )
    }
}
