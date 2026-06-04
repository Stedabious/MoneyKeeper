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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneykeeper.app.domain.model.Category
import com.moneykeeper.app.domain.model.PendingEvent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingEventScreen(
    onBack: () -> Unit,
    viewModel: PendingEventViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.expenseCategories.collectAsStateWithLifecycle()

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
    var selectedCategoryId by remember(event.id) { mutableStateOf(event.categoryId ?: 8L) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 商家 + 金額
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.merchant ?: "未知商家",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        dateFormat.format(Date(event.eventTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (event.amount != null) {
                    Text(
                        "NT$ ${amountFormat.format(event.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 信心度
            LinearProgressIndicator(
                progress = { event.confidence },
                modifier = Modifier.fillMaxWidth(),
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
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("確認記帳")
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
