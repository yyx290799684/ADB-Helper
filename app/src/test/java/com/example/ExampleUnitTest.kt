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
}
