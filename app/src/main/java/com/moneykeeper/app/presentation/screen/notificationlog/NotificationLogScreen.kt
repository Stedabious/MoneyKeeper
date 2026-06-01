package com.moneykeeper.app.presentation.screen.notificationlog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneykeeper.app.data.database.entity.NotificationLogEntity
import com.moneykeeper.app.data.notification.NotificationCategory
import com.moneykeeper.app.data.notification.ParseStatus
import com.moneykeeper.app.domain.model.EventSource
import com.moneykeeper.app.service.MoneyNotificationListenerService
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationLogScreen(
    onBack: () -> Unit,
    onRegexBuilder: (Long) -> Unit = {},
    onPatternLibrary: () -> Unit = {},
    onDetail: (Long) -> Unit = {},
    viewModel: NotificationLogViewModel = hiltViewModel(),
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val reparseProgress by viewModel.reparseProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotifPermission = granted }

    var showClearIgnoredDialog by remember { mutableStateOf(false) }

    if (showClearIgnoredDialog) {
        AlertDialog(
            onDismissRequest = { showClearIgnoredDialog = false },
            title = { Text("清除已過濾紀錄") },
            text = { Text("確定要刪除所有「已過濾」的通知紀錄？") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearIgnored(); showClearIgnoredDialog = false }) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearIgnoredDialog = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知記錄") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onPatternLibrary) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Pattern 庫")
                    }
                    if (logs.isNotEmpty()) {
                        if (activeFilter == LogFilter.IGNORED) {
                            IconButton(onClick = { showClearIgnoredDialog = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "清除已過濾")
                            }
                        } else {
                            IconButton(onClick = viewModel::clearAllLogs) {
                                Icon(Icons.Default.Delete, contentDescription = "清除全部記錄")
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                PipelineStatusCard(
                    hasNotifPermission = hasNotifPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenListenerSettings = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                )
            }
            item {
                DebugToolsCard(
                    onInject = viewModel::injectDebugNotification,
                    onClearDebug = viewModel::clearDebugData,
                    onReparseFailedLogs = viewModel::reparseFailedLogs,
                    onReparseOutdated = viewModel::reparseOutdatedLogs,
                    reparseProgress = reparseProgress,
                    onDismissProgress = viewModel::clearReparseProgress,
                )
            }
            // Filter chips
            item {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LogFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.displayLabel) },
                        )
                    }
                }
            }
            item { HorizontalDivider() }

            if (logs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("尚無通知記錄", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    NotificationLogItem(
                        log = log,
                        onBuildRegex = { onRegexBuilder(log.id) },
                        onDetail = { onDetail(log.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PipelineStatusCard(
    hasNotifPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenListenerSettings: () -> Unit,
) {
    val context = LocalContext.current
    val isServiceConnected by MoneyNotificationListenerService.isConnected.collectAsStateWithLifecycle()
    var refreshCount by remember { mutableStateOf(0) }
    val isListenerEnabled = remember(refreshCount) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Pipeline 狀態",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                IconButton(onClick = { refreshCount++ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "重新整理", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            PipelineStatusRow("POST_NOTIFICATIONS 權限", hasNotifPermission)
            PipelineStatusRow("通知存取授權", isListenerEnabled)
            PipelineStatusRow("Service 已連接", isServiceConnected)
            if (!hasNotifPermission || !isListenerEnabled) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasNotifPermission) {
                        OutlinedButton(onClick = onRequestPermission) { Text("授予通知權限") }
                    }
                    if (!isListenerEnabled) {
                        OutlinedButton(onClick = onOpenListenerSettings) { Text("開啟通知存取") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineStatusRow(label: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun DebugToolsCard(
    onInject: () -> Unit,
    onClearDebug: () -> Unit,
    onReparseFailedLogs: () -> Unit,
    onReparseOutdated: () -> Unit,
    reparseProgress: ReparseProgress,
    onDismissProgress: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Debug 工具", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onInject) { Text("注入測試通知") }
                OutlinedButton(onClick = onClearDebug) { Text("清除 Debug 資料") }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReparseFailedLogs,
                    enabled = !reparseProgress.isRunning,
                ) { Text("重解析失敗") }
                OutlinedButton(
                    onClick = onReparseOutdated,
                    enabled = !reparseProgress.isRunning,
                ) { Text("重解析舊版本") }
            }
            // Reparse progress
            if (reparseProgress.isRunning) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("重新解析中… ${reparseProgress.done}/${reparseProgress.total}",
                        style = MaterialTheme.typography.bodySmall)
                }
                LinearProgressIndicator(
                    progress = { if (reparseProgress.total > 0) reparseProgress.done.toFloat() / reparseProgress.total else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            } else if (reparseProgress.isDone) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("完成：已重解析 ${reparseProgress.done} 筆",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onDismissProgress) { Text("關閉") }
                }
            }
        }
    }
}

@Composable
private fun NotificationLogItem(
    log: NotificationLogEntity,
    onBuildRegex: () -> Unit = {},
    onDetail: () -> Unit = {},
) {
    val timeFormat = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
    val amountFormat = NumberFormat.getNumberInstance(Locale.US)
    val isDebug = log.eventSource.startsWith("DEBUG_")
    val isFiltered = log.isFiltered
    val eventSource = EventSource.fromName(log.eventSource)
    val parseStatus = ParseStatus.fromName(log.parseStatus)
    val category = NotificationCategory.fromName(log.category)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    log.appLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isFiltered -> MaterialTheme.colorScheme.outline
                        isDebug    -> MaterialTheme.colorScheme.tertiary
                        else       -> MaterialTheme.colorScheme.primary
                    },
                )
                if (isFiltered) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("已過濾", style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    )
                } else {
                    ParseStatusChip(parseStatus, isDebug)
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(category.displayLabel, style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (category == NotificationCategory.FINANCIAL)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timeFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                if (!isFiltered) {
                    IconButton(onClick = onBuildRegex, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DataObject,
                            contentDescription = "建立 Regex",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        if (log.title.isNotBlank()) {
            Text(log.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }

        Text(
            text = log.body.take(120) + if (log.body.length > 120) "…" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (isFiltered) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isFiltered && log.filteredReason != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                "原因：${log.filteredReason}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(6.dp))

        if (!isFiltered && log.parsedAmount != null && log.confidence != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("NT$ ${amountFormat.format(log.parsedAmount)}") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isDebug) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("${(log.confidence * 100).toInt()}/100") },
                )
                log.parserName?.let { name ->
                    SuggestionChip(onClick = {}, label = { Text(name.removeSuffix("Parser")) })
                }
            }
        } else if (!isFiltered && log.parserName != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SuggestionChip(onClick = {}, label = { Text(log.parserName.removeSuffix("Parser")) })
            }
        }

        TextButton(
            onClick = onDetail,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("詳細", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ParseStatusChip(status: ParseStatus, isDebug: Boolean) {
    val containerColor = when (status) {
        ParseStatus.HIGH_CONFIDENCE   -> if (isDebug) MaterialTheme.colorScheme.tertiaryContainer
                                         else MaterialTheme.colorScheme.primaryContainer
        ParseStatus.MEDIUM_CONFIDENCE -> MaterialTheme.colorScheme.primaryContainer
        ParseStatus.LOW_CONFIDENCE    -> MaterialTheme.colorScheme.secondaryContainer
        ParseStatus.PARTIAL_PARSE     -> MaterialTheme.colorScheme.errorContainer
        ParseStatus.TRANSFER          -> MaterialTheme.colorScheme.secondaryContainer
        ParseStatus.UNKNOWN           -> MaterialTheme.colorScheme.surfaceVariant
        ParseStatus.IGNORED           -> MaterialTheme.colorScheme.errorContainer
        ParseStatus.DUPLICATE         -> MaterialTheme.colorScheme.tertiaryContainer
    }
    SuggestionChip(
        onClick = {},
        label = { Text(status.displayLabel, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = containerColor),
    )
}
