# ADB Helper (ADB与Scrcpy远程控制助手)

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" height="100" alt="ADB Helper Logo" style="border-radius: 20%;" />
</p>

<p align="center">
  <b>基于 Jetpack Compose 与纯 Kotlin 实现的强大 Android 端 ADB 客户端 & Scrcpy 屏幕镜像控制工具</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-purple.svg" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Architecture-MVVM%20%2B%20Coroutines-orange.svg" alt="Architecture" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg" alt="License" />
</p>

---

## 📖 项目简介 (Overview)

**ADB Helper** 是一款无需借助电脑、直接在 Android 设备上运行的全功能 ADB 调试与远程控制助手。本应用内部完整实现了 **ADB 传输协议栈** 与 **Scrcpy 视频流解码控制协议**，让您可以随时随地通过手机或平板对另一台 Android 设备进行无线连接、屏幕镜像控制、深度应用管理、文件传输、命令调试和硬件状态监控。

---

## ✨ 核心功能 (Features)

### 1. 🔗 多模式设备连接 (Multi-mode Connection)
- **Wi-Fi 无线调试 (ADB over TCP/IP)**：支持直连指定 IP 与 5555 端口。
- **Android 11+ 无线配对 (Wireless Pairing)**：支持输入 6 位配对码与随机端口完成安全配对并握手。
- **USB OTG 有线直连**：通过 Type-C OTG 数据线连接被控设备，免网络延迟。
- **本地历史设备**：自动记忆最近连接的设备信息，支持一键重连与设备管理。
- **内置安全认证**：本地生成 RSA 2048 位密钥对，自动完成 ADB 认证握手与凭据管理。

### 2. 🖥️ Scrcpy 低延迟屏幕镜像与控制 (Screen Mirroring & Control)
- **硬解流传输**：被控端运行轻量化 `scrcpy-server`，主控端通过 Android `MediaCodec` + `SurfaceView` 实现超低延迟 H.264 视频硬解。
- **全手势与触控交互**：支持单点轻触、拖动、滑动、长按，精准映射主控端手势到被控端屏幕。
- **快捷按键控制**：内置返回键 (Back)、主屏幕 (Home)、多任务 (Recents)、电源键、音量增减等快捷指令。
- **文字输入同步**：支持通过 ADB 直接向被控设备注入文本内容。
- **高级投屏选项**：
  - 支持画质码率、分辨率与最大帧率自定义。
  - 支持投屏时关闭被控端屏幕（熄屏控制/保持亮屏）。
  - 支持虚拟副屏模式（创建扩展显示屏）。

### 3. 📦 深度应用与进程管理 (App & Process Management)
- **极速应用名称解析**：内置 `aapt2` 工具自动提取 APK `badging`，支持简体中文 (`zh-CN` / `zh-Hans`) 优先匹配，告别冷冰冰的包名。
- **第三方应用 / 系统应用分类**：支持仅检索第三方应用加速加载，支持即时搜索与过滤。
- **应用操作面板**：
  - 🚀 一键启动应用 (`monkey` 启动入口)
  - 🛑 强行停止应用 (`am force-stop`)
  - 🗑️ 卸载应用 / 清除数据 (`pm clear` / `pm uninstall`)
  - 📥 本地 APK 远程安装（支持流式推送并静默安装）
- **可视化进度指示**：应用列表解析过程提供进度条实时反馈。
- **进程监控与管理**：实时列出被控端运行中的进程列表 (`ps -ef`)，支持按 PID 强行终止进程。

### 4. 📁 ADB 高速文件管理器 (File Explorer)
- **内置 AdbSync 协议**：基于原生 ADB SYNC 协议实现高吞吐量文件传输。
- **全功能文件操作**：支持目录树浏览、文件/文件夹新建、重命名、删除及详情查看。
- **文件互传**：支持将本地文件/图片/安装包推送到远程设备，或将远程文件拉取到本地存储。

### 5. 💻 交互式 ADB 终端 (Interactive Terminal)
- **原生 Shell 终端**：提供无缝的交互式 ADB Shell 执行环境。
- **预设快捷指令集**：内置一键截屏、屏幕录制、重启设备、进入 Recovery/Fastboot、查看系统日志 (`logcat`)、电池信息查询等常用运维脚本。
- **命令历史记录**：自动记录已执行指令，便于快速翻查与重复执行。

### 6. 📊 硬件与系统仪表盘 (Device Dashboard)
- **设备基础信息**：设备型号、品牌、制造商、Android 系统版本、SDK API 级别、CPU ABI 架构、屏幕分辨率。
- **资源监控**：实时监控被控设备电池电量、充电状态、电池温度、RAM 运行内存占用以及内部存储空间使用率。

---

## 🛠️ 技术架构 (Tech Stack)

- **UI 框架**：Jetpack Compose (Material 3) + Edge-to-Edge 全面屏设计
- **开发语言**：Kotlin 2.0+
- **架构模式**：MVVM + Kotlin Coroutines & Flow (响应式状态管理)
- **ADB 协议实现**：纯 Kotlin 实现的 Socket 通信协议，覆盖 `CNXN`、`AUTH`、`OPEN`、`OKAY`、`CLSE`、`WRTE` 及 `SYNC` 原生协议
- **编解码流**：Android MediaCodec (H.264 NALU 解析与表面渲染)
- **资源解析**：针对 aapt2 与 dumpsys 的流式管道数据解析器

---

## 🚀 快速上手 (Getting Started)

### 1. 准备被控端 Android 设备
1. 进入 **设置 -> 关于手机**，连续点击 7 次“版本号”以启用 **开发者选项**。
2. 进入 **开发者选项**：
   - 开启 **USB 调试**。
   - 若使用无线调试（Android 11+），开启 **无线调试**。
   - 若为 MIUI/HyperOS/ColorOS/OriginOS 等定制系统，建议开启 **“USB 调试（安全设置）”** 以允许模拟点击。

### 2. 建立连接
- **方式 A：无线配对 (Android 11+)**
  1. 在被控设备进入“无线调试 -> 使用配对码配对”。
  2. 在 ADB Helper 中输入被控端的 IP 地址、配对端口及 6 位配对码，点击 **配对**。
  3. 配对成功后，输入无线调试主界面的服务端口并点击 **连接**。
- **方式 B：传统网络调试 (端口 5555)**
  1. 确保两台设备处于同一 Wi-Fi 局域网下。
  2. 在 ADB Helper 中输入被控设备 IP 及 `5555`，点击 **连接**。
  3. 被控设备弹出“允许 USB 调试吗？”提示时，勾选“一律允许”并点击 **确定**。
- **方式 C：OTG 有线连接**
  1. 使用 OTG 转接线将两台手机直连。
  2. 授予 ADB Helper USB 设备访问权限即可秒级连接。

---

## 📦 项目构建 (Build & Run)

```bash
# 克隆仓库
git clone https://github.com/your-username/AdbHelper.git
cd AdbHelper

# 使用 Gradle 构建 Debug APK
gradle assembleDebug

# 构建输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚠️ 免责声明 (Disclaimer)

1. 本工具仅供开发者调试、设备维护及个人合法学习交流使用。
2. 在使用终端执行 `rm`、`kill` 或系统级指令前，请确认命令准确性，因操作不当引起的数据丢失由使用者自行负责。

---

## 📄 开源协议 (License)

```
Copyright 2026 YangYX

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
