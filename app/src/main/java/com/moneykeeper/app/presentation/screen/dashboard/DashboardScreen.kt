package com.moneykeeper.app.presentation.screen.dashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.moneykeeper.app.domain.model.Transaction
import com.moneykeeper.app.domain.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val PIE_COLORS = listOf(
    Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006E1C),
    Color(0xFFBA1A1A), Color(0xFFB15105), Color(0xFF006A6A),
    Color(0xFF6B5778), Color(0xFF825500),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddClick: () -> Unit,
    onPendingClick: () -> Unit,
    onNotificationLogClick: () -> Unit,
    onTrashClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val amountFormat = NumberFormat.getNumberInstance(Locale.US)
    val dateFormat   = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val monthFormat  = SimpleDateFormat("yyyy年M月", Locale.getDefault())
    var showMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPermissionStatus()
        }
    }

    if (showMonthPicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = state.selectedDate }
        MonthYearPickerDialog(
            initialYear  = cal.get(Calendar.YEAR),
            initialMonth = cal.get(Calendar.MONTH),
            onConfirm = { year, month ->
                viewModel.onDateSelected(year to month)
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MoneyKeeper") },
                actions = {
                    IconButton(onClick = onNotificationLogClick) {
                        Icon(Icons.Default.BugReport, contentDescription = "通知記錄")
                    }
                    BadgedBox(
                        badge = { if (state.trashCount > 0) Badge { Text(state.trashCount.toString()) } },
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        IconButton(onClick = onTrashClick) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "垃圾桶")
                        }
                    }
                    if (state.pendingCount > 0) {
                        BadgedBox(
                            badge = { Badge { Text(state.pendingCount.toString()) } },
                            modifier = Modifier.padding(end = 12.dp),
                        ) {
                            IconButton(onClick = onPendingClick) {
                                Icon(Icons.Default.Notifications, contentDescription = "待確認消費")
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // 權限警告 Banner
            PermissionWarningBanner(
                isNLSGranted = state.isNotificationListenerGranted,
                isBattOptIgnored = state.isBatteryOptimizationIgnored,
                onOpenNLSSettings = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onOpenBatterySettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                },
            )

            // ── 上方總結 40% ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 月份標題列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = monthFormat.format(Date(state.selectedDate)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showMonthPicker = true },
                    )
                    val net = state.monthlyIncome - state.monthlyExpense
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "淨收支 ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = if (net >= 0) "+NT$ ${amountFormat.format(net)}"
                                   else "-NT$ ${amountFormat.format(-net)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (net >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                        )
                    }
                }

                // 收支摘要卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                "-NT$ ${amountFormat.format(state.monthlyExpense)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F),
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("收入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                "+NT$ ${amountFormat.format(state.monthlyIncome)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                            )
                        }
                    }
                }

                // 圓餅圖
                if (state.categoryBreakdown.isNotEmpty()) {
                    CompactPieChart(
                        breakdown = state.categoryBreakdown,
                        amountFormat = amountFormat,
                    )
                }
            }

            HorizontalDivider()

            // ── 下方細項 60% ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f),
            ) {
                // 日期導航列
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::onPreviousDay) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前一天")
                    }
                    Text(
                        text = if (state.isToday) "今天" else dateFormat.format(Date(state.selectedDate)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showMonthPicker = true },
                    )
                    IconButton(onClick = viewModel::onNextDay, enabled = !state.isToday) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "後一天")
                    }
                }

                HorizontalDivider()

                // 交易列表
                if (state.transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("這天沒有記錄", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        items(state.transactions, key = { it.id }) { tx ->
                            TransactionRow(
                                tx = tx,
                                amountFormat = amountFormat,
                                onEdit = { onEditClick(tx.id) },
                                onDelete = { viewModel.deleteTransaction(tx) },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningBanner(
    isNLSGranted: Boolean,
    isBattOptIgnored: Boolean,
    onOpenNLSSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    if (isNLSGranted && isBattOptIgnored) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!isNLSGranted) {
                WarningRow(
                    text = "通知監聽未授權，無法自動記帳",
                    actionText = "前往設定",
                    onClick = onOpenNLSSettings,
                )
            }
            if (!isBattOptIgnored) {
                WarningRow(
                    text = "電池優化未關閉，可能中斷背景接收",
                    actionText = "關閉優化",
                    onClick = onOpenBatterySettings,
                )
            }
        }
    }
}

@Composable
private fun WarningRow(
    text: String,
    actionText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF5D4037),
            )
        }
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(
                actionText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE65100),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CompactPieChart(
    breakdown: List<CategoryBreakdown>,
    amountFormat: NumberFormat,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 環形圖
            Box(modifier = Modifier.size(80.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.22f
                    val inset  = stroke / 2f
                    var startAngle = -90f
                    breakdown.forEachIndexed { i, seg ->
                        val sweep = seg.fraction * 360f
                        drawArc(
                            color = PIE_COLORS[i % PIE_COLORS.size],
                            startAngle = startAngle,
                            sweepAngle = sweep - 1f,
                            useCenter = false,
                            topLeft = Offset(inset / 2f, inset / 2f),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke),
                        )
                        startAngle += sweep
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            // 圖例 (最多5項)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                breakdown.take(5).forEachIndexed { i, seg ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = PIE_COLORS[i % PIE_COLORS.size])
                        }
                        Text(
                            "${seg.categoryName} ${(seg.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            // 最大類別金額
            Column(horizontalAlignment = Alignment.End) {
                breakdown.take(3).forEachIndexed { i, seg ->
                    Text(
                        "NT$${amountFormat.format(seg.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PIE_COLORS[i % PIE_COLORS.size],
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    amountFormat: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isIncome = tx.transactionType == TransactionType.INCOME
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tx.merchant.ifEmpty { if (isIncome) "收入" else "消費" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                timeFormat.format(Date(tx.transactionDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Text(
            text = if (isIncome) "+NT$ ${amountFormat.format(tx.amount)}"
                   else "-NT$ ${amountFormat.format(tx.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F),
            fontSize = 14.sp,
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "刪除",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onConfirm: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var showYearPicker by remember { mutableStateOf(false) }
    var selectedYear  by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    val monthNames = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.KeyboardArrowLeft, null)
                    }
                    Text(
                        text = if (showYearPicker) "選擇年份" else "${selectedYear}年",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable { showYearPicker = !showYearPicker },
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (showYearPicker) {
                    val years = (selectedYear - 5..selectedYear + 5).toList()
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        years.chunked(4).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                row.forEach { year ->
                                    TextButton(onClick = { selectedYear = year; showYearPicker = false }, modifier = Modifier.weight(1f)) {
                                        Text("$year", fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal,
                                            color = if (year == selectedYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        monthNames.chunked(4).forEachIndexed { rowIdx, row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                row.forEachIndexed { colIdx, name ->
                                    val month = rowIdx * 4 + colIdx
                                    TextButton(onClick = { selectedMonth = month }, modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                            color = if (month == selectedMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) { Text("確定") }
                }
            }
        }
    }
}
