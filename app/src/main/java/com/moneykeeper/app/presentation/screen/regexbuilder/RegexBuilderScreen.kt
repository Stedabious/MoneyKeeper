package com.moneykeeper.app.presentation.screen.regexbuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneykeeper.app.data.notification.PatternType

private data class Snippet(val label: String, val value: String)

private val SNIPPETS = listOf(
    Snippet("NT\$ 金額", """NT\$\s*([\d,]+)"""),
    Snippet("純數字", """([\d,]+)"""),
    Snippet("商家名稱", """(.{2,20}?)"""),
    Snippet("任意文字", """.+?"""),
    Snippet("空白", """\s*"""),
    Snippet("冒號", """[：:]"""),
    Snippet("於…消費", """於\s*(.{2,20}?)\s*消費"""),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegexBuilderScreen(
    logId: Long,
    onBack: () -> Unit,
    viewModel: RegexBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Regex 建立器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save, enabled = state.patternField.text.isNotBlank()) {
                        Icon(Icons.Default.Save, contentDescription = "儲存")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Source notification ──────────────────────────
            state.sourceLog?.let { log ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("來源通知", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Text(log.appLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (log.title.isNotBlank()) {
                                Text(log.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Text(log.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Snippet helper buttons ───────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("插入 Regex 片段", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SNIPPETS.forEach { snippet ->
                        SuggestionChip(
                            onClick = { viewModel.insertSnippet(snippet.value) },
                            label = { Text(snippet.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            // ── Pattern editor ───────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Regex Pattern", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                ) {
                    BasicTextField(
                        value = state.patternField,
                        onValueChange = viewModel::onPatternChanged,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (state.patternField.text.isEmpty()) {
                                Text(
                                    "在此輸入或點選上方片段...",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                    ),
                                )
                            }
                            inner()
                        },
                    )
                }

                // Test result
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SuggestionChip(
                        onClick = viewModel::testPattern,
                        label = { Text("測試 Pattern") },
                    )
                    state.testPassed?.let { passed ->
                        Icon(
                            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            if (passed) "符合" else "不符合",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // ── Pattern type ─────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Pattern 類型", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    PatternType.entries.forEach { type ->
                        FilterChip(
                            selected = state.selectedType == type,
                            onClick = { viewModel.onTypeSelected(type) },
                            label = { Text(type.displayLabel) },
                        )
                    }
                }
            }

            // ── Note ─────────────────────────────────────────
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備註") },
                placeholder = { Text("例如：用於 CathayParser 的金額擷取") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // ── Save button ──────────────────────────────────
            Button(
                onClick = viewModel::save,
                enabled = state.patternField.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("儲存到 Pattern 庫")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
