package com.moneykeeper.app.presentation.screen.patternlibrary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneykeeper.app.data.database.entity.RegexPatternEntity
import com.moneykeeper.app.data.notification.PatternType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternLibraryScreen(
    onBack: () -> Unit,
    viewModel: PatternLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pattern 庫") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.patterns.isNotEmpty()) {
                        IconButton(onClick = {
                            val text = viewModel.formatForExport(state.patterns)
                            context.copyToClipboard("MoneyKeeper Regex Patterns", text)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "複製全部到剪貼簿")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Type filter row
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedType == null,
                    onClick = { viewModel.selectType(null) },
                    label = { Text("全部 (${state.patterns.size})") },
                )
                PatternType.entries.forEach { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { viewModel.selectType(type) },
                        label = { Text(type.displayLabel) },
                    )
                }
            }

            HorizontalDivider()

            if (state.patterns.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("尚無儲存的 Pattern", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.patterns, key = { it.id }) { pattern ->
                        PatternItem(
                            pattern = pattern,
                            onCopy = {
                                context.copyToClipboard("regex", pattern.patternString)
                            },
                            onDelete = { viewModel.delete(pattern.id) },
                        )
                        HorizontalDivider()
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PatternItem(
    pattern: RegexPatternEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    val type = PatternType.fromName(pattern.patternType)
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("刪除 Pattern") },
            text = { Text("確定要刪除此 Pattern？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); confirmDelete = false }) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(type.displayLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(pattern.sourceAppLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
                pattern.testPassed?.let { passed ->
                    Icon(
                        imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(dateFormat.format(Date(pattern.createdAt)),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }

        Spacer(Modifier.height(6.dp))

        // Pattern string (monospace box)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                pattern.patternString,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (pattern.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(pattern.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }

        Spacer(Modifier.height(4.dp))

        // Source preview
        Text(
            "來源：" + pattern.sourceBody.take(60) + if (pattern.sourceBody.length > 60) "…" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )

        // Actions
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "複製", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "刪除",
                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun Context.copyToClipboard(label: String, text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
