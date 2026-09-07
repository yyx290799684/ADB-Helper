package com.yangyx.adbhelper.ui.screens

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.ui.AdbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier
) {
    val outputText by viewModel.terminalOutput.collectAsState()
    val isRootMode by viewModel.isRootMode.collectAsState()
    val terminalPath by viewModel.terminalPath.collectAsState()
    val shortcutCommands by viewModel.shortcutCommands.collectAsState()
    val commandHistory by viewModel.commandHistory.collectAsState()

    var inputTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var historyPointer by remember { mutableIntStateOf(-1) }
    var draftCommand by remember { mutableStateOf("") }

    var fontSizeSp by remember { mutableFloatStateOf(13f) }
    var isWrapMode by remember { mutableStateOf(true) }
    var showShortcutSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var historySearchQuery by remember { mutableStateOf("") }

    // Dialog state for adding/editing shortcut
    var showAddDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var dialogCommandText by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val shortcutScrollState = rememberScrollState()

    // Auto-scroll to bottom on new output
    LaunchedEffect(outputText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val sheetState = rememberModalBottomSheetState()
    val historySheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    fun triggerTabCompletion() {
        coroutineScope.launch {
            val curText = inputTextFieldValue.text
            val sel = inputTextFieldValue.selection.start
            val result = viewModel.performTabCompletion(curText, sel)
            if (result != null) {
                inputTextFieldValue = TextFieldValue(
                    text = result.first,
                    selection = TextRange(result.second)
                )
                draftCommand = result.first
            }
        }
    }

    // History navigation helper functions (Direction Up: backwards in history; Direction Down: forwards towards newest/draft)
    fun navigateHistoryUp() {
        if (commandHistory.isEmpty()) return
        if (historyPointer == -1) {
            draftCommand = inputTextFieldValue.text
            val newIndex = commandHistory.size - 1
            historyPointer = newIndex
            val targetCmd = commandHistory[newIndex]
            inputTextFieldValue = TextFieldValue(
                text = targetCmd,
                selection = TextRange(targetCmd.length)
            )
        } else if (historyPointer > 0) {
            val newIndex = historyPointer - 1
            historyPointer = newIndex
            val targetCmd = commandHistory[newIndex]
            inputTextFieldValue = TextFieldValue(
                text = targetCmd,
                selection = TextRange(targetCmd.length)
            )
        }
    }

    fun navigateHistoryDown() {
        if (historyPointer == -1) return
        if (historyPointer < commandHistory.size - 1) {
            val newIndex = historyPointer + 1
            historyPointer = newIndex
            val targetCmd = commandHistory[newIndex]
            inputTextFieldValue = TextFieldValue(
                text = targetCmd,
                selection = TextRange(targetCmd.length)
            )
        } else {
            // Reached beyond latest -> restore draft text
            historyPointer = -1
            inputTextFieldValue = TextFieldValue(
                text = draftCommand,
                selection = TextRange(draftCommand.length)
            )
        }
    }

    fun sendCurrentCommand() {
        val cmd = inputTextFieldValue.text
        if (cmd.isNotBlank()) {
            viewModel.executeCommand(cmd)
            inputTextFieldValue = TextFieldValue("")
            draftCommand = ""
            historyPointer = -1
        }
    }

    val isSystemDark = isSystemInDarkTheme()
    val terminalColors = if (isSystemDark) OneHalfDark else OneHalfLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // Expanded Terminal Output Window
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = terminalColors.background
            ),
            border = if (!isSystemDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)) else null,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        fontSizeSp = (fontSizeSp * zoom).coerceIn(5f, 32f)
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val scrollModifier = if (!isWrapMode) {
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .verticalScroll(scrollState)
                        .horizontalScroll(horizontalScrollState)
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .verticalScroll(scrollState)
                }

                Column(
                    modifier = scrollModifier
                        .drawWithContent {
                            drawContent()
                            val totalHeight = size.height
                            val totalScrollable = scrollState.maxValue.toFloat()
                            if (totalScrollable > 0) {
                                val visibleRatio = totalHeight / (totalHeight + totalScrollable)
                                val barHeight = (totalHeight * visibleRatio).coerceAtLeast(36f)
                                val scrollOffsetRatio = scrollState.value.toFloat() / totalScrollable
                                val barOffsetY = scrollOffsetRatio * (totalHeight - barHeight)

                                drawRoundRect(
                                    color = if (terminalColors.isDark) Color(0xAA5C6370) else Color(0x66A0A1A7),
                                    topLeft = Offset(size.width - 3.dp.toPx(), barOffsetY),
                                    size = Size(3.dp.toPx(), barHeight),
                                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                                )
                            }
                            if (!isWrapMode && horizontalScrollState.maxValue > 0) {
                                val totalWidth = size.width
                                val totalHorizScrollable = horizontalScrollState.maxValue.toFloat()
                                val visibleRatio = totalWidth / (totalWidth + totalHorizScrollable)
                                val barWidth = (totalWidth * visibleRatio).coerceAtLeast(36f)
                                val scrollOffsetRatio = horizontalScrollState.value.toFloat() / totalHorizScrollable
                                val barOffsetX = scrollOffsetRatio * (totalWidth - barWidth)

                                drawRoundRect(
                                    color = if (terminalColors.isDark) Color(0xAA5C6370) else Color(0x66A0A1A7),
                                    topLeft = Offset(barOffsetX, size.height - 3.dp.toPx()),
                                    size = Size(barWidth, 3.dp.toPx()),
                                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                                )
                            }
                        }
                ) {
                    val annotatedOutput = remember(outputText, terminalColors) {
                        buildTerminalAnnotatedString(outputText, terminalColors)
                    }
                    SelectionContainer {
                        Text(
                            text = annotatedOutput,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.35f).sp,
                            softWrap = isWrapMode
                        )
                    }
                }

                // Top right overlay action buttons (Wrap Toggle, Font Size, Copy & Clear)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(terminalColors.overlayBar, shape = RoundedCornerShape(8.dp))
                        .border(0.5.dp, if (terminalColors.isDark) Color(0x22FFFFFF) else Color(0x1F000000), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { isWrapMode = !isWrapMode },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isWrapMode) terminalColors.blue.copy(alpha = 0.2f) else if (terminalColors.isDark) Color(0x1AFFFFFF) else Color(0x14000000)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            if (isWrapMode) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已开启自动换行",
                                    tint = terminalColors.blue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = "自动换行",
                                color = if (isWrapMode) terminalColors.blue else terminalColors.comment,
                                fontSize = 10.sp,
                                fontWeight = if (isWrapMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Text(
                        text = "${fontSizeSp.toInt()}pt",
                        color = terminalColors.comment,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(outputText))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制全部输出",
                            tint = terminalColors.overlayText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearTerminal() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清空控制台",
                            tint = terminalColors.overlayText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Terminal Toolbar / Accessory Bar (For Touch navigation & History calling without physical keyboard)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(shortcutScrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Touch Arrow UP (Previous History Command)
            FilledTonalIconButton(
                onClick = { navigateHistoryUp() },
                enabled = commandHistory.isNotEmpty(),
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "上一条历史命令 (方向键上)",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Touch Arrow DOWN (Next History Command)
            FilledTonalIconButton(
                onClick = { navigateHistoryDown() },
                enabled = historyPointer != -1,
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "下一条历史命令 (方向键下)",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Tab Button (Shell Auto-Completion)
            Surface(
                onClick = { triggerTabCompletion() },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Tab",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // History Commands Sheet Button (Touch-accessible history browser)
            FilledTonalButton(
                onClick = {
                    historySearchQuery = ""
                    showHistorySheet = true
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "历史命令",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (commandHistory.isNotEmpty()) "历史 (${commandHistory.size})" else "历史",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Shortcut Commands Sheet Button
            FilledTonalButton(
                onClick = { showShortcutSheet = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "快捷命令",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "快捷指令",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quick character insertion buttons for mobile typing (single quote, double quote, underscore, semicolon, backslash, redirect, etc.)
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickSymbols = listOf("'", "\"", "_", ";", "\\", ">", ">>", "<", "|", "/", "-", "&", "~", "$")
                quickSymbols.forEach { sym ->
                    Surface(
                        onClick = {
                            val cur = inputTextFieldValue.text
                            val sel = inputTextFieldValue.selection.start
                            val newText = cur.substring(0, sel) + sym + cur.substring(sel)
                            inputTextFieldValue = TextFieldValue(newText, TextRange(sel + sym.length))
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = sym,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val promptPrefix = if (isRootMode) "#" else "$"
            val promptColor = if (isRootMode) Color(0xFFF43F5E) else Color(0xFF10B981)

            OutlinedTextField(
                value = inputTextFieldValue,
                onValueChange = { newVal ->
                    inputTextFieldValue = newVal
                    if (historyPointer != -1 && newVal.text != commandHistory.getOrNull(historyPointer)) {
                        draftCommand = newVal.text
                    }
                },
                placeholder = {
                    Text(
                        text = "输入指令 (支持方向键 ↑/↓ 调出历史)...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = promptColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                prefix = {
                    Text(
                        text = "$promptPrefix ",
                        color = promptColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Terminal",
                        tint = promptColor
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { sendCurrentCommand() }
                ),
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { keyEvent ->
                        // Intercept physical keys (Up/Down arrow for history, Tab for auto-completion)
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val nativeCode = keyEvent.nativeKeyEvent.keyCode
                            when {
                                keyEvent.key == Key.Tab || nativeCode == AndroidKeyEvent.KEYCODE_TAB -> {
                                    triggerTabCompletion()
                                    true
                                }
                                keyEvent.key == Key.DirectionUp || nativeCode == AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                    navigateHistoryUp()
                                    true
                                }
                                keyEvent.key == Key.DirectionDown || nativeCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                    navigateHistoryDown()
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { sendCurrentCommand() },
                modifier = Modifier
                    .background(
                        promptColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "发送命令", tint = Color.White)
            }
        }
    }

    // Modal Bottom Sheet for Command History (No keyboard / touch friendly)
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = historySheetState
        ) {
            val reversedHistory = remember(commandHistory) { commandHistory.asReversed() }
            val filteredHistory = remember(reversedHistory, historySearchQuery) {
                if (historySearchQuery.isBlank()) {
                    reversedHistory
                } else {
                    reversedHistory.filter { it.contains(historySearchQuery, ignoreCase = true) }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "命令历史记录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${commandHistory.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (commandHistory.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearHistoryDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "清空历史",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("清空历史", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Filter for History
                if (commandHistory.size > 5) {
                    OutlinedTextField(
                        value = historySearchQuery,
                        onValueChange = { historySearchQuery = it },
                        placeholder = { Text("搜索历史命令...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (historySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { historySearchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "清除",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (historySearchQuery.isNotBlank()) "未匹配到相关历史命令" else "暂无已执行的历史命令",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "在终端中输入并执行命令后将自动保存在此",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "点击命令直接填入输入框，或点击 ▶ 立即执行：",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredHistory, key = { it }) { cmd ->
                            Surface(
                                onClick = {
                                    inputTextFieldValue = TextFieldValue(cmd, TextRange(cmd.length))
                                    draftCommand = cmd
                                    historyPointer = -1
                                    showHistorySheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = cmd,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Run immediately button
                                    IconButton(
                                        onClick = {
                                            viewModel.executeCommand(cmd)
                                            inputTextFieldValue = TextFieldValue("")
                                            showHistorySheet = false
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "立即执行",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Copy button
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(cmd))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "复制",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Delete from history
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCommandFromHistory(cmd)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除记录",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Bottom Sheet for Shortcut Commands
    if (showShortcutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShortcutSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "快捷 ADB / Shell 命令",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(onClick = { viewModel.resetShortcutCommands() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "重置默认", tint = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = {
                                dialogCommandText = ""
                                editingIndex = null
                                showAddDialog = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(shortcutCommands) { index, cmd ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cmd,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                // Execute Button
                                IconButton(
                                    onClick = {
                                        inputTextFieldValue = TextFieldValue(cmd, TextRange(cmd.length))
                                        viewModel.executeCommand(cmd)
                                        showShortcutSheet = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "执行",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Edit Button
                                IconButton(
                                    onClick = {
                                        editingIndex = index
                                        dialogCommandText = cmd
                                        showAddDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "编辑",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Delete Button
                                IconButton(
                                    onClick = {
                                        viewModel.deleteShortcutCommand(index)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("清空历史记录") },
            text = { Text("确定要清空全部终端历史命令记录吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCommandHistory()
                        historyPointer = -1
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Dialog for Adding or Editing Shortcut Command
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(if (editingIndex == null) "添加快捷命令" else "编辑快捷命令")
            },
            text = {
                OutlinedTextField(
                    value = dialogCommandText,
                    onValueChange = { dialogCommandText = it },
                    label = { Text("输入 shell 命令") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = editingIndex
                        if (index == null) {
                            viewModel.addShortcutCommand(dialogCommandText)
                        } else {
                            viewModel.editShortcutCommand(index, dialogCommandText)
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// One Half Color Palette (from https://github.com/sonph/onehalf)
private data class TerminalPalette(
    val background: Color,
    val foreground: Color,
    val red: Color,
    val green: Color,
    val yellow: Color,
    val blue: Color,
    val purple: Color,
    val cyan: Color,
    val comment: Color,
    val gutterFg: Color,
    val selection: Color,
    val overlayBar: Color,
    val overlayText: Color,
    val brightWhite: Color,
    val isDark: Boolean
)

private val OneHalfDark = TerminalPalette(
    background  = Color(0xFF282C34), // #282c34
    foreground  = Color(0xFFDCDFE4), // #dcdfe4
    red         = Color(0xFFE06C75), // #e06c75
    green       = Color(0xFF98C379), // #98c379
    yellow      = Color(0xFFE5C07B), // #e5c07b
    blue        = Color(0xFF61AFEF), // #61afef
    purple      = Color(0xFFC678DD), // #c678dd
    cyan        = Color(0xFF56B6C2), // #56b6c2
    comment     = Color(0xFF5C6370), // #5c6370
    gutterFg    = Color(0xFF4B5263), // #4b5263
    selection   = Color(0xFF474E5D), // #474e5d
    overlayBar  = Color(0xDD21252B), // #21252b
    overlayText = Color(0xFFDCDFE4), // #dcdfe4
    brightWhite = Color(0xFFFFFFFF), // #ffffff
    isDark      = true
)

private val OneHalfLight = TerminalPalette(
    background  = Color(0xFFFAFAFA), // #fafafa
    foreground  = Color(0xFF383A42), // #383a42
    red         = Color(0xFFE45649), // #e45649
    green       = Color(0xFF50A14F), // #50a14f
    yellow      = Color(0xFFC18401), // #c18401
    blue        = Color(0xFF0184BC), // #0184bc
    purple      = Color(0xFFA626A4), // #a626a4
    cyan        = Color(0xFF0997B3), // #0997b3
    comment     = Color(0xFFA0A1A7), // #a0a1a7
    gutterFg    = Color(0xFF9D9D9F), // #9d9d9f
    selection   = Color(0xFFE5E5E6), // #e5e5e6
    overlayBar  = Color(0xEEF0F0F0), // #f0f0f0
    overlayText = Color(0xFF383A42), // #383a42
    brightWhite = Color(0xFFFFFFFF), // #ffffff
    isDark      = false
)

// Precompiled Regular Expressions for One Half Terminal Syntax Highlighting
private object TerminalRegexPatterns {
    val AnsiEscape = Regex("\u001B\\[([0-9;]*)m")
    
    // Shell prompt: e.g. root@android:/sdcard # ls or shell@android /sdcard $ ls or 130|shell@phone:/ $ ls
    val PromptFull = Regex("^(?:(\\d+\\|)?([a-zA-Z0-9_\\-\\.]+@[a-zA-Z0-9_\\-\\.]+)[\\s:]+([^#$]+)([$#]))\\s*(.*)$")
    val PromptSimple = Regex("^([$#])\\s*(.*)$")
    
    // Logcat format A: 08-29 12:34:56.789 1234 5678 E TagName: Log message
    val LogcatTime = Regex("^(\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\d+)\\s+(\\d+)\\s+([VDIWEAF])\\s+([^:]+):\\s*(.*)$")
    // Logcat format B: E/TagName( 1234): Log message or I/TagName: Log message
    val LogcatBrief = Regex("^([VDIWEAF])/([^(:\\s]+)(?:\\((\\s*\\d+)\\))?:\\s*(.*)$")
    
    // getprop: [ro.build.version.sdk]: [34]
    val GetProp = Regex("^\\[([^\\]]+)\\]:\\s*\\[([^\\]]*)\\]$")
    
    // ls -l: drwxr-xr-x 2 root root 4096 2026-08-29 12:00 file
    val LsLong = Regex("^([dcbpsl-][rwxstST-]{9})\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}|\\w{3}\\s+\\d+\\s+[\\d:]+)\\s+(.+)$")
    
    // Table headers (ps, top)
    val TableHeader = Regex("^(USER\\s+PID\\s+PPID\\s+VSZ\\s+RSS.*|PID\\s+USER\\s+PR\\s+NI.*|UID\\s+PID\\s+PPID.*)$", RegexOption.IGNORE_CASE)

    // Tokenizer Regex for generic output text
    val TokenPattern = Regex(
        "(?<COMMENT>#[^\n]*)|" +
        "(?<URL>https?://[^\\s\"'<>]+)|" +
        "(?<IP>\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?::\\d+)?\\b)|" +
        "(?<MAC>\\b(?:[0-9A-Fa-f]{2}[:-]){5}(?:[0-9A-Fa-f]{2})\\b)|" +
        "(?<HEX>\\b0x[0-9a-fA-F]+\\b)|" +
        "(?<UUID>\\b[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}\\b)|" +
        "(?<PACKAGE>package:[a-zA-Z0-9_\\.]+)|" +
        "(?<FLAG>(?:^|\\s)(?:--[a-zA-Z0-9_\\-]+|-[a-zA-Z0-9]))|" +
        "(?<PATH>(?:\\.{1,2}/|[a-zA-Z0-9_\\-]+/|/[a-zA-Z0-9_\\.\\-]+)+[a-zA-Z0-9_\\.\\-]*)|" +
        "(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')|" +
        "(?<NUMUNIT>\\b\\d+(?:\\.\\d+)?\\s*(?:KB|MB|GB|TB|PB|B|bytes|ms|s|sec|fps|Hz|kHz|MHz|GHz|%|px|dp|sp|dpi)\\b)|" +
        "(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)|" +
        "(?<SUCCESS>\\b(?:Success|SUCCESS|OK|Connected|passed|enabled|active|true)\\b)|" +
        "(?<ERROR>\\b(?:Error|ERROR|FAILED|Failure|failed|Exception|fatal|denied|killed|false|null|nil|SIGSEGV|abort|not found|No such file)\\b)|" +
        "(?<WARN>\\b(?:Warning|WARN|WARNING|caution|disabled|timeout|deprecated)\\b)|" +
        "(?<KEY>^[a-zA-Z0-9_\\.\\-]+(?=\\s*[:=]\\s+))|" +
        "(?<OP>[{}\\[\\]():=><|&;*+^~])|" +
        "(?<TEXT>[^\\s{}\\[\\]():=><|&;*+^~#\"']+)|" +
        "(?<WS>\\s+)",
        RegexOption.IGNORE_CASE
    )
}

/**
 * Builds annotated string for terminal output using One Half syntax highlighting and ANSI code support.
 */
private fun buildTerminalAnnotatedString(text: String, theme: TerminalPalette): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            // Check for ANSI Escape Sequences in the line
            if (TerminalRegexPatterns.AnsiEscape.containsMatchIn(line)) {
                renderAnsiLine(this, line, theme)
            } else {
                renderHighlightedLine(this, line, theme)
            }

            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

/**
 * Parses and renders lines with standard ANSI Escape Sequences using One Half palette mapping.
 */
private fun renderAnsiLine(builder: AnnotatedString.Builder, line: String, theme: TerminalPalette) {
    var currentIndex = 0
    var currentColor: Color? = null
    var isBold = false

    val matches = TerminalRegexPatterns.AnsiEscape.findAll(line)
    for (match in matches) {
        val textBefore = line.substring(currentIndex, match.range.first)
        if (textBefore.isNotEmpty()) {
            builder.withStyle(
                SpanStyle(
                    color = currentColor ?: theme.foreground,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                )
            ) {
                append(textBefore)
            }
        }

        // Parse code
        val codes = match.groupValues[1].split(";").mapNotNull { it.toIntOrNull() }
        if (codes.isEmpty() || codes.contains(0)) {
            currentColor = null
            isBold = false
        }
        for (code in codes) {
            when (code) {
                0 -> { currentColor = null; isBold = false }
                1 -> isBold = true
                30 -> currentColor = if (theme.isDark) theme.background else theme.foreground
                31 -> currentColor = theme.red
                32 -> currentColor = theme.green
                33 -> currentColor = theme.yellow
                34 -> currentColor = theme.blue
                35 -> currentColor = theme.purple
                36 -> currentColor = theme.cyan
                37 -> currentColor = theme.foreground
                39 -> currentColor = null
                90 -> currentColor = theme.comment
                91 -> currentColor = theme.red
                92 -> currentColor = theme.green
                93 -> currentColor = theme.yellow
                94 -> currentColor = theme.blue
                95 -> currentColor = theme.purple
                96 -> currentColor = theme.cyan
                97 -> currentColor = if (theme.isDark) theme.brightWhite else theme.foreground
            }
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < line.length) {
        val remaining = line.substring(currentIndex)
        builder.withStyle(
            SpanStyle(
                color = currentColor ?: theme.foreground,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            )
        ) {
            append(remaining)
        }
    }
}

/**
 * Line & Token-level syntax highlighting based on One Half theme grammar.
 */
private fun renderHighlightedLine(builder: AnnotatedString.Builder, line: String, theme: TerminalPalette) {
    val trimmed = line.trimStart()

    // 1. Full Shell Prompt (e.g. root@android:/sdcard # ls -la)
    val promptMatch = TerminalRegexPatterns.PromptFull.find(trimmed)
    if (promptMatch != null) {
        val leadingWs = line.substring(0, line.length - trimmed.length)
        builder.append(leadingWs)

        val exitCode = promptMatch.groupValues[1]
        val userHost = promptMatch.groupValues[2]
        val path = promptMatch.groupValues[3]
        val symbol = promptMatch.groupValues[4]
        val command = promptMatch.groupValues[5]

        if (exitCode.isNotEmpty()) {
            builder.withStyle(SpanStyle(color = theme.red, fontWeight = FontWeight.Bold)) {
                append(exitCode)
            }
        }

        val isRoot = userHost.startsWith("root")
        val userColor = if (isRoot) theme.red else theme.green
        builder.withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
            append(userHost)
        }

        builder.withStyle(SpanStyle(color = theme.comment)) {
            append(":")
        }

        builder.withStyle(SpanStyle(color = theme.blue, fontWeight = FontWeight.SemiBold)) {
            append(path)
        }

        val symbolColor = if (symbol == "#") theme.red else theme.yellow
        builder.withStyle(SpanStyle(color = symbolColor, fontWeight = FontWeight.Bold)) {
            append(symbol)
            append(" ")
        }

        renderCommandLineTokens(builder, command, theme)
        return
    }

    // 2. Simple prompt ($ cmd or # cmd)
    val simplePromptMatch = TerminalRegexPatterns.PromptSimple.find(trimmed)
    if (simplePromptMatch != null) {
        val leadingWs = line.substring(0, line.length - trimmed.length)
        builder.append(leadingWs)

        val symbol = simplePromptMatch.groupValues[1]
        val cmd = simplePromptMatch.groupValues[2]
        val symbolColor = if (symbol == "#") theme.red else theme.yellow

        builder.withStyle(SpanStyle(color = symbolColor, fontWeight = FontWeight.Bold)) {
            append(symbol)
            append(" ")
        }
        renderCommandLineTokens(builder, cmd, theme)
        return
    }

    // 3. Logcat Format A: Timestamp PID TID Level Tag: Message
    val logcatTimeMatch = TerminalRegexPatterns.LogcatTime.find(trimmed)
    if (logcatTimeMatch != null) {
        val leadingWs = line.substring(0, line.length - trimmed.length)
        builder.append(leadingWs)

        val time = logcatTimeMatch.groupValues[1]
        val pid = logcatTimeMatch.groupValues[2]
        val tid = logcatTimeMatch.groupValues[3]
        val level = logcatTimeMatch.groupValues[4]
        val tag = logcatTimeMatch.groupValues[5]
        val message = logcatTimeMatch.groupValues[6]

        builder.withStyle(SpanStyle(color = theme.comment)) {
            append(time)
            append(" ")
        }
        builder.withStyle(SpanStyle(color = theme.purple)) {
            append(pid)
            append(" ")
            append(tid)
            append(" ")
        }

        val (levelColor, isBoldLevel) = when (level) {
            "E", "F", "A" -> theme.red to true
            "W" -> theme.yellow to true
            "I" -> theme.cyan to true
            "D" -> theme.blue to true
            else -> theme.comment to false
        }
        builder.withStyle(SpanStyle(color = levelColor, fontWeight = if (isBoldLevel) FontWeight.Bold else FontWeight.Normal)) {
            append(level)
            append(" ")
        }

        builder.withStyle(SpanStyle(color = theme.blue, fontWeight = FontWeight.Bold)) {
            append(tag)
        }
        builder.withStyle(SpanStyle(color = theme.comment)) {
            append(": ")
        }

        renderGenericTokens(builder, message, theme, defaultColor = if (level == "E" || level == "F") theme.red else null)
        return
    }

    // 4. Logcat Format B: Level/Tag(PID): Message
    val logcatBriefMatch = TerminalRegexPatterns.LogcatBrief.find(trimmed)
    if (logcatBriefMatch != null) {
        val leadingWs = line.substring(0, line.length - trimmed.length)
        builder.append(leadingWs)

        val level = logcatBriefMatch.groupValues[1]
        val tag = logcatBriefMatch.groupValues[2]
        val pid = logcatBriefMatch.groupValues[3]
        val message = logcatBriefMatch.groupValues[4]

        val levelColor = when (level) {
            "E", "F", "A" -> theme.red
            "W" -> theme.yellow
            "I" -> theme.cyan
            "D" -> theme.blue
            else -> theme.comment
        }

        builder.withStyle(SpanStyle(color = levelColor, fontWeight = FontWeight.Bold)) {
            append(level)
            append("/")
        }
        builder.withStyle(SpanStyle(color = theme.blue, fontWeight = FontWeight.Bold)) {
            append(tag)
        }
        if (pid.isNotEmpty()) {
            builder.withStyle(SpanStyle(color = theme.comment)) { append("(") }
            builder.withStyle(SpanStyle(color = theme.purple)) { append(pid) }
            builder.withStyle(SpanStyle(color = theme.comment)) { append(")") }
        }
        builder.withStyle(SpanStyle(color = theme.comment)) {
            append(": ")
        }

        renderGenericTokens(builder, message, theme, defaultColor = if (level == "E" || level == "F") theme.red else null)
        return
    }

    // 5. getprop format: [prop.key]: [value]
    val getPropMatch = TerminalRegexPatterns.GetProp.find(trimmed)
    if (getPropMatch != null) {
        val leadingWs = line.substring(0, line.length - trimmed.length)
        builder.append(leadingWs)

        val key = getPropMatch.groupValues[1]
        val value = getPropMatch.groupValues[2]

        builder.withStyle(SpanStyle(color = theme.comment)) { append("[") }
        builder.withStyle(SpanStyle(color = theme.blue, fontWeight = FontWeight.SemiBold)) { append(key) }
        builder.withStyle(SpanStyle(color = theme.comment)) { append("]: [") }
        
        val valColor = when {
            value.all { it.isDigit() || it == '.' } && value.isNotEmpty() -> theme.purple
            value.equals("true", ignoreCase = true) || value.equals("running", ignoreCase = true) -> theme.green
            value.equals("false", ignoreCase = true) || value.equals("stopped", ignoreCase = true) -> theme.red
            else -> theme.green
        }
        builder.withStyle(SpanStyle(color = valColor)) { append(value) }
        builder.withStyle(SpanStyle(color = theme.comment)) { append("]") }
        return
    }

    // 6. ls -l file listing
    val lsMatch = TerminalRegexPatterns.LsLong.find(trimmed)
    if (lsMatch != null) {
        val leadingWs = line.substring(0, line.length - trimmed.length)
        builder.append(leadingWs)

        val perm = lsMatch.groupValues[1]
        val links = lsMatch.groupValues[2]
        val owner = lsMatch.groupValues[3]
        val group = lsMatch.groupValues[4]
        val size = lsMatch.groupValues[5]
        val date = lsMatch.groupValues[6]
        val name = lsMatch.groupValues[7]

        val typeChar = perm[0]
        val typeColor = when (typeChar) {
            'd' -> theme.blue
            'l' -> theme.cyan
            'c', 'b' -> theme.yellow
            else -> theme.comment
        }
        builder.withStyle(SpanStyle(color = typeColor, fontWeight = FontWeight.Bold)) {
            append(typeChar.toString())
        }
        builder.withStyle(SpanStyle(color = theme.green)) {
            append(perm.substring(1))
            append(" ")
        }
        builder.withStyle(SpanStyle(color = theme.purple)) {
            append(links)
            append(" ")
        }
        builder.withStyle(SpanStyle(color = theme.yellow)) {
            append(owner)
            append(" ")
            append(group)
            append(" ")
        }
        builder.withStyle(SpanStyle(color = theme.purple)) {
            append(size)
            append(" ")
        }
        builder.withStyle(SpanStyle(color = theme.comment)) {
            append(date)
            append(" ")
        }

        // File name styling
        when {
            typeChar == 'd' -> {
                builder.withStyle(SpanStyle(color = theme.blue, fontWeight = FontWeight.Bold)) {
                    append(name)
                }
            }
            typeChar == 'l' && name.contains(" -> ") -> {
                val parts = name.split(" -> ")
                builder.withStyle(SpanStyle(color = theme.cyan)) { append(parts[0]) }
                builder.withStyle(SpanStyle(color = theme.yellow)) { append(" -> ") }
                builder.withStyle(SpanStyle(color = theme.blue)) { append(parts.getOrElse(1) { "" }) }
            }
            perm.contains("x") -> {
                builder.withStyle(SpanStyle(color = theme.green, fontWeight = FontWeight.Bold)) {
                    append(name)
                }
            }
            else -> {
                builder.withStyle(SpanStyle(color = theme.foreground)) {
                    append(name)
                }
            }
        }
        return
    }

    // 7. Table Header (ps, top)
    if (TerminalRegexPatterns.TableHeader.matches(trimmed)) {
        builder.withStyle(SpanStyle(color = theme.cyan, fontWeight = FontWeight.Bold)) {
            append(line)
        }
        return
    }

    // 8. Context message
    if (trimmed.startsWith("[Context]") || trimmed.startsWith(">>>")) {
        builder.withStyle(SpanStyle(color = theme.cyan, fontWeight = FontWeight.SemiBold)) {
            append(line)
        }
        return
    }

    // 9. Generic Line with Token-based Syntax Highlighting
    renderGenericTokens(builder, line, theme)
}

/**
 * Tokenizes and renders command line invocation arguments.
 */
private fun renderCommandLineTokens(builder: AnnotatedString.Builder, commandText: String, theme: TerminalPalette) {
    val tokens = commandText.split(" ")
    tokens.forEachIndexed { index, token ->
        when {
            index == 0 -> {
                // Command binary name
                builder.withStyle(SpanStyle(color = theme.green, fontWeight = FontWeight.Bold)) {
                    append(token)
                }
            }
            token.startsWith("--") || (token.startsWith("-") && token.length > 1) -> {
                // Flags / Options
                builder.withStyle(SpanStyle(color = theme.cyan)) {
                    append(token)
                }
            }
            token.startsWith("/") || token.startsWith("./") || token.startsWith("~/") -> {
                // File Path
                builder.withStyle(SpanStyle(color = theme.blue)) {
                    append(token)
                }
            }
            token.startsWith("\"") || token.startsWith("'") || token.endsWith("\"") || token.endsWith("'") -> {
                // Quoted Strings
                builder.withStyle(SpanStyle(color = theme.yellow)) {
                    append(token)
                }
            }
            token in listOf("|", ">", ">>", "<", "&&", "||", ";", "&") -> {
                // Pipes & Operators
                builder.withStyle(SpanStyle(color = theme.yellow, fontWeight = FontWeight.Bold)) {
                    append(token)
                }
            }
            token.startsWith("$") -> {
                // Environment variables
                builder.withStyle(SpanStyle(color = theme.cyan, fontWeight = FontWeight.SemiBold)) {
                    append(token)
                }
            }
            token.all { it.isDigit() || it == '.' || it == ':' || it == '%' } && token.isNotBlank() -> {
                // Numbers
                builder.withStyle(SpanStyle(color = theme.purple)) {
                    append(token)
                }
            }
            else -> {
                builder.withStyle(SpanStyle(color = theme.foreground)) {
                    append(token)
                }
            }
        }
        if (index < tokens.size - 1) {
            builder.append(" ")
        }
    }
}

/**
 * Parses arbitrary text and applies token-level One Half syntax highlighting.
 */
private fun renderGenericTokens(
    builder: AnnotatedString.Builder,
    text: String,
    theme: TerminalPalette,
    defaultColor: Color? = null
) {
    var lastIndex = 0
    val matches = TerminalRegexPatterns.TokenPattern.findAll(text)

    for (match in matches) {
        val range = match.range
        if (range.first > lastIndex) {
            val unmatched = text.substring(lastIndex, range.first)
            builder.withStyle(SpanStyle(color = defaultColor ?: theme.foreground)) {
                append(unmatched)
            }
        }

        val token = match.value
        val groups = match.groups

        when {
            groups["COMMENT"] != null -> {
                builder.withStyle(SpanStyle(color = theme.comment)) {
                    append(token)
                }
            }
            groups["URL"] != null -> {
                builder.withStyle(
                    SpanStyle(
                        color = theme.blue,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(token)
                }
            }
            groups["IP"] != null -> {
                builder.withStyle(SpanStyle(color = theme.cyan, fontWeight = FontWeight.SemiBold)) {
                    append(token)
                }
            }
            groups["MAC"] != null || groups["HEX"] != null || groups["UUID"] != null -> {
                builder.withStyle(SpanStyle(color = theme.purple)) {
                    append(token)
                }
            }
            groups["PACKAGE"] != null -> {
                builder.withStyle(SpanStyle(color = theme.purple)) {
                    append("package:")
                }
                builder.withStyle(SpanStyle(color = theme.green)) {
                    append(token.removePrefix("package:"))
                }
            }
            groups["FLAG"] != null -> {
                builder.withStyle(SpanStyle(color = theme.cyan)) {
                    append(token)
                }
            }
            groups["PATH"] != null -> {
                builder.withStyle(SpanStyle(color = theme.blue)) {
                    append(token)
                }
            }
            groups["STRING"] != null -> {
                builder.withStyle(SpanStyle(color = theme.green)) {
                    append(token)
                }
            }
            groups["NUMUNIT"] != null || groups["NUMBER"] != null -> {
                builder.withStyle(SpanStyle(color = theme.purple)) {
                    append(token)
                }
            }
            groups["SUCCESS"] != null -> {
                builder.withStyle(SpanStyle(color = theme.green, fontWeight = FontWeight.Bold)) {
                    append(token)
                }
            }
            groups["ERROR"] != null -> {
                builder.withStyle(SpanStyle(color = theme.red, fontWeight = FontWeight.Bold)) {
                    append(token)
                }
            }
            groups["WARN"] != null -> {
                builder.withStyle(SpanStyle(color = theme.yellow, fontWeight = FontWeight.Bold)) {
                    append(token)
                }
            }
            groups["KEY"] != null -> {
                builder.withStyle(SpanStyle(color = theme.blue, fontWeight = FontWeight.SemiBold)) {
                    append(token)
                }
            }
            groups["OP"] != null -> {
                builder.withStyle(SpanStyle(color = theme.yellow)) {
                    append(token)
                }
            }
            else -> {
                builder.withStyle(SpanStyle(color = defaultColor ?: theme.foreground)) {
                    append(token)
                }
            }
        }
        lastIndex = range.last + 1
    }

    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex)
        builder.withStyle(SpanStyle(color = defaultColor ?: theme.foreground)) {
            append(remaining)
        }
    }
}
