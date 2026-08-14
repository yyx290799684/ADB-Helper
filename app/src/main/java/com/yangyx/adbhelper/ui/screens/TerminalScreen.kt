package com.yangyx.adbhelper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    var inputCommand by remember { mutableStateOf("") }
    var fontSizeSp by remember { mutableFloatStateOf(13f) }
    var showShortcutSheet by remember { mutableStateOf(false) }

    // Dialog state for adding/editing shortcut
    var showAddDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var dialogCommandText by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom on new output
    LaunchedEffect(outputText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(12.dp)
    ) {
        // Expanded Terminal Output Window
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0D1117)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        fontSizeSp = (fontSizeSp * zoom).coerceIn(9f, 32f)
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .verticalScroll(scrollState)
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
                                    color = Color(0xAA4B5563),
                                    topLeft = Offset(size.width - 4.dp.toPx(), barOffsetY),
                                    size = Size(4.dp.toPx(), barHeight),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                ) {
                    SelectionContainer {
                        Text(
                            text = buildTerminalAnnotatedString(outputText),
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.35f).sp
                        )
                    }
                }

                // Top right overlay action buttons (Copy & Clear)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xCC161B22), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${fontSizeSp.toInt()}pt",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(outputText))
                        },
                        modifier = Modifier.height(32.dp).width(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制全部输出", tint = Color.LightGray)
                    }
                    IconButton(
                        onClick = { viewModel.clearTerminal() },
                        modifier = Modifier.height(32.dp).width(32.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "清空控制台", tint = Color.LightGray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Shortcut Commands Button
            IconButton(
                onClick = { showShortcutSheet = true },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "快捷命令",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            val promptPrefix = if (isRootMode) "#" else "$"
            val promptColor = if (isRootMode) Color(0xFFF43F5E) else Color(0xFF10B981)

            OutlinedTextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                placeholder = {
                    Text(
                        text = "输入指令 (例如 su, cd /sdcard, ls)...",
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
                    onSend = {
                        if (inputCommand.isNotBlank()) {
                            viewModel.executeCommand(inputCommand)
                            inputCommand = ""
                        }
                    }
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.executeCommand(inputCommand)
                        inputCommand = ""
                    }
                },
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
                                        inputCommand = cmd
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

/**
 * Builds annotated string for terminal output with ZSH-style syntax highlighting scheme.
 */
private fun buildTerminalAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            val trimmed = line.trimStart()
            when {
                // ZSH Prompt line (e.g. root@android:/path # command or shell@android:/path $ command)
                trimmed.contains("@android:") && (trimmed.contains(" # ") || trimmed.contains(" $ ")) -> {
                    val promptSymbolIndex = if (trimmed.contains(" # ")) trimmed.indexOf(" # ") else trimmed.indexOf(" $ ")
                    val promptHeader = trimmed.substring(0, promptSymbolIndex + 3)
                    val cmdText = trimmed.substring(promptSymbolIndex + 3)

                    // Colorize prompt header (e.g. root@android:/sdcard #)
                    val userPart = if (promptHeader.startsWith("root")) "root@android" else "shell@android"
                    val userColor = if (promptHeader.startsWith("root")) Color(0xFFF43F5E) else Color(0xFF10B981)

                    withStyle(SpanStyle(color = userColor, fontWeight = FontWeight.Bold)) {
                        append(userPart)
                    }

                    val afterUser = promptHeader.removePrefix(userPart)
                    val symbolStr = if (afterUser.contains(" # ")) " # " else " $ "
                    val pathStr = afterUser.removeSuffix(symbolStr)

                    withStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) {
                        append(pathStr)
                    }
                    withStyle(SpanStyle(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)) {
                        append(symbolStr)
                    }

                    // ZSH-style command line syntax highlighting
                    val tokens = cmdText.split(" ")
                    tokens.forEachIndexed { tIdx, token ->
                        when {
                            tIdx == 0 -> {
                                withStyle(SpanStyle(color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)) {
                                    append(token)
                                }
                            }
                            token.startsWith("-") -> {
                                withStyle(SpanStyle(color = Color(0xFF7DD3FC))) {
                                    append(token)
                                }
                            }
                            token.startsWith("/") || token.startsWith("./") -> {
                                withStyle(SpanStyle(color = Color(0xFFC084FC))) {
                                    append(token)
                                }
                            }
                            token.startsWith("\"") || token.startsWith("'") || token.endsWith("\"") || token.endsWith("'") -> {
                                withStyle(SpanStyle(color = Color(0xFFFDE047))) {
                                    append(token)
                                }
                            }
                            token.all { it.isDigit() || it == '.' || it == ':' } && token.isNotBlank() -> {
                                withStyle(SpanStyle(color = Color(0xFFFB923C))) {
                                    append(token)
                                }
                            }
                            else -> {
                                withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                                    append(token)
                                }
                            }
                        }
                        if (tIdx < tokens.size - 1) append(" ")
                    }
                }

                // Simple prompt fallback ($ command or # command)
                trimmed.startsWith("$ ") || trimmed.startsWith("# ") -> {
                    val symbol = trimmed.substring(0, 2)
                    val cmd = trimmed.substring(2)
                    val symbolColor = if (symbol.startsWith("#")) Color(0xFFF43F5E) else Color(0xFF10B981)

                    withStyle(SpanStyle(color = symbolColor, fontWeight = FontWeight.Bold)) {
                        append(symbol)
                    }

                    val tokens = cmd.split(" ")
                    tokens.forEachIndexed { tIdx, token ->
                        if (tIdx == 0) {
                            withStyle(SpanStyle(color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)) { append(token) }
                        } else if (token.startsWith("-")) {
                            withStyle(SpanStyle(color = Color(0xFF7DD3FC))) { append(token) }
                        } else {
                            withStyle(SpanStyle(color = Color(0xFFE2E8F0))) { append(token) }
                        }
                        if (tIdx < tokens.size - 1) append(" ")
                    }
                }

                // Context message line
                trimmed.startsWith("[Context]") -> {
                    withStyle(SpanStyle(color = Color(0xFF2DD4BF), fontWeight = FontWeight.SemiBold)) {
                        append(line)
                    }
                }

                // Error / Exception
                trimmed.contains("Error:", ignoreCase = true) ||
                trimmed.contains("[ERROR]", ignoreCase = true) ||
                trimmed.contains("FAILED", ignoreCase = true) ||
                trimmed.contains("Permission denied", ignoreCase = true) ||
                trimmed.contains("Exception", ignoreCase = true) -> {
                    withStyle(SpanStyle(color = Color(0xFFFF6B6B))) {
                        append(line)
                    }
                }

                // Warning
                trimmed.contains("[WARN]", ignoreCase = true) ||
                trimmed.contains("Warning", ignoreCase = true) -> {
                    withStyle(SpanStyle(color = Color(0xFFFACC15))) {
                        append(line)
                    }
                }

                // Success / Connected / Info
                trimmed.contains("[OK]", ignoreCase = true) ||
                trimmed.contains("Success", ignoreCase = true) ||
                trimmed.contains("[INFO]", ignoreCase = true) -> {
                    withStyle(SpanStyle(color = Color(0xFF34D399))) {
                        append(line)
                    }
                }

                // Package name output line (package:com.xxx.yyy)
                trimmed.startsWith("package:") -> {
                    withStyle(SpanStyle(color = Color(0xFFC084FC))) {
                        append("package:")
                    }
                    withStyle(SpanStyle(color = Color(0xFF4ADE80))) {
                        append(line.removePrefix("package:"))
                    }
                }

                // Directory / File listing (drwxr-xr-x or -rw-r--r--)
                trimmed.length > 10 && (trimmed.startsWith("d") || trimmed.startsWith("-") || trimmed.startsWith("l")) && trimmed[1] in listOf('r', '-') -> {
                    val fileType = trimmed.substring(0, 1)
                    val permRest = trimmed.substring(1)
                    val typeColor = if (fileType == "d") Color(0xFF38BDF8) else Color(0xFF94A3B8)
                    withStyle(SpanStyle(color = typeColor, fontWeight = FontWeight.Bold)) {
                        append(fileType)
                    }
                    withStyle(SpanStyle(color = Color(0xFF86EFAC))) {
                        append(permRest)
                    }
                }

                // Absolute file path output line
                trimmed.startsWith("/") -> {
                    withStyle(SpanStyle(color = Color(0xFFA3E635))) {
                        append(line)
                    }
                }

                // Default text
                else -> {
                    withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                        append(line)
                    }
                }
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}
