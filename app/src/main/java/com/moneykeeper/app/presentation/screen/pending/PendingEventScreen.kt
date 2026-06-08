package com.moneykeeper.app.presentation.screen.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneykeeper.app.domain.model.Category
import com.moneykeeper.app.domain.model.PendingEvent
import com.moneykeeper.app.domain.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ExpenseColor = Color(0xFFD32F2F)
private val IncomeColor  = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingEventScreen(
    onBack: () -> Unit,
    viewModel: PendingEventViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories  by viewModel.incomeCategories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待確認消費") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (state.events.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("目前沒有待確認的消費", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.events, key = { it.id }) { event ->
                    val isIncome = event.transactionType == TransactionType.INCOME
                    val categories = if (isIncome) incomeCategories else expenseCategories
                    PendingEventCard(
                        event = event,
                        categories = categories,
                        onConfirm = { categoryId -> viewModel.confirm(event, categoryId) },
                        onReject = { viewModel.reject(event) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingEventCard(
    event: PendingEvent,
    categories: List<Category>,
    onConfirm: (Long) -> Unit,
    onReject: () -> Unit,
) {
    val amountFormat = NumberFormat.getNumberInstance(Locale.US)
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val isIncome = event.transactionType == TransactionType.INCOME
    val accentColor = if (isIncome) IncomeColor else ExpenseColor
    val defaultCategoryId = event.categoryId ?: if (isIncome) 14L else 8L
    var selectedCategoryId by remember(event.id) { mutableStateOf(defaultCategoryId) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 頂部列：類型徽章 + 時間
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = if (isIncome) "收入" else "支出",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    dateFormat.format(Date(event.eventTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(Modifier.height(8.dp))

            // 商家 + 金額
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    event.merchant ?: if (isIncome) "收入通知" else "未知商家",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (event.amount != null) {
                    Text(
                        text = "${if (isIncome) "+" else "−"}NT$ ${amountFormat.format(event.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 信心度
            LinearProgressIndicator(
                progress = { event.confidence },
                modifier = Modifier.fillMaxWidth(),
                color = accentColor,
            )
            Text(
                "辨識信心度 ${(event.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(Modifier.height(12.dp))

            // 類別選擇
            if (categories.isNotEmpty()) {
                Text(
                    "類別",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        PendingCategoryChip(
                            cat = cat,
                            selected = cat.id == selectedCategoryId,
                            onClick = { selectedCategoryId = cat.id },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 操作按鈕
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("略過")
                }
                Button(
                    onClick = { onConfirm(selectedCategoryId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isIncome) "確認收入" else "確認支出")
                }
            }
        }
    }
}

@Composable
private fun PendingCategoryChip(
    cat: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(cat.colorHex))
    }.getOrDefault(Color.Gray)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                cat.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        leadingIcon = {
            Box(modifier = Modifier.size(8.dp).background(catColor, CircleShape))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = catColor.copy(alpha = 0.15f),
            selectedLabelColor = catColor,
        ),
    )
}
