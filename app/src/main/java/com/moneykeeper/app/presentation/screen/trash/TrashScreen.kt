package com.moneykeeper.app.presentation.screen.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneykeeper.app.domain.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DAY_MS = 24 * 60 * 60 * 1000L
private const val HOUR_MS = 60 * 60 * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val items by viewModel.trashedItems.collectAsStateWithLifecycle()
    var showEmptyConfirm by remember { mutableStateOf(false) }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("清空垃圾桶") },
            text = { Text("所有垃圾桶內的記錄將永久刪除，無法復原。") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyTrash(); showEmptyConfirm = false }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("垃圾桶") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirm = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "清空垃圾桶")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("垃圾桶是空的", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                item {
                    Text(
                        "記錄刪除後 24 小時自動清除",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(items, key = { it.id }) { tx ->
                    TrashItem(tx, onRestore = { viewModel.restore(tx) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TrashItem(tx: Transaction, onRestore: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val amountFormat = NumberFormat.getNumberInstance(Locale.US)
    val expiryLabel = tx.deletedAt?.let { deletedAt ->
        val remaining = (deletedAt + DAY_MS) - System.currentTimeMillis()
        when {
            remaining <= 0 -> "即將自動刪除"
            remaining < HOUR_MS -> "${(remaining / 60_000).toInt()} 分鐘後自動刪除"
            else -> "${(remaining / HOUR_MS).toInt()} 小時後自動刪除"
        }
    } ?: ""

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tx.merchant.ifEmpty { "消費" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                dateFormat.format(Date(tx.transactionDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                expiryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            "NT$ ${amountFormat.format(tx.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onRestore) {
            Icon(
                Icons.Default.Restore,
                contentDescription = "復原",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
