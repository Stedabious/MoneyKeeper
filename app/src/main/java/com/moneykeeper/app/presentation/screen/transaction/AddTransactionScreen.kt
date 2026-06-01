package com.moneykeeper.app.presentation.screen.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneykeeper.app.domain.model.Category
import com.moneykeeper.app.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ExpenseColor    = Color(0xFFD32F2F)
private val ExpenseBg       = Color(0xFFFFEBEE)
private val ExpenseSelected = Color(0xFFEF5350)
private val IncomeColor     = Color(0xFF2E7D32)
private val IncomeBg        = Color(0xFFE8F5E9)
private val IncomeSelected  = Color(0xFF43A047)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onDone: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories  by viewModel.incomeCategories.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    val isIncome   = state.transactionType == TransactionType.INCOME
    val isEditMode = state.existingTransactionId > 0L
    val amountColor = if (isIncome) IncomeColor else ExpenseColor
    val amountBg    = if (isIncome) IncomeBg    else ExpenseBg
    val categories  = if (isIncome) incomeCategories else expenseCategories

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onDone()
    }

    if (showDatePicker) {
        val utcMidnight = localToUtcMidnight(state.transactionDate)
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcMidnight)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChanged(it) }
                    showDatePicker = false
                }) { Text("確定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isEditMode -> "編輯記錄"
                            isIncome   -> "新增收入"
                            else       -> "新增支出"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = amountBg,
                    titleContentColor = amountColor,
                    navigationIconContentColor = amountColor,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── 收入/支出 切換 + 金額顯示 ─────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(amountBg)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                // 類型切換 pill
                TypeToggle(
                    isIncome = isIncome,
                    onExpense = { viewModel.onTransactionTypeChanged(TransactionType.EXPENSE) },
                    onIncome  = { viewModel.onTransactionTypeChanged(TransactionType.INCOME) },
                )

                Spacer(Modifier.height(12.dp))

                // 金額顯示
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                ) {
                    if (state.pendingOp != null) {
                        Text(
                            text = "${state.storedValue?.let { formatDisplay(it) }.orEmpty()} ${state.pendingOp}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = amountColor.copy(alpha = 0.6f),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = if (isIncome) "+" else "−",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "NT$",
                            fontSize = 18.sp,
                            color = amountColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = state.displayAmount,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                        )
                    }
                }
            }

            // ── 表單 ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = state.merchant,
                    onValueChange = viewModel::onMerchantChanged,
                    label = { Text(if (isIncome) "來源（選填）" else "商家（選填）") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChanged,
                    label = { Text("備注（選填）") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                // 類別
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "類別",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            CategoryChip(
                                cat = cat,
                                selected = cat.id == state.selectedCategoryId,
                                onClick = { viewModel.onCategorySelected(cat.id) },
                            )
                        }
                    }
                }

                // 日期
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("日期", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(
                        dateFormat.format(Date(state.transactionDate)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = amountColor,
                        fontWeight = FontWeight.Medium,
                    )
                }

                HorizontalDivider()
            }

            // ── 數字鍵盤 ────────────────────────────────────────────────
            NumpadGrid(
                onKey = viewModel::onNumpadKey,
                accentColor = amountColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )

            // ── 儲存 ────────────────────────────────────────────────────
            Button(
                onClick = viewModel::save,
                enabled = state.displayAmount != "0" && !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = amountColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(52.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEditMode) "更新記錄" else if (isIncome) "新增收入" else "新增支出",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeToggle(
    isIncome: Boolean,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(3.dp),
    ) {
        // 支出 pill
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(if (!isIncome) ExpenseSelected else Color.Transparent)
                .clickable(onClick = onExpense)
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "− 支出",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (!isIncome) FontWeight.Bold else FontWeight.Normal,
                color = if (!isIncome) Color.White else MaterialTheme.colorScheme.outline,
            )
        }
        // 收入 pill
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(if (isIncome) IncomeSelected else Color.Transparent)
                .clickable(onClick = onIncome)
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "+ 收入",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Normal,
                color = if (isIncome) Color.White else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun CategoryChip(
    cat: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(cat.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(cat.name, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(catColor, CircleShape)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = catColor.copy(alpha = 0.18f),
            selectedLabelColor = catColor,
        ),
    )
}

private fun localToUtcMidnight(localMidnightMillis: Long): Long {
    val local = Calendar.getInstance()
    local.timeInMillis = localMidnightMillis
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    utc.set(Calendar.MILLISECOND, 0)
    return utc.timeInMillis
}

private fun formatDisplay(v: Double): String {
    val asLong = v.toLong()
    return if (asLong.toDouble() == v) asLong.toString()
    else v.toBigDecimal().stripTrailingZeros().toPlainString()
}

@Composable
private fun NumpadGrid(
    onKey: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("7", "8", "9", "⌫"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "−"),
        listOf(".", "0", "00", "="),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    val isOp = key in setOf("+", "−", "=", "⌫")
                    val actualKey = if (key == "−") "-" else key
                    if (isOp) {
                        FilledTonalButton(
                            onClick = { onKey(actualKey) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) }
                    } else {
                        ElevatedButton(
                            onClick = { onKey(actualKey) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(key, style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        }
    }
}
