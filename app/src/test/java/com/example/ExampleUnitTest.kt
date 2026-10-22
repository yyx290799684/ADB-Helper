package com.example

import com.yangyx.adbhelper.ui.models.RemoteProcessItem
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun processItem_isRootUserIdentification() {
    val rootProc = RemoteProcessItem(
      pid = 1,
      user = "root",
      cpuUsage = "0.5%",
      memUsage = "12MB",
      name = "init"
    )
    assertTrue("User 'root' should be recognized as root", rootProc.isRootUser)

    val rootUidProc = RemoteProcessItem(
      pid = 2,
      user = "0",
      cpuUsage = "0.1%",
      memUsage = "5MB",
      name = "kthreadd"
    )
    assertTrue("User '0' should be recognized as root", rootUidProc.isRootUser)

    val nonRootProc = RemoteProcessItem(
      pid = 1234,
      user = "u0_a123",
      cpuUsage = "1.2%",
      memUsage = "45MB",
      name = "com.demo.app"
    )
    assertFalse("User 'u0_a123' should not be recognized as root", nonRootProc.isRootUser)

    val systemProc = RemoteProcessItem(
      pid = 800,
      user = "system",
      cpuUsage = "2.0%",
      memUsage = "120MB",
      name = "system_server"
    )
    assertFalse("User 'system' should not be recognized as root", systemProc.isRootUser)
  }

  @Test
  fun processItem_fullCommandLineResolution() {
    val shProc = RemoteProcessItem(
      pid = 12526,
      user = "root",
      cpuUsage = "0.0%",
      memUsage = "10MB",
      name = "sh",
      cmdline = "sh /data/adb/service.d/frpc.sh"
    )
    assertEquals("sh /data/adb/service.d/frpc.sh", shProc.fullCommandLine)

    val frpcProc = RemoteProcessItem(
      pid = 12530,
      user = "root",
      cpuUsage = "0.0%",
      memUsage = "15MB",
      name = "frpc",
      cmdline = "frpc -c /data/adb/service.d/frp/frpc.toml"
    )
    assertEquals("frpc -c /data/adb/service.d/frp/frpc.toml", frpcProc.fullCommandLine)
  }

  @Test
  fun testMediaSessionParserWithQQMusicDump() {
    val dump = """
MEDIA SESSION SERVICE (dumpsys media_session)

14 sessions listeners.
Global priority session is com.android.server.telecom/HeadsetMediaButton/1 (userId=0)
  HeadsetMediaButton com.android.server.telecom/HeadsetMediaButton/1 (userId=0)
    ownerPid=2534, ownerUid=1000, userId=0
    package=com.android.server.telecom
    launchIntent=null
    mediaButtonReceiver=null
    active=false
    flags=65537
    rating type=0
    controllers: 0
    state=null
    audioAttrs=AudioAttributes: usage=USAGE_VOICE_COMMUNICATION content=CONTENT_TYPE_SPEECH flags=0x800 tags= bundle=null
    volumeType=LOCAL, controlType=ABSOLUTE, max=0, current=0, volumeControlId=null
    metadata: null
    queueTitle=null, size=0
User Records:
Record for full_user=0
  Volume key long-press listener: null
  Volume key long-press listener package: 
  Media key listener: null
  Media key listener package: 
  OnMediaKeyEventDispatchedListener: added 0 listener(s)
  OnMediaKeyEventSessionChangedListener: added 1 listener(s)
    from com.android.bluetooth
  Last MediaButtonReceiver: MBR {pi=PendingIntent{8cc360b: PendingIntentRecord{eec8ce8 com.tencent.qqmusic broadcastIntent}}, componentName=ComponentInfo{com.tencent.qqmusic/androidx.media.session.MediaButtonReceiver}, type=1, pkg=com.tencent.qqmusic}
  Media button session is com.tencent.qqmusic/QQMusicMediaSession/310 (userId=0)
  Sessions Stack - have 1 sessions:
    QQMusicMediaSession com.tencent.qqmusic/QQMusicMediaSession/310 (userId=0)
      ownerPid=26077, ownerUid=10402, userId=0
      package=com.tencent.qqmusic
      launchIntent=null
      mediaButtonReceiver=MBR {pi=PendingIntent{8cc360b: PendingIntentRecord{eec8ce8 com.tencent.qqmusic broadcastIntent}}, componentName=ComponentInfo{com.tencent.qqmusic/androidx.media.session.MediaButtonReceiver}, type=1, pkg=com.tencent.qqmusic}
      active=true
      flags=3
      rating type=0
      controllers: 16
      state=PlaybackState {state=PLAYING(3), position=0, buffered position=0, speed=1.0, updated=495053181, actions=1911, custom actions=[Action:mName='收藏, mIcon=2131240663, mExtras=Bundle[mParcelledData.dataSize=40], Action:mName='显示桌面歌词, mIcon=2131240659, mExtras=Bundle[mParcelledData.dataSize=44]], active item id=-1, error=null}
      audioAttrs=AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_UNKNOWN flags=0x800 tags= bundle=null
      volumeType=LOCAL, controlType=ABSOLUTE, max=0, current=0, volumeControlId=null
      metadata: size=18, description=杯酒人生, 张妙格, 杯酒人生
      queueTitle=null, size=0
Audio playback (lastly played comes first)
  uid=10402 packages=com.tencent.qqmusic 
  uid=10316 packages=com.taobao.taobao 
  uid=10140 packages=com.miui.home 
Media session config:
  media_button_receiver_fgs_allowlist_duration_ms: [cur: 10000, def: 10000]
  media_session_calback_fgs_allowlist_duration_ms: [cur: 10000, def: 10000]
  media_session_callback_fgs_while_in_use_temp_allow_duration_ms: [cur: 10000, def: 10000]
  media_session_temp_user_engaged_duration_ms: [cur: 600000, def: 600000]
    """.trimIndent()

    val info = com.yangyx.adbhelper.ui.models.MediaSessionParser.parseMediaSessionOutput(dump)
    assertTrue("Should detect active session", info.hasActiveSession)
    assertTrue("Should be playing", info.isPlaying)
    assertEquals("杯酒人生", info.title)
    assertEquals("张妙格", info.artist)
    assertEquals("QQ音乐", info.appName)
    assertEquals("正在播放", info.stateText)
  }
}
