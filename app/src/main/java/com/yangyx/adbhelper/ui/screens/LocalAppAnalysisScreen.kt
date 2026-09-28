@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.yangyx.adbhelper.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.ConnectionState
import com.yangyx.adbhelper.ui.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalAppAnalysisScreen(
    viewModel: AdbViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState is ConnectionState.Connected
    val connectedDeviceTitle = when (val s = connectionState) {
        is ConnectionState.Connected -> s.deviceName.ifBlank { s.ip }
        else -> null
    }

    val report by viewModel.appAnalysisReport.collectAsState()
    val isScanning by viewModel.isAppAnalysisScanning.collectAsState()
    val scanProgress by viewModel.appAnalysisProgress.collectAsState()
    val scanStatus by viewModel.appAnalysisStatus.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "综合概览",
        "Target API",
        "Min API",
        "架构与安全",
        "敏感权限",
        "体积排行",
        "应用检索"
    )

    // Filter states for App List tab
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterChip by remember { mutableStateOf("全部") }
    var selectedAppForDetail by remember { mutableStateOf<AppAnalysisItem?>(null) }

    // Auto trigger scan on first open if empty and connected
    LaunchedEffect(isConnected) {
        if (isConnected && report == null && !isScanning) {
            viewModel.runLocalAppAnalysis(context, force = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "远端应用分析报告",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (report != null) {
                                "被控端${if (connectedDeviceTitle != null) " ($connectedDeviceTitle)" else ""} · 共 ${report?.thirdPartyApps?.size ?: 0} 款第三方应用 · 生成于 ${LocalAppAnalyzer.formatDate(report?.scanTimestamp ?: 0)}"
                            } else if (connectedDeviceTitle != null) {
                                "已连接设备: $connectedDeviceTitle · 深度统计 Target / Min API / 架构"
                            } else {
                                "深度统计分析被控端第三方应用画像"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // Share / Copy Report Action
                    IconButton(
                        onClick = {
                            report?.let { rep ->
                                val md = LocalAppAnalyzer.generateMarkdownReport(rep)
                                clipboardManager.setText(AnnotatedString(md))
                                Toast.makeText(context, "✅ 已复制远端应用 Markdown 分析报告到剪贴板！", Toast.LENGTH_SHORT).show()
                            } ?: run {
                                Toast.makeText(context, "报告尚未生成", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制报告")
                    }

                    // Share intent
                    IconButton(
                        onClick = {
                            report?.let { rep ->
                                val md = LocalAppAnalyzer.generateMarkdownReport(rep)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, md)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "分享远端设备第三方应用分析报告")
                                context.startActivity(shareIntent)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "分享报告")
                    }

                    // Re-scan button
                    IconButton(
                        onClick = { viewModel.runLocalAppAnalysis(context, force = true) },
                        enabled = !isScanning && isConnected
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新分析")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isScanning) {
                // Scanning Progress View
                ScanningProgressView(
                    progress = scanProgress,
                    status = scanStatus,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (!isConnected && report == null) {
                // Unconnected empty state view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "未连接远端 Android 设备",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "应用分析功能专门针对通过 ADB 连接的被控端目标手机进行统计。请先在首页通过无线调试或 USB 数据线连接设备后重试。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("返回并连接设备")
                    }
                }
            } else if (report == null) {
                // Connected but not yet scanned
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "点击开始分析远端设备应用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "将通过 ADB 深度分析被控端手机 (${connectedDeviceTitle ?: "已连接设备"}) 安装的第三方应用，全面统计 Target API、Min API 兼容性、64位架构、安装包体积与权限画像",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.runLocalAppAnalysis(context, force = true) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("立即开始远端深度分析")
                    }
                }
            } else {
                // Full Report Content
                val rep = report!!

                Column(modifier = Modifier.fillMaxSize()) {
                    // Scrollable Category Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }

                    // Content based on selected tab
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTabIndex) {
                            0 -> OverviewSection(
                                report = rep,
                                onNavigateToTab = { selectedTabIndex = it },
                                onFilterApps = { chip ->
                                    searchQuery = ""
                                    selectedFilterChip = chip
                                    selectedTabIndex = 6 // App directory tab
                                }
                            )
                            1 -> TargetSdkSection(
                                report = rep,
                                onSelectSdk = { sdk ->
                                    searchQuery = ""
                                    selectedFilterChip = "API $sdk"
                                    selectedTabIndex = 6
                                }
                            )
                            2 -> MinSdkSection(
                                report = rep,
                                onSelectSdk = { sdk ->
                                    searchQuery = ""
                                    selectedFilterChip = "Min API $sdk"
                                    selectedTabIndex = 6
                                }
                            )
                            3 -> ArchitectureAndSecuritySection(
                                report = rep,
                                onNavigateToFilter = { chip ->
                                    searchQuery = ""
                                    selectedFilterChip = chip
                                    selectedTabIndex = 6
                                },
                                onAppClick = { selectedAppForDetail = it }
                            )
                            4 -> SensitivePermissionsSection(
                                report = rep,
                                onAppClick = { selectedAppForDetail = it }
                            )
                            5 -> StorageLeaderboardSection(
                                report = rep,
                                onAppClick = { selectedAppForDetail = it }
                            )
                            6 -> AppDirectorySection(
                                report = rep,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedFilterChip = selectedFilterChip,
                                onFilterChipSelected = { selectedFilterChip = it },
                                onAppClick = { selectedAppForDetail = it }
                            )
                        }
                    }
                }
            }

            // Bottom sheet for app details
            selectedAppForDetail?.let { app ->
                AppDetailBottomSheet(
                    app = app,
                    viewModel = viewModel,
                    onDismiss = { selectedAppForDetail = null }
                )
            }
        }
    }
}

/**
 * Scanning progress presentation
 */
@Composable
private fun ScanningProgressView(
    progress: Float,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(56.dp),
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "正在全面分析远端应用...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 1. Comprehensive Overview Tab
 */
@Composable
private fun OverviewSection(
    report: AppAnalysisReport,
    onNavigateToTab: (Int) -> Unit,
    onFilterApps: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Metric Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "第三方应用总数",
                        mainValue = "${report.thirdPartyApps.size} 款",
                        subtitle = "总存储: ${LocalAppAnalyzer.formatBytes(report.totalStorageBytes)}",
                        icon = Icons.Default.Apps,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(6) }
                    )
                    MetricCard(
                        title = "平均目标 API",
                        mainValue = "API ${String.format("%.1f", report.averageTargetSdk)}",
                        subtitle = "Android 14+ 达标率: ${String.format("%.0f", report.modernTargetSdkRate)}%",
                        icon = Icons.Default.TrendingUp,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "64位架构支持",
                        mainValue = "${report.arm64AppsCount} 款",
                        subtitle = if (report.pure32BitAppsCount > 0) "⚠️ 纯32位: ${report.pure32BitAppsCount} 款" else "全部为64位或纯Java",
                        icon = Icons.Default.Memory,
                        containerColor = if (report.pure32BitAppsCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = if (report.pure32BitAppsCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(3) }
                    )
                    MetricCard(
                        title = "安全健康标记",
                        mainValue = "${report.debuggableAppsCount} 调试",
                        subtitle = "${report.backupAllowedAppsCount}款允许备份 · ${report.splitApksCount}分包",
                        icon = Icons.Default.Security,
                        containerColor = if (report.debuggableAppsCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (report.debuggableAppsCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(3) }
                    )
                }
            }
        }

        // Actionable Warning Banners
        if (report.pure32BitAppsCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "发现 ${report.pure32BitAppsCount} 款纯 32 位遗留架构应用",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "纯 32 位应用无法在最新纯 64 位芯片（如骁龙 8 Gen 3、天玑 9300）上启动运行。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                        TextButton(onClick = { onFilterApps("纯32位") }) {
                            Text("查看应用", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (report.debuggableAppsCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "发现 ${report.debuggableAppsCount} 款处于 Debuggable 调试状态",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "生产环境可调试应用易受 ADB 内存挂钩与逆向分析，存在安全风险。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                        TextButton(onClick = { onFilterApps("Debuggable") }) {
                            Text("立即排查", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (report.outdatedTargetSdkCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "有 ${report.outdatedTargetSdkCount} 款应用 Target API 低于 Android 10 (API 29)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "这类应用较少适配现代系统权限沙盒与后台运行限制，在现代系统上可能被限制安装或弹出警告。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onFilterApps("过时API") }) {
                            Text("查看", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Target SDK Mini Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Target API 分布概况",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(onClick = { onNavigateToTab(1) }) {
                            Text("完整图表 >")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val total = report.thirdPartyApps.size.coerceAtLeast(1)
                    report.targetSdkDistribution.entries.take(5).forEach { (sdk, count) ->
                        val pct = count.toFloat() / total
                        val verName = AppAnalysisItem.sdkToAndroidVersion(sdk)
                        DistributionBarItem(
                            label = "API $sdk ($verName)",
                            count = count,
                            percentage = pct,
                            onClick = { onFilterApps("API $sdk") }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Top 5 Largest Apps Preview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "安装包体积 TOP 5",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(onClick = { onNavigateToTab(5) }) {
                            Text("查看完整榜单 >")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    report.largestApps.take(5).forEachIndexed { index, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when (index) {
                                    0 -> Color(0xFFFFD700)
                                    1 -> Color(0xFFC0C0C0)
                                    2 -> Color(0xFFCD7F32)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index < 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = LocalAppAnalyzer.formatBytes(app.totalSizeBytes),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Generous bottom spacer so nothing gets hidden behind system navigation bar
        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * 2. Target SDK In-Depth Distribution Tab
 */
@Composable
private fun TargetSdkSection(
    report: AppAnalysisReport,
    onSelectSdk: (Int) -> Unit
) {
    val total = report.thirdPartyApps.size.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎯 目标 API (Target SDK) 统计说明",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Target SDK 代表应用开发者针对哪个版本的 Android 系统进行了充分的特性适配与行为测试。数值越高，应用对最新系统的隐私沙盒、通知限制、后台启动等规范遵循越好。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        items(report.targetSdkDistribution.entries.toList(), key = { "target_sdk_${it.key}" }) { (sdk, count) ->
            val pct = count.toFloat() / total
            val verName = AppAnalysisItem.sdkToAndroidVersion(sdk)
            val isOutdated = sdk < 29
            val isModern = sdk >= 34

            Card(
                onClick = { onSelectSdk(sdk) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isOutdated -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        isModern -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "API $sdk",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isModern) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = verName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isModern) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (isOutdated) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = "过时",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "$count 款 (${String.format("%.1f", pct * 100)}%)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isOutdated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "点击查看该版本应用列表 >",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * 3. Min SDK In-Depth Distribution Tab
 */
@Composable
private fun MinSdkSection(
    report: AppAnalysisReport,
    onSelectSdk: (Int) -> Unit
) {
    val total = report.thirdPartyApps.size.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚀 最低 API (Min SDK) 统计说明",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Min SDK 决定了应用能够运行的最早 Android 系统版本。Min SDK 越低，向下兼容性越广泛（例如 API 21 兼容 Android 5.0 及以上）；Min SDK 越高，应用越可以使用较新的底层系统特性。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        items(report.minSdkDistribution.entries.toList(), key = { "min_sdk_${it.key}" }) { (sdk, count) ->
            val pct = count.toFloat() / total
            val verName = AppAnalysisItem.sdkToAndroidVersion(sdk)

            Card(
                onClick = { onSelectSdk(sdk) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Min API $sdk",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "支持 $verName+",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "$count 款 (${String.format("%.1f", pct * 100)}%)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "点击筛选应用 >",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * 4. Architecture & Security Section
 */
@Composable
private fun ArchitectureAndSecuritySection(
    report: AppAnalysisReport,
    onNavigateToFilter: (String) -> Unit,
    onAppClick: (AppAnalysisItem) -> Unit
) {
    var expandedAbiCategory by remember { mutableStateOf<AppAbiCategory?>(null) }
    var expandedPackaging by remember { mutableStateOf<String?>(null) } // "split", "single"
    var expandedSecurity by remember { mutableStateOf<String?>(null) } // "debuggable", "backup_allowed", "backup_disallowed"

    val total = report.thirdPartyApps.size.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // CPU ABI Architecture Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CPU 原生指令集架构 (ABI)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    report.abiCategoryDistribution.forEach { (cat, count) ->
                        val is32 = cat == AppAbiCategory.ARM32_ONLY
                        val pct = count.toFloat() / total
                        val isExpanded = expandedAbiCategory == cat
                        val chipName = when (cat) {
                            AppAbiCategory.ARM64_ONLY -> "纯64位"
                            AppAbiCategory.MULTI_ARCH -> "双架构"
                            AppAbiCategory.ARM32_ONLY -> "纯32位"
                            AppAbiCategory.PURE_JAVA -> "纯Java"
                            AppAbiCategory.X86_64, AppAbiCategory.OTHER -> "x86架构"
                        }
                        val matchingApps = remember(report, cat) {
                            report.thirdPartyApps.filter { it.abiCategory == cat }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedAbiCategory = if (isExpanded) null else cat
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = cat.label,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            if (is32) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.error
                                                ) {
                                                    Text(
                                                        text = "存在兼容隐患",
                                                        color = MaterialTheme.colorScheme.onError,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { pct },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (is32) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$count 款 (${String.format("%.0f", pct * 100)}%)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "收起" else "展开",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (isExpanded) {
                                    InlineAppListPreview(
                                        title = cat.label,
                                        filterChipName = chipName,
                                        apps = matchingApps,
                                        onNavigateToFilter = onNavigateToFilter,
                                        onAppClick = onAppClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Package Formats (Split APKs / App Bundles)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "打包形态 (Split APKs / App Bundle)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Split APKs Card
                        val isSplitExpanded = expandedPackaging == "split"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    expandedPackaging = if (isSplitExpanded) null else "split"
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSplitExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSplitExpanded) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("分包动态交付", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Icon(
                                        if (isSplitExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${report.splitApksCount} 款", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("点击查看清单 ➔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }

                        // Single APK Card
                        val isSingleExpanded = expandedPackaging == "single"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    expandedPackaging = if (isSingleExpanded) null else "single"
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSingleExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSingleExpanded) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("单体完整 APK", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Icon(
                                        if (isSingleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${report.singleApksCount} 款", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("点击查看清单 ➔", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }

                    if (expandedPackaging == "split") {
                        val splitApps = remember(report) { report.thirdPartyApps.filter { it.isSplitApk } }
                        InlineAppListPreview(
                            title = "分包动态交付应用",
                            filterChipName = "分包应用",
                            apps = splitApps,
                            onNavigateToFilter = onNavigateToFilter,
                            onAppClick = onAppClick
                        )
                    } else if (expandedPackaging == "single") {
                        val singleApps = remember(report) { report.thirdPartyApps.filter { !it.isSplitApk } }
                        InlineAppListPreview(
                            title = "单体完整 APK 应用",
                            filterChipName = "单体APK",
                            apps = singleApps,
                            onNavigateToFilter = onNavigateToFilter,
                            onAppClick = onAppClick
                        )
                    }
                }
            }
        }

        // Security & Health Flags
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "安全配置与调试健康",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Debuggable
                    val isDebugExpanded = expandedSecurity == "debuggable"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                expandedSecurity = if (isDebugExpanded) null else "debuggable"
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FLAG_DEBUGGABLE (可调试)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    if (report.debuggableAppsCount == 0) "✅ 良好：未发现处于调试状态的应用" else "⚠️ 发现 ${report.debuggableAppsCount} 款处于调试状态 (存在逆向/泄露风险)",
                                    fontSize = 12.sp,
                                    color = if (report.debuggableAppsCount == 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${report.debuggableAppsCount} 款",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (report.debuggableAppsCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    if (isDebugExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isDebugExpanded) {
                            val debugApps = remember(report) { report.thirdPartyApps.filter { it.isDebuggable } }
                            InlineAppListPreview(
                                title = "处于调试状态的应用",
                                filterChipName = "Debuggable",
                                apps = debugApps,
                                onNavigateToFilter = onNavigateToFilter,
                                onAppClick = onAppClick
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 2. Allow Backup
                    val isBackupAllowedExpanded = expandedSecurity == "backup_allowed"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                expandedSecurity = if (isBackupAllowedExpanded) null else "backup_allowed"
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FLAG_ALLOW_BACKUP = true (允许数据备份)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    "支持 ADB Backup 与系统云端数据快照备份与数据迁移",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${report.backupAllowedAppsCount} 款",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    if (isBackupAllowedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isBackupAllowedExpanded) {
                            val backupApps = remember(report) { report.thirdPartyApps.filter { it.allowBackup } }
                            InlineAppListPreview(
                                title = "允许备份的应用",
                                filterChipName = "允许备份",
                                apps = backupApps,
                                onNavigateToFilter = onNavigateToFilter,
                                onAppClick = onAppClick
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 3. Disallow Backup
                    val isBackupDisallowedExpanded = expandedSecurity == "backup_disallowed"
                    val disallowCount = (report.thirdPartyApps.size - report.backupAllowedAppsCount).coerceAtLeast(0)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                expandedSecurity = if (isBackupDisallowedExpanded) null else "backup_disallowed"
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FLAG_ALLOW_BACKUP = false (禁止数据备份)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    "显式声明禁止快照与导出，保护银行/金融/核心隐私数据",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$disallowCount 款",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    if (isBackupDisallowedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isBackupDisallowedExpanded) {
                            val disallowApps = remember(report) { report.thirdPartyApps.filter { !it.allowBackup } }
                            InlineAppListPreview(
                                title = "显式禁止备份的应用",
                                filterChipName = "禁止备份",
                                apps = disallowApps,
                                onNavigateToFilter = onNavigateToFilter,
                                onAppClick = onAppClick
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun InlineAppListPreview(
    title: String,
    filterChipName: String,
    apps: List<AppAnalysisItem>,
    onNavigateToFilter: (String) -> Unit,
    onAppClick: (AppAnalysisItem) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title (共 ${apps.size} 款)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = { onNavigateToFilter(filterChipName) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("在完整目录中筛选 ➔", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (apps.isEmpty()) {
                Text(
                    text = "暂无匹配的应用",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                val previewApps = apps.take(10)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    previewApps.forEach { app ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconPreview(app = app, size = 32.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (app.isSplitApk) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    text = "分包 (${app.splitCount})",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = app.abiCategory.badgeText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = LocalAppAnalyzer.formatBytes(app.totalSizeBytes),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (apps.size > 10) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "仅预览前 10 款，点击右上角按钮可去应用目录查看全部 ${apps.size} 款应用",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

/**
 * 5. Sensitive Permissions Section
 */
@Composable
private fun SensitivePermissionsSection(
    report: AppAnalysisReport,
    onAppClick: (AppAnalysisItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔐 敏感权限分析说明",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "根据 Android 官方危险权限分类标准，统计第三方应用在清单（Manifest）中申请的敏感隐私权限类型与频次。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Category breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "敏感权限分类申请频次",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val total = report.thirdPartyApps.size.coerceAtLeast(1)

                    SensitivePermissionCategory.values().forEach { cat ->
                        val count = report.sensitivePermissionCounts[cat] ?: 0
                        val pct = count.toFloat() / total

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.title,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${cat.description})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$count 款",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (cat.isHighRisk && count > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (cat.isHighRisk && count > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Top Permission Hungry Apps
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "申请敏感权限较多的应用",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击可查看该应用完整的权限清单与详细配置",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    report.permissionHungryApps.take(15).forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconPreview(app = app, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${app.sensitiveCategories.size} 项敏感分类 · 共 ${app.requestedPermissions.size} 项权限",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * 6. Storage Leaderboard Section
 */
@Composable
private fun StorageLeaderboardSection(
    report: AppAnalysisReport,
    onAppClick: (AppAnalysisItem) -> Unit
) {
    val maxBytes = report.largestApps.firstOrNull()?.totalSizeBytes?.coerceAtLeast(1L) ?: 1L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "第三方应用存储总占用：${LocalAppAnalyzer.formatBytes(report.totalStorageBytes)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "共 ${report.thirdPartyApps.size} 款应用 · 平均每款 ${LocalAppAnalyzer.formatBytes(report.averageStorageBytes)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        items(report.largestApps.take(25), key = { "storage_${it.packageName}" }) { app ->
            val rank = report.largestApps.indexOf(app) + 1
            val relativeRatio = app.totalSizeBytes.toFloat() / maxBytes

            Card(
                onClick = { onAppClick(app) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (rank) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$rank",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rank <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        AppIconPreview(app = app, size = 42.dp)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${app.packageName} · Target API ${app.targetSdk}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = LocalAppAnalyzer.formatBytes(app.totalSizeBytes),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (app.isSplitApk) {
                                Text(
                                    text = "分包 (${app.splitCount})",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { relativeRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * 7. Searchable & Filterable App Directory Section
 */
@Composable
private fun AppDirectorySection(
    report: AppAnalysisReport,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilterChip: String,
    onFilterChipSelected: (String) -> Unit,
    onAppClick: (AppAnalysisItem) -> Unit
) {
    val filterChips = remember(report, selectedFilterChip) {
        val baseList = mutableListOf(
            "全部", "纯64位", "双架构", "纯32位", "纯Java", "x86架构",
            "Debuggable", "允许备份", "禁止备份",
            "分包应用", "单体APK",
            "过时API", "超200MB", "超1年未更新", "近30天更新"
        )
        if (!baseList.contains(selectedFilterChip)) {
            baseList.add(1, selectedFilterChip)
        }
        baseList
    }

    val filteredApps = remember(report, searchQuery, selectedFilterChip) {
        report.thirdPartyApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true) ||
                    app.targetSdk.toString() == searchQuery.trim()

            val matchesFilter = when {
                selectedFilterChip == "全部" -> true
                selectedFilterChip == "纯64位" -> app.abiCategory == AppAbiCategory.ARM64_ONLY
                selectedFilterChip == "双架构" -> app.abiCategory == AppAbiCategory.MULTI_ARCH
                selectedFilterChip == "纯32位" -> app.is32BitOnly
                selectedFilterChip == "纯Java" -> app.abiCategory == AppAbiCategory.PURE_JAVA
                selectedFilterChip == "x86架构" -> app.abiCategory == AppAbiCategory.X86_64 || app.abiCategory == AppAbiCategory.OTHER
                selectedFilterChip == "Debuggable" -> app.isDebuggable
                selectedFilterChip == "允许备份" -> app.allowBackup
                selectedFilterChip == "禁止备份" -> !app.allowBackup
                selectedFilterChip == "分包应用" -> app.isSplitApk
                selectedFilterChip == "单体APK" -> !app.isSplitApk
                selectedFilterChip == "过时API" -> app.isOutdatedTargetSdk
                selectedFilterChip == "超200MB" -> app.totalSizeBytes > 200L * 1024 * 1024
                selectedFilterChip == "超1年未更新" -> app.isDormant
                selectedFilterChip == "近30天更新" -> app.isRecentlyUpdated
                selectedFilterChip.startsWith("API ") -> {
                    val sdkNum = selectedFilterChip.removePrefix("API ").toIntOrNull()
                    sdkNum == null || app.targetSdk == sdkNum
                }
                selectedFilterChip.startsWith("Min API ") -> {
                    val sdkNum = selectedFilterChip.removePrefix("Min API ").toIntOrNull()
                    sdkNum == null || app.minSdk == sdkNum
                }
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val chipListState = rememberLazyListState()

    // Auto scroll to the selected chip (e.g. "分包应用", "单体APK", "纯32位", etc.)
    LaunchedEffect(selectedFilterChip, filterChips) {
        val index = filterChips.indexOf(selectedFilterChip)
        if (index >= 0) {
            // Wait until the LazyRow has items and layout measured
            snapshotFlow { chipListState.layoutInfo.totalItemsCount }
                .filter { it > 0 }
                .first()

            val layoutInfo = chipListState.layoutInfo
            val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            val isFullyVisible = visibleItem != null &&
                    visibleItem.offset >= layoutInfo.viewportStartOffset &&
                    (visibleItem.offset + visibleItem.size) <= layoutInfo.viewportEndOffset

            if (!isFullyVisible) {
                val targetIndex = if (index > 0) index - 1 else 0
                chipListState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar & Filter Chips Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("搜索应用名 / 包名 / Target API...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                state = chipListState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(filterChips) { chip ->
                    FilterChip(
                        selected = selectedFilterChip == chip,
                        onClick = { onFilterChipSelected(chip) },
                        label = { Text(chip, fontSize = 12.sp) },
                        leadingIcon = if (selectedFilterChip == chip) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedFilterChip == "全部") {
                        "共找到 ${filteredApps.size} 款应用"
                    } else {
                        "筛选【$selectedFilterChip】：共 ${filteredApps.size} 款应用"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedFilterChip != "全部" || searchQuery.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onSearchQueryChange("")
                            onFilterChipSelected("全部")
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("重置筛选", fontSize = 11.sp)
                    }
                }
            }
        }

        // App List
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未找到匹配的应用",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { "item_${it.packageName}" }) { app ->
                    AppAnalysisListItemCard(
                        app = app,
                        onClick = { onAppClick(app) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

/**
 * App list item card with metadata badges wrapped gracefully using FlowRow
 */
@Composable
private fun AppAnalysisListItemCard(
    app: AppAnalysisItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconPreview(app = app, size = 46.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v${app.versionName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Badges with FlowRow so they never overflow or get clipped
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Target SDK Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (app.isOutdatedTargetSdk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Target ${app.targetSdk}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (app.isOutdatedTargetSdk) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Min SDK Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Min ${app.minSdk}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // ABI Badge (concise)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (app.is32BitOnly) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        val shortAbi = when (app.abiCategory) {
                            AppAbiCategory.ARM64_ONLY -> "64位"
                            AppAbiCategory.MULTI_ARCH -> "双架构"
                            AppAbiCategory.ARM32_ONLY -> "32位遗留"
                            AppAbiCategory.PURE_JAVA -> "纯Java"
                            AppAbiCategory.X86_64 -> "x86_64"
                            else -> "其他"
                        }
                        Text(
                            text = shortAbi,
                            fontSize = 10.sp,
                            color = if (app.is32BitOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (app.is32BitOnly) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (app.isDebuggable) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "可调试",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = LocalAppAnalyzer.formatBytes(app.totalSizeBytes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = app.installerName,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.isSplitApk) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "分包 (${app.splitCount})",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "单体APK",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Metric Card Component with uniform vertical sizing and clean typography
 */
@Composable
private fun MetricCard(
    title: String,
    mainValue: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mainValue,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                minLines = 2,
                maxLines = 2,
                color = contentColor.copy(alpha = 0.8f),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Distribution Bar Component
 */
@Composable
private fun DistributionBarItem(
    label: String,
    count: Int,
    percentage: Float,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "$count 款 (${String.format("%.1f", percentage * 100)}%)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

/**
 * Bottom Sheet for App Details
 * - Optimized 2-column info grid so text never wraps awkwardly or gets truncated
 * - Full permissions view with friendly names & color risk explanation
 * - Smooth vertical scrolling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailBottomSheet(
    app: AppAnalysisItem,
    viewModel: AdbViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showAllPermissions by remember { mutableStateOf(false) }
    var isVerifyingPath by remember { mutableStateOf(false) }
    var rawPathVerificationOutput by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconPreview(app = app, size = 52.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "版本: v${app.versionName} (代码: ${app.versionCode})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Information Grid (Clean 2-column layout so text never wraps awkwardly)
            Text("📊 核心 API 与系统规范", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoBadge(
                    title = "目标 API (Target SDK)",
                    value = "API ${app.targetSdk}",
                    sub = app.targetSdkAndroidVersion,
                    modifier = Modifier.weight(1f)
                )
                InfoBadge(
                    title = "最低 API (Min SDK)",
                    value = "API ${app.minSdk}",
                    sub = "支持 ${app.minSdkAndroidVersion}+",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoBadge(
                    title = "CPU 原生架构 (ABI)",
                    value = app.abiCategory.badgeText,
                    sub = app.nativeAbis.joinToString().ifEmpty { "纯 Java/Kotlin (无.so)" },
                    modifier = Modifier.weight(1f)
                )
                InfoBadge(
                    title = "安装包体积与形态",
                    value = LocalAppAnalyzer.formatBytes(app.totalSizeBytes),
                    sub = if (app.isSplitApk) "分包 (共${app.splitCount}个切片)" else "单体完整 APK (独立单包)",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            InfoBadge(
                title = "安装来源渠道",
                value = app.installerName,
                sub = app.installerPackage ?: "直接/本地离线安装",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Packaging & Slices Detail Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📦 安装包打包形态与分片明细", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (app.isSplitApk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (app.isSplitApk) "动态分包 (${app.splitCount}个切片)" else "单体完整 APK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (app.isSplitApk) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (app.isSplitApk) {
                            "💡 此应用采用 Google Play / App Bundle 动态分包机制交付，由 1 个主程序基包与 ${app.splitCount - 1} 个架构/资源切片组成。系统仅下载适配当前设备的切片以节省存储。"
                        } else {
                            "💡 此应用采用传统单体 APK 独立打包，全部 Dex 字节码、原生架构动态库 (.so) 及所有界面资源均封装在同一个 APK 中。"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Split Files List
                    val detailsToShow = if (app.splitDetails.isNotEmpty()) app.splitDetails else listOf(
                        ApkSplitDetail(
                            fileName = "base.apk",
                            filePath = "",
                            sizeBytes = app.totalSizeBytes,
                            isBase = true,
                            description = if (app.isSplitApk) "主程序基包" else "单体完整 APK"
                        )
                    )

                    Text(
                        text = "安装文件清单 (${detailsToShow.size} 项)：",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    detailsToShow.forEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (item.isBase) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                text = if (item.isBase) "主基包" else "分片 #${index}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.isBase) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.fileName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (item.sizeBytes > 0) {
                                        Text(
                                            text = LocalAppAnalyzer.formatBytes(item.sizeBytes),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (item.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = item.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (item.filePath.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.filePath,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(item.filePath))
                                                Toast.makeText(context, "已复制路径: ${item.fileName}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "复制路径",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live ADB pm path verification tool
                    if (viewModel != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "需要确认设备端物理文件？",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isVerifyingPath = true
                                        rawPathVerificationOutput = viewModel.runRawShell(
                                            "pm path ${app.packageName} && ls -l $(pm path ${app.packageName} | cut -d: -f2 2>/dev/null)"
                                        )
                                        isVerifyingPath = false
                                    }
                                },
                                enabled = !isVerifyingPath,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                if (isVerifyingPath) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("校验中...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ADB 实时校验", fontSize = 11.sp)
                                }
                            }
                        }

                        rawPathVerificationOutput?.let { rawOutput ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("终端原始输出 (pm path & ls -l)：", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = rawOutput.ifBlank { "(无输出)" },
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time and security status
            Text("🕒 时间与安全配置", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("首次安装时间：${LocalAppAnalyzer.formatDate(app.firstInstallTime)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("最近更新时间：${LocalAppAnalyzer.formatDate(app.lastUpdateTime)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("允许系统/云端备份：${if (app.allowBackup) "是 (FLAG_ALLOW_BACKUP)" else "否"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "调试模式：${if (app.isDebuggable) "⚠️ 处于调试模式 (FLAG_DEBUGGABLE)" else "否 (生产签名)"}",
                        fontSize = 12.sp,
                        color = if (app.isDebuggable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (app.isDebuggable) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permissions Section with clear color explanation & full wrapping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔐 申请系统权限",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "共 ${app.requestedPermissions.size} 项 (含 ${app.sensitiveCategories.size} 项敏感分类)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Color Risk Legend Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "红色高危：涉及相机、录音、定位、短信、通讯录等敏感核心隐私",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "灰色常规：存储文件、前台服务、开机自启、通知等系统功能权限",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sensitive categories wrapped with FlowRow (shows ALL categories, not limited to 5)
            if (app.sensitiveCategories.isEmpty()) {
                Text(
                    text = "该应用未在清单中申请上述危险敏感权限分类",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    app.sensitiveCategories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (cat.isHighRisk) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (cat.isHighRisk) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (cat.isHighRisk) "🔴 ${cat.title}" else "⚪ ${cat.title}",
                                    fontSize = 12.sp,
                                    fontWeight = if (cat.isHighRisk) FontWeight.Bold else FontWeight.Medium,
                                    color = if (cat.isHighRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Button to toggle complete permissions list
            OutlinedButton(
                onClick = { showAllPermissions = !showAllPermissions },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    if (showAllPermissions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showAllPermissions) "收起权限清单" else "展开完整权限清单 (${app.requestedPermissions.size} 项)",
                    fontSize = 13.sp
                )
            }

            // Expandable full permission list
            AnimatedVisibility(visible = showAllPermissions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (app.requestedPermissions.isEmpty()) {
                        Text(
                            text = "未声明任何权限",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        app.requestedPermissions.forEach { perm ->
                            val (friendlyName, isHighRisk) = resolvePermissionDetails(perm)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isHighRisk) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isHighRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (isHighRisk) "高危" else "常规",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHighRisk) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = friendlyName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = perm,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Human-friendly Chinese description resolver for Android permissions
 */
private fun resolvePermissionDetails(permission: String): Pair<String, Boolean> {
    return when (permission) {
        "android.permission.CAMERA" -> Pair("相机拍照 / 录制视频", true)
        "android.permission.RECORD_AUDIO" -> Pair("麦克风录音", true)
        "android.permission.ACCESS_FINE_LOCATION" -> Pair("精准 GPS 地理位置", true)
        "android.permission.ACCESS_COARSE_LOCATION" -> Pair("粗略网络地理位置", true)
        "android.permission.ACCESS_BACKGROUND_LOCATION" -> Pair("后台持续获取位置", true)
        "android.permission.READ_CONTACTS" -> Pair("读取通讯录联系人", true)
        "android.permission.WRITE_CONTACTS" -> Pair("修改通讯录联系人", true)
        "android.permission.READ_CALL_LOG" -> Pair("读取通话记录", true)
        "android.permission.WRITE_CALL_LOG" -> Pair("修改通话记录", true)
        "android.permission.CALL_PHONE" -> Pair("直接拨打电话", true)
        "android.permission.READ_PHONE_STATE" -> Pair("读取设备通话状态与识别码", true)
        "android.permission.READ_SMS" -> Pair("读取短信内容", true)
        "android.permission.SEND_SMS" -> Pair("发送手机短信", true)
        "android.permission.RECEIVE_SMS" -> Pair("接收手机短信", true)
        "android.permission.READ_EXTERNAL_STORAGE" -> Pair("读取外部存储文件", false)
        "android.permission.WRITE_EXTERNAL_STORAGE" -> Pair("写入 / 删除外部存储文件", false)
        "android.permission.MANAGE_EXTERNAL_STORAGE" -> Pair("所有文件访问权限 (全部存储)", true)
        "android.permission.READ_MEDIA_IMAGES" -> Pair("读取相册图片", false)
        "android.permission.READ_MEDIA_VIDEO" -> Pair("读取视频文件", false)
        "android.permission.READ_MEDIA_AUDIO" -> Pair("读取音频媒体", false)
        "android.permission.POST_NOTIFICATIONS" -> Pair("发送通知栏消息", false)
        "android.permission.FOREGROUND_SERVICE" -> Pair("前台常驻服务运行", false)
        "android.permission.SYSTEM_ALERT_WINDOW" -> Pair("显示悬浮窗 / 覆盖在其他应用上方", true)
        "android.permission.REQUEST_INSTALL_PACKAGES" -> Pair("安装未知来源应用", true)
        "android.permission.INTERNET" -> Pair("访问网络", false)
        "android.permission.ACCESS_NETWORK_STATE" -> Pair("查看网络连接状态", false)
        "android.permission.ACCESS_WIFI_STATE" -> Pair("查看 Wi-Fi 状态", false)
        "android.permission.CHANGE_WIFI_STATE" -> Pair("修改 Wi-Fi 开关与连接", false)
        "android.permission.VIBRATE" -> Pair("控制振动器", false)
        "android.permission.WAKE_LOCK" -> Pair("唤醒锁定 / 防止休眠", false)
        "android.permission.RECEIVE_BOOT_COMPLETED" -> Pair("开机自启动", false)
        "android.permission.BLUETOOTH" -> Pair("蓝牙连接", false)
        "android.permission.BLUETOOTH_CONNECT" -> Pair("连接附近蓝牙设备", false)
        "android.permission.BLUETOOTH_SCAN" -> Pair("搜索附近蓝牙设备", true)
        "android.permission.QUERY_ALL_PACKAGES" -> Pair("获取本机已安装所有应用列表", true)
        else -> {
            val simple = permission.substringAfterLast('.')
            val isRisk = permission.contains("LOCATION") ||
                    permission.contains("CAMERA") ||
                    permission.contains("RECORD") ||
                    permission.contains("SMS") ||
                    permission.contains("CONTACT") ||
                    permission.contains("CALL")
            Pair(simple, isRisk)
        }
    }
}

/**
 * Information Badge Component designed with ample space so text never wraps awkwardly
 */
@Composable
private fun InfoBadge(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Cached App Icon Preview
 */
@Composable
private fun AppIconPreview(app: AppAnalysisItem, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(app.packageName) {
        try {
            app.icon?.toBitmap(80, 80)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = app.appName,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f))
        )
    } else {
        Surface(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f)),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}
