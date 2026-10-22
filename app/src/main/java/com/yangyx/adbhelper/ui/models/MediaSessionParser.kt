package com.yangyx.adbhelper.ui.models

object MediaSessionParser {

    private val knownApps = mapOf(
        "com.tencent.mm" to "微信",
        "com.ss.android.ugc.aweme" to "抖音",
        "com.ss.android.ugc.aweme.lite" to "抖音极速版",
        "com.ss.android.ugc.livelite" to "抖音火山版",
        "com.tencent.mobileqq" to "QQ",
        "com.tencent.tim" to "TIM",
        "com.eg.android.AlipayGphone" to "支付宝",
        "com.taobao.taobao" to "淘宝",
        "com.taobao.idlefish" to "闲鱼",
        "com.jingdong.app.mall" to "京东",
        "com.jingdong.app.mall.lite" to "京东极速版",
        "com.xunmeng.pinduoduo" to "拼多多",
        "com.sina.weibo" to "微博",
        "com.sankuai.meituan" to "美团",
        "com.sankuai.meituan.takeout" to "美团外卖",
        "me.ele" to "饿了么",
        "com.bilibili.app.in" to "哔哩哔哩",
        "tv.danmaku.bili" to "哔哩哔哩",
        "com.baidu.netdisk" to "百度网盘",
        "com.autonavi.minimap" to "高德地图",
        "com.baidu.BaiduMap" to "百度地图",
        "com.zhihu.android" to "知乎",
        "com.coolapk.market" to "酷安",
        "com.xingin.xhs" to "小红书",
        "com.kuaishou.nebula" to "快手极速版",
        "com.smile.gifmaker" to "快手",
        "com.netease.cloudmusic" to "网易云音乐",
        "com.tencent.qqmusic" to "QQ音乐",
        "com.kugou.android" to "酷狗音乐",
        "cn.kuwo.player" to "酷我音乐",
        "com.qiyi.video" to "爱奇艺",
        "com.tencent.qqlive" to "腾讯视频",
        "com.youku.phone" to "优酷视频",
        "com.ss.android.article.news" to "今日头条",
        "com.baidu.tieba" to "百度贴吧",
        "com.baidu.searchbox" to "百度",
        "com.quark.browser" to "夸克",
        "com.UCMobile" to "UC浏览器",
        "com.alibaba.android.rimet" to "钉钉",
        "com.ss.android.lark" to "飞书",
        "com.tencent.wework" to "企业微信",
        "com.kingsoft.moffice_eng" to "WPS Office",
        "com.ximalaya.ting.android" to "喜马拉雅",
        "com.dragon.read" to "番茄免费小说",
        "com.tencent.tmgp.sgame" to "王者荣耀",
        "com.tencent.tmgp.pubgmhd" to "和平精英",
        "com.miHoYo.Yuanshen" to "原神",
        "com.miHoYo.hkrpg" to "崩坏：星穹铁道",
        "com.chaozh.iReader" to "掌阅",
        "com.zhangyue.read" to "掌阅",
        "com.duokan.reader" to "多看阅读",
        "com.sinovatech.unicom.ui" to "中国联通",
        "com.greenpoint.android.mc10086" to "中国移动",
        "com.ct.client" to "中国电信",
        "com.icbc" to "中国工商银行",
        "com.ccb.longpay" to "中国建设银行",
        "com.chinamworld.bocmbci" to "中国银行",
        "com.pingan.carowner" to "平安好车主",
        "com.cmbchina.ccd.kanjia" to "掌上生活",
        "com.cmbchina.psbc" to "邮储银行",
        "com.huawei.appmarket" to "华为应用市场",
        "com.xiaomi.market" to "小米应用商店",
        "com.oppo.market" to "OPPO 应用商店",
        "com.bbk.appstore" to "vivo 应用商店",
        "com.heytap.market" to "HeyTap 应用中心",
        "com.android.settings" to "系统设置",
        "com.android.camera" to "相机",
        "com.android.camera2" to "相机",
        "com.android.gallery3d" to "相册",
        "com.sec.android.gallery3d" to "图库",
        "com.android.chrome" to "Chrome 浏览器",
        "com.google.android.youtube" to "YouTube",
        "com.google.android.gms" to "Google Play 服务",
        "com.google.android.vending" to "Google Play 商店",
        "com.android.phone" to "电话服务",
        "com.android.mms" to "信息",
        "com.android.contacts" to "通讯录",
        "com.android.calculator2" to "计算器",
        "com.android.deskclock" to "时钟",
        "com.android.filemanager" to "文件管理",
        "com.android.soundrecorder" to "录音机",
        "com.android.vending" to "Google Play",
        "org.mozilla.firefox" to "Firefox 浏览器",
        "com.microsoft.emmx" to "Edge 浏览器",
        "com.spotify.music" to "Spotify",
        "com.netflix.mediaclient" to "Netflix",
        "org.telegram.messenger" to "Telegram",
        "com.whatsapp" to "WhatsApp",
        "com.instagram.android" to "Instagram",
        "com.twitter.android" to "X (Twitter)",
        "com.facebook.katana" to "Facebook"
    )

    fun parseFriendlyAppName(pkg: String): String {
        if (knownApps.containsKey(pkg)) {
            return knownApps[pkg]!!
        }

        val parts = pkg.split(".")
        val filteredParts = parts.filterNot {
            it in listOf("com", "cn", "net", "org", "android", "app", "mobile", "client", "ui", "service")
        }

        return if (filteredParts.isNotEmpty()) {
            filteredParts.last().replaceFirstChar { it.uppercase() }
        } else {
            pkg
        }
    }

    fun parseMediaSessionOutput(output: String): MediaPlaybackInfo {
        if (output.isBlank() || output.contains("No media sessions", ignoreCase = true)) {
            return MediaPlaybackInfo()
        }

        data class ParsedSession(
            var pkg: String = "",
            var stateCode: Int = 0,
            var positionMs: Long = 0L,
            var durationMs: Long = 0L,
            var title: String = "",
            var artist: String = "",
            var album: String = "",
            var isActive: Boolean = false
        )

        val sessions = mutableListOf<ParsedSession>()
        var current: ParsedSession? = null

        // 提取 Audio playback (lastly played comes first) 中的首个活跃应用包名
        val lastlyPlayedPkg = Regex("""packages=([a-zA-Z0-9._]+)""").find(output)?.groupValues?.get(1)?.trim()

        output.lines().forEach { rawLine ->
            val line = rawLine.replace("\r", "").trim()

            // 识别新 Session 的开始：ownerPid 或 package= 或独立 Session 头部
            if (line.startsWith("ownerPid=")) {
                current = ParsedSession()
                sessions.add(current!!)
            } else if (line.startsWith("package=")) {
                val pkgName = line.removePrefix("package=").trim().substringBefore(" ")
                if (current == null) {
                    current = ParsedSession(pkg = pkgName)
                    sessions.add(current!!)
                } else if (current?.pkg.isNullOrEmpty()) {
                    current?.pkg = pkgName
                } else if (current?.pkg != pkgName) {
                    current = ParsedSession(pkg = pkgName)
                    sessions.add(current!!)
                }
            } else if (line.contains("/QQMusicMediaSession") || line.contains("/MediaSession") || line.contains("Media button session is")) {
                val extractedPkg = Regex("""\b([a-zA-Z0-9._]+)/""").find(line)?.groupValues?.get(1)?.trim() ?: ""
                if (extractedPkg.isNotEmpty() && !extractedPkg.startsWith("com.android.server.telecom")) {
                    if (current == null) {
                        current = ParsedSession(pkg = extractedPkg)
                        sessions.add(current!!)
                    } else if (current?.pkg.isNullOrEmpty()) {
                        current?.pkg = extractedPkg
                    }
                }
            }

            current?.let { s ->
                if (line.startsWith("active=")) {
                    s.isActive = line.removePrefix("active=").trim().toBoolean()
                }

                if (line.contains("state=PlaybackState") || line.contains("state=")) {
                    when {
                        line.contains("PLAYING", ignoreCase = true) -> s.stateCode = 3
                        line.contains("PAUSED", ignoreCase = true) -> s.stateCode = 2
                        line.contains("BUFFERING", ignoreCase = true) -> s.stateCode = 6
                        line.contains("STOPPED", ignoreCase = true) -> s.stateCode = 1
                        else -> {
                            val numMatch = Regex("""state=(?:[A-Za-z_]+\()?(\d+)""").find(line)
                            numMatch?.groupValues?.get(1)?.toIntOrNull()?.let { s.stateCode = it }
                        }
                    }
                    val posMatch = Regex("""position=(\d+)""").find(line)
                    posMatch?.groupValues?.get(1)?.toLongOrNull()?.let { s.positionMs = it }
                }

                if (line.contains("description=")) {
                    val desc = line.substringAfter("description=").trim()
                    if (desc.isNotBlank() && desc != "null") {
                        if (desc.contains("title=")) {
                            Regex("""title=([^,]+)""").find(desc)?.groupValues?.get(1)?.trim()?.let { if (it != "null") s.title = it }
                            Regex("""subtitle=([^,]+)""").find(desc)?.groupValues?.get(1)?.trim()?.let { if (it != "null") s.artist = it }
                            Regex("""description=([^,]+)""").find(desc)?.groupValues?.get(1)?.trim()?.let { if (it != "null") s.album = it }
                        } else {
                            val parts = desc.split(",").map { it.trim() }
                            if (parts.isNotEmpty() && parts[0].isNotEmpty() && parts[0] != "null") {
                                s.title = parts[0]
                            }
                            if (parts.size > 1 && parts[1].isNotEmpty() && parts[1] != "null") {
                                s.artist = parts[1]
                            }
                            if (parts.size > 2 && parts[2].isNotEmpty() && parts[2] != "null") {
                                s.album = parts[2]
                            }
                        }
                    }
                }

                if (line.contains("android.media.metadata.TITLE=")) {
                    val t = line.substringAfter("android.media.metadata.TITLE=").trim()
                    if (t.isNotBlank() && t != "null") s.title = t
                } else if (line.contains("DISPLAY_TITLE=")) {
                    val t = line.substringAfter("DISPLAY_TITLE=").trim()
                    if (s.title.isEmpty() && t.isNotBlank() && t != "null") s.title = t
                }

                if (line.contains("android.media.metadata.ARTIST=")) {
                    val a = line.substringAfter("android.media.metadata.ARTIST=").trim()
                    if (a.isNotBlank() && a != "null") s.artist = a
                } else if (line.contains("DISPLAY_SUBTITLE=")) {
                    val a = line.substringAfter("DISPLAY_SUBTITLE=").trim()
                    if (s.artist.isEmpty() && a.isNotBlank() && a != "null") s.artist = a
                }

                if (line.contains("android.media.metadata.ALBUM=")) {
                    val al = line.substringAfter("android.media.metadata.ALBUM=").trim()
                    if (al.isNotBlank() && al != "null") s.album = al
                }

                if (line.contains("duration=") || line.contains("DURATION=")) {
                    val durMatch = Regex("""(?:duration|DURATION)=(\d+)""").find(line)
                    durMatch?.groupValues?.get(1)?.toLongOrNull()?.let { s.durationMs = it }
                }
            }
        }

        // 过滤系统非媒体 Session (如电信/通话 HeadsetMediaButton、systemui等)
        val validSessions = sessions.filterNot {
            it.pkg.startsWith("com.android.server.telecom") || it.pkg == "android" || it.pkg == "com.android.systemui" || it.pkg == "com.android.bluetooth"
        }

        val best = validSessions.firstOrNull { it.pkg == lastlyPlayedPkg && it.stateCode == 3 }
            ?: validSessions.firstOrNull { it.stateCode == 3 && it.title.isNotEmpty() }
            ?: validSessions.firstOrNull { it.stateCode == 3 }
            ?: validSessions.firstOrNull { it.pkg == lastlyPlayedPkg && it.title.isNotEmpty() }
            ?: validSessions.firstOrNull { it.stateCode == 2 && it.title.isNotEmpty() }
            ?: validSessions.firstOrNull { it.stateCode == 2 }
            ?: validSessions.firstOrNull { it.title.isNotEmpty() }
            ?: validSessions.firstOrNull { it.isActive && it.pkg.isNotEmpty() }
            ?: validSessions.firstOrNull { it.pkg.isNotEmpty() }
            ?: validSessions.firstOrNull()

        if (best == null || (best.pkg.isEmpty() && best.title.isEmpty())) {
            // 尝试从 Audio playback (lastly played comes first) 中解析正在输出音频的应用包名
            if (!lastlyPlayedPkg.isNullOrBlank() && !lastlyPlayedPkg.startsWith("com.android.server.telecom") && lastlyPlayedPkg != "android") {
                val friendlyName = parseFriendlyAppName(lastlyPlayedPkg)
                return MediaPlaybackInfo(
                    hasActiveSession = true,
                    isPlaying = true,
                    title = "正在播放音频",
                    packageName = lastlyPlayedPkg,
                    appName = friendlyName,
                    stateCode = 3,
                    stateText = "正在播放"
                )
            }
            return MediaPlaybackInfo(
                hasActiveSession = false,
                isPlaying = false,
                stateText = "未在播放"
            )
        }

        val isPlaying = best.stateCode == 3 || best.stateCode == 6
        val stateText = when (best.stateCode) {
            3 -> "正在播放"
            2 -> "已暂停"
            6 -> "缓冲中"
            1 -> "已停止"
            else -> if (isPlaying) "正在播放" else if (best.isActive) "就绪" else "已暂停"
        }

        val friendlyName = parseFriendlyAppName(best.pkg)

        return MediaPlaybackInfo(
            hasActiveSession = true,
            isPlaying = isPlaying,
            title = best.title.ifEmpty { "未知曲目/媒体" },
            artist = best.artist,
            album = best.album,
            packageName = best.pkg,
            appName = friendlyName,
            durationMs = best.durationMs,
            positionMs = best.positionMs,
            stateCode = best.stateCode,
            stateText = stateText
        )
    }
}
