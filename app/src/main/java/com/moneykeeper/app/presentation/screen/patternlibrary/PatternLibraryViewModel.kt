package com.moneykeeper.app.presentation.screen.patternlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneykeeper.app.data.database.entity.RegexPatternEntity
import com.moneykeeper.app.data.notification.PatternType
import com.moneykeeper.app.data.repository.RegexPatternRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatternLibraryUiState(
    val patterns: List<RegexPatternEntity> = emptyList(),
    val selectedType: PatternType? = null,   // null = show all
)

@HiltViewModel
class PatternLibraryViewModel @Inject constructor(
    private val repository: RegexPatternRepository,
) : ViewModel() {

    private val _selectedType = MutableStateFlow<PatternType?>(null)

    val uiState: StateFlow<PatternLibraryUiState> = combine(
        repository.getAll(),
        _selectedType,
    ) { all, type ->
        PatternLibraryUiState(
            patterns = if (type == null) all else all.filter { it.patternType == type.name },
            selectedType = type,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatternLibraryUiState())

    fun selectType(type: PatternType?) {
        _selectedType.value = type
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    /** Returns all patterns as a formatted string for AI-assisted parser writing. */
    fun formatForExport(patterns: List<RegexPatternEntity>): String {
        if (patterns.isEmpty()) return "（尚無儲存的 Pattern）"
        val sb = StringBuilder()
        sb.appendLine("// ╔══ MoneyKeeper Regex Pattern Library ══╗")
        sb.appendLine("// 共 ${patterns.size} 個 Pattern，請根據以下內容補充 Parser")
        sb.appendLine()
        PatternType.entries.forEach { type ->
            val group = patterns.filter { it.patternType == type.name }
            if (group.isEmpty()) return@forEach
            sb.appendLine("// ═══ ${type.displayLabel} ═══")
            group.forEach { p ->
                sb.appendLine("// App: ${p.sourceAppLabel} (${p.sourcePackageName})")
                if (p.note.isNotBlank()) sb.appendLine("// 備註: ${p.note}")
                sb.appendLine("// 來源: ${p.sourceBody.take(80)}${if (p.sourceBody.length > 80) "…" else ""}")
                val testLabel = when (p.testPassed) { true -> "✓ MATCH"; false -> "✗ NO MATCH"; null -> "? 未測試" }
                sb.appendLine("// 測試: $testLabel")
                sb.appendLine(p.patternString)
                sb.appendLine()
            }
        }
        return sb.toString().trimEnd()
    }
}
