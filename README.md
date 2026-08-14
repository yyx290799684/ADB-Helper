# Proxifier Core 🚀

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" height="100" alt="Proxifier Core Icon" />
</p>

<p align="center">
  <b>Android 平台轻量、高效的 SOCKS5 / HTTP 规则分流与透明代理路由器</b><br/>
  <i>支持 KernelSU Root 透明代理 与 免 Root VpnService 双引擎</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?style=flat&logo=android" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4.svg?style=flat&logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/Root-KernelSU%20%2F%20Magisk-critical.svg?style=flat" alt="KernelSU" />
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=flat" alt="License" />
</p>

---

## 📖 项目简介 (Introduction)

**Proxifier Core** 是一款专为 Android 平台打造的专业级网络透明代理与规则分流路由工具。类似 PC 端的 Proxifier，它能将系统及各应用程序的网络流量精细化重定向至指定的 SOCKS5 或 HTTP 代理服务器。

项目具备 **双引擎架构**：既可以在具备 Root 权限的设备上使用 **KernelSU / Magisk 透明代理引擎**（基于 iptables / ip6tables NAT 重定向，极低系统开销与极致延迟表现），也可以在未 Root 设备上无缝切换至标准 **Android VpnService 引擎**。

---

## ✨ 核心功能特性 (Key Features)

### 1. ⚡ 双工作模式引擎 (Dual Engine Architecture)
- **KernelSU / Root 透明代理模式**：
  - 基于 Linux 内核 `iptables` / `ip6tables` NAT 规则，将系统流量无感重定向至本地透明代理服务器。
  - 内置高性能 `SO_ORIGINAL_DST` 与 `/proc/net/nf_conntrack` 原目的地址及应用 UID 还原机制。
  - 支持 SELinux 策略动态规避 (`magiskpolicy` / `supolicy`) 与 Android 10+ 隐藏 API 限制绕过。
  - **开销极低**：无需维护复杂虚拟网卡，减少内核-用户态二次拷贝，延迟更低。
- **VPN Service 免 Root 模式**：
  - 采用标准 Android `VpnService` 虚拟网卡 (TUN) 拦截网络流量。
  - 内置 TCP 协议栈解析与连接中继引擎，支持全自动 IPv4 / IPv6 分流。
  - 内置 UDP 53 端口 DNS 转发处理，防范 DNS 污染与泄漏。

### 2. 🔀 强大的多维度链式分流规则 (Advanced Routing Rules)
- **多条件匹配系统**：
  - 📱 **应用分流 (App Packages)**：内置应用选择器，可指定特定 App 走代理、直连或拦截。
  - 🌐 **域名分流 (Domain Matching)**：支持通配符与泛域名（例如 `*.google.com`、`github.com`、`all`）。
  - 🎯 **IP / CIDR 网段分流 (IP Matching)**：支持精准 IP 及 CIDR 网段（例如 `192.168.1.0/24`、`10.0.0.0/8`）。
  - 🔌 **端口与端口段 (Port Range)**：支持单端口或端口范围过滤（例如 `80`、`443`、`8080-8090`）。
- **3 种流量动作策略**：
  - `PROXY`（代理）：重定向至指定代理服务器。
  - `DIRECT`（直连）：直接发起网络连接，绕过代理。
  - `BLOCK` / `REJECT`（拦截）：拒绝匹配流量建立连接。
- **多节点指定与故障旁路**：
  - 支持为不同的规则指定不同的代理节点（多节点并发分流）。
  - 支持 `ignoreIfProxyDown`（故障旁路模式）：当规则所指定的代理节点离线时，自动降级为直连，避免网络断连。
- **规则优先级管理**：支持一键上下调整规则匹配顺序及单独启用/禁用开关。

### 3. 🌐 节点管理与多协议支持 (Proxy Management)
- 支持 **SOCKS5**（支持用户名/密码认证）与 **HTTP/HTTPS CONNECT** 代理协议。
- 支持 IPv4、IPv6 及域名主机地址。
- **快速测速 (Latency Ping)**：支持单节点及全部节点一键并发延迟探测。
- 节点颜色标识分类与活动节点一键切换。

### 4. 📦 KernelSU / Magisk 动态模块生成 (One-Click Flashable Module)
- App 内置 **一劳永逸版 Magisk / KernelSU 模块生成器**。
- 生成规范的 `Proxifier_KernelSU_Module.zip` 刷机包，自动保存至系统 `Download/` 目录。
- **桥接式热更新设计**：刷入模块一次即可，后续在 App 内增删改查分流规则，即时同步生效并支持开机自动应用，无需反复重新刷包。

### 5. 🔍 实时连接抓包与诊断分析 (Real-time Diagnostics & Logs)
- 实时追踪所有进出站连接：应用名、包名、目标域名/IP、目标端口、匹配规则、所用节点、连接状态。
- **SNI / Host 解析**：自动从 TLS ClientHello 或 HTTP Header 中嗅探目标域名。
- **数据包分析**：支持查看首包 16 进制 Hex Dump 与 ASCII 明文预览。
- 错误异常追踪与重试诊断。

### 6. 📱 便捷系统级集成 (System Integration)
- **快捷设置磁贴 (Quick Settings Tile)**：下拉系统通知栏即可一键开启/关闭代理服务。
- **开机自启 (Boot Receiver)**：支持系统启动完成后自动挂载分流规则并拉起服务。
- **流量监控**：实时上下行速率计算与总吞吐量统计。

---

## 📱 界面预览 (UI Showcase)

| 仪表盘 (Dashboard) | 节点管理 (Proxies) | 分流规则 (Rules) | KernelSU (Root) | 日志抓包 (Logs) |
| :---: | :---: | :---: | :---: | :---: |
| 📊 实时速率与状态监控 | 🌐 节点列表与延迟测速 | 🔀 应用/域名/IP分流配置 | ⚡ 模块导出与规则应用 | 🔍 实时连接与Hex预览 |

---

## 🛠️ 使用指南 (Getting Started)

### 模式一：免 Root 使用 (VpnService Mode)
1. 打开应用，进入 **「仪表盘」** 页面。
2. 切换运行模式为 **「VPN Service (Non-Root)」**。
3. 进入 **「代理节点」** 页面，点击右下角 **「+」** 添加你的 SOCKS5 或 HTTP 代理服务器，并设为活动节点。
4. 进入 **「分流规则」** 页面，配置所需的应用分流、域名分流或 IP 规则（默认已包含常见规则模板）。
5. 回到仪表盘，点击中心大按钮启动代理。初次启动会弹出系统 VPN 权限申请，点击 **「确定」** 即可。

### 模式二：KernelSU / Magisk Root 透明代理 (Root Mode)
1. 确保设备已安装 **KernelSU**、**APatch** 或 **Magisk** 并授予本应用 Root 权限。
2. 在 **「仪表盘」** 切换运行模式为 **「KernelSU / Root Transparent」**。
3. 配置好代理节点与分流规则。
4. 进入 **「KernelSU」** 页面：
   - 点击 **「应用当前规则」**：App 将即时通过 Root Shell 写入 iptables 规则并启动透明中继服务。
   - 点击 **「导出动态刷机模块 (ZIP)」**：App 将在系统的 `Download/` 文件夹下生成 `Proxifier_KernelSU_Module.zip`。
   - 打开 KernelSU / Magisk 管理器，从本地安装该 ZIP 模块并重启设备。
   - 此后设备开机即自动生效，后续在 App 内调整规则无需重新刷入模块。

---

## 📋 分流规则配置示例 (Rule Configuration Examples)

| 规则名称 | 目标包名 (Packages) | 目标域名 (Domains) | 目标 IP (CIDR) | 动作 (Action) | 适用场景 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **局域网直连** | - | `*.local` | `10.0.0.0/8`, `192.168.0.0/16`, `127.0.0.0/8` | `DIRECT` | 避免局域网设备与内网服务走代理 |
| **特定应用代理** | `com.android.chrome`, `com.twitter.android` | - | - | `PROXY` | 仅将指定浏览器和 App 进行代理 |
| **开发服务分流** | - | `*.github.com`, `*.docker.com` | - | `PROXY (指定节点)` | 让开发相关域名走高速专用节点 |
| **广告拦截** | - | `*.admob.com`, `*.doubleclick.net` | - | `BLOCK` | 屏蔽常见广告追踪与分析域名 |

---

## 🏗️ 技术架构 (Architecture)

```
app/src/main/java/com/yangyx/proxy/
├── MainActivity.kt                # 单 Activity 结构，Compose 路由导航
├── data/
│   ├── db/                        # Room 数据库 (AppDatabase, DAO)
│   ├── model/                     # 数据实体 (ProxyServer, ProxyRule, ConnectionLog 等)
│   └── repository/                # 数据仓库层
├── engine/
│   ├── KernelSuManager.kt         # iptables 脚本构建与 Magisk 模块打包
│   ├── LocalTransparentProxyServer.kt # 本地透明代理服务、SO_ORIGINAL_DST 还原与 SOCKS5/HTTP 握手
│   ├── VpnProxyService.kt         # Android VpnService TUN 网卡管理与 TCP/UDP 协议栈
│   ├── SocketUidResolver.kt       # 端口与应用 UID / PackageName 映射反查
│   ├── Socks5Tester.kt            # 节点延迟与连通性检测器
│   └── RootShellManager.kt        # Root Shell 执行器
├── service/
│   ├── KernelSuBootService.kt     # Root 开机自启前台服务
│   └── ProxyTileService.kt        # Quick Settings 快捷设置下拉磁贴
├── receiver/
│   └── BootReceiver.kt            # 系统开机广播监听
└── ui/
    ├── screens/                   # Compose 页面 (Dashboard, Proxies, Rules, KernelSu, Logs)
    ├── theme/                     # Material 3 主题与配色
    └── viewmodel/                 # MVVM 架构 ViewModel
```

---

## 🔧 构建与编译 (Build & Compilation)

### 环境要求
- **Android Studio**: Ladybug / Meerkat (2024.2+) 或更高版本
- **JDK**: OpenJDK 11 或 17
- **Android SDK**: `compileSdk 36`, `minSdk 24` (Android 7.0+)
- **Gradle**: 8.x + Kotlin 2.x

### 本地编译步骤
```bash
# 1. 克隆本仓库
git clone https://github.com/<your-username>/proxifier-android.git
cd proxifier-android

# 2. 编译 Debug APK
./gradlew assembleDebug

# 3. 输出文件位于
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 权限说明 (Permissions)

- `android.permission.INTERNET`: 用于建立代理与网络中继连接。
- `android.permission.BIND_VPN_SERVICE`: 用于免 Root 模式下建立 VpnService。
- `android.permission.QUERY_ALL_PACKAGES`: 用于在规则配置时读取已安装应用列表与图标。
- `android.permission.RECEIVE_BOOT_COMPLETED`: 用于开机自启动服务与恢复透明代理规则。
- `android.permission.POST_NOTIFICATIONS`: 用于前台服务保活与通知展示。
- `Root 权限 (可选)`: 仅在启用 KernelSU / Root 透明代理模式时需要。

---

## ⚠️ 免责声明 (Disclaimer)

1. 本软件仅供网络技术学习、网络协议分析与合法的日常网络优化使用。
2. 请在遵守当地法律法规的前提下使用本软件。开发者不对任何由于不正当使用本软件而造成的后果承担责任。

---

## 📄 开源许可证 (License)

本项目采用 [Apache-2.0 License](LICENSE) 开源。欢迎提交 PR 和 Issue！
