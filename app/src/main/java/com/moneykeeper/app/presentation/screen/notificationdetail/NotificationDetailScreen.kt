package com.moneykeeper.app.presentation.screen.notificationdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneykeeper.app.data.notification.LineSenderAnalyzer
import com.moneykeeper.app.data.notification.NotificationCategory
import com.moneykeeper.app.data.notification.ParseStatus
import com.moneykeeper.app.data.notification.ParserRegistry
import com.moneykeeper.app.domain.model.EventSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationDetailScreen(
    onBack: () -> Unit,
    viewModel: NotificationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("刪除記錄") },
            text = { Text("確定要永久刪除這筆通知記錄？") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); showDeleteDialog = false }) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!state.isReparsing) {
                        IconButton(onClick = { viewModel.reparse() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "重新解析")
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "刪除",
                            tint = MaterialTheme.colorScheme.error)
                    }
                },
            )
        },
    ) { padding ->
        val log = state.log
        if (log == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isReparsing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text("重新解析中…")
                }
            }

            // Basic info
            DetailSection("基本資訊") {
                DetailRow("App 標籤", log.appLabel)
                DetailRow("Package", log.packageName)
                DetailRow("時間", SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)))
                DetailRow("來源", EventSource.fromName(log.eventSource).displayLabel)
                DetailRow("類別", NotificationCategory.fromName(log.category).displayLabel)
                DetailRow("Parse 版本", "${log.parseVersion} (目前 v${ParserRegistry.PARSE_VERSION})")
                if (log.lastParsedAt != null) {
                    DetailRow("最後解析", SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()).format(Date(log.lastParsedAt)))
                }
            }

            // Notification content
            DetailSection("通知內容") {
                if (log.title.isNotBlank()) {
                    DetailRow("標題", log.title)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("內文", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                    IconButton(
                        onClick = {
                            val rawText = buildString {
                                if (log.title.isNotBlank()) appendLine(log.title)
                                append(log.body)
                            }
                            clipboardManager.setText(AnnotatedString(rawText))
                        },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "複製原始通知",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { clipboardManager.setText(AnnotatedString(log.body)) },
                    ),
                ) {
                    Text(
                        log.body,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "長按內文可複製",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            // Filter result
            DetailSection("過濾結果") {
                DetailRow("已過濾", if (log.isFiltered) "是" else "否")
                if (log.filteredReason != null) {
                    DetailRow("過濾原因", log.filteredReason)
                }
                if (log.lineSenderType != null) {
                    val senderType = runCatching {
                        LineSenderAnalyzer.SenderType.valueOf(log.lineSenderType)
                    }.getOrNull()
                    DetailRow("LINE 發送者分析", senderType?.displayLabel ?: log.lineSenderType)
                }
            }

            // Parse result
            DetailSection("解析結果") {
                val status = ParseStatus.fromName(log.parseStatus)
                DetailRow("狀態", status.displayLabel)
                if (log.parserName != null) {
                    DetailRow("解析器", log.parserName.removeSuffix("Parser"))
                }
                if (log.parsedAmount != null) {
                    DetailRow("金額", "NT$ ${log.parsedAmount}")
                }
                if (log.confidence != null) {
                    val score = (log.confidence * 100).toInt()
                    val level = when {
                        score >= 60 -> "HIGH"
                        score >= 40 -> "MED"
                        score >= 25 -> "LOW"
                        else -> "PARTIAL"
                    }
                    DetailRow("信心分數", "$score/100 ($level)")
                }
                DetailRow("建立待確認事件", if (status.shouldCreatePendingEvent) "是" else "否")
            }

            // Parse trace / confidence breakdown
            if (!log.parseTrace.isNullOrBlank()) {
                DetailSection("信心分析 / 解析追蹤") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(log.parseTrace)) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "複製解析報告",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            log.parseTrace,
                            modifier = Modifier.padding(10.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.reparse() },
                    enabled = !state.isReparsing,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp))
                    Text("重新解析")
                }
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp))
                    Text("刪除記錄")
                }
            }

            // Copy full debug report button
            OutlinedButton(
                onClick = {
                    val report = buildDebugReport(
                        packageName = log.packageName,
                        appLabel = log.appLabel,
                        title = log.title,
                        body = log.body,
                        parseStatus = log.parseStatus,
                        parserName = log.parserName,
                        parsedAmount = log.parsedAmount,
                        parseTrace = log.parseTrace,
                    )
                    clipboardManager.setText(AnnotatedString(report))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp))
                Text("複製完整 Debug 報告")
            }
        }
    }
}

private fun buildDebugReport(
    packageName: String,
    appLabel: String,
    title: String,
    body: String,
    parseStatus: String,
    parserName: String?,
    parsedAmount: Double?,
    parseTrace: String?,
): String = buildString {
    appendLine("=== MoneyKeeper Debug Report ===")
    appendLine("App: $appLabel ($packageName)")
    appendLine("Title: $title")
    appendLine("Body:")
    appendLine(body)
    appendLine()
    appendLine("--- Parse Result ---")
    appendLine("Status: $parseStatus")
    if (parserName != null) appendLine("Parser: $parserName")
    if (parsedAmount != null) appendLine("Amount: NT$$parsedAmount")
    if (!parseTrace.isNullOrBlank()) {
        appendLine()
        appendLine("--- Confidence Trace ---")
        append(parseTrace)
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(0.38f))
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.62f))
    }
}
