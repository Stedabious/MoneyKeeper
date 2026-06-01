package com.moneykeeper.app.presentation.screen.regexbuilder

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneykeeper.app.data.database.entity.NotificationLogEntity
import com.moneykeeper.app.data.database.entity.RegexPatternEntity
import com.moneykeeper.app.data.notification.PatternType
import com.moneykeeper.app.data.repository.NotificationLogRepository
import com.moneykeeper.app.data.repository.RegexPatternRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegexBuilderUiState(
    val sourceLog: NotificationLogEntity? = null,
    val patternField: TextFieldValue = TextFieldValue(""),
    val selectedType: PatternType = PatternType.AMOUNT,
    val note: String = "",
    val testPassed: Boolean? = null,   // null = not tested yet / invalid regex
    val saved: Boolean = false,
)

@HiltViewModel
class RegexBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val logRepository: NotificationLogRepository,
    private val patternRepository: RegexPatternRepository,
) : ViewModel() {

    private val logId: Long = savedStateHandle.get<Long>("logId") ?: 0L

    private val _uiState = MutableStateFlow(RegexBuilderUiState())
    val uiState: StateFlow<RegexBuilderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val log = logRepository.getById(logId)
            _uiState.value = _uiState.value.copy(sourceLog = log)
        }
    }

    fun onPatternChanged(value: TextFieldValue) {
        _uiState.value = _uiState.value.copy(patternField = value, testPassed = null, saved = false)
    }

    fun onTypeSelected(type: PatternType) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    fun onNoteChanged(v: String) {
        _uiState.value = _uiState.value.copy(note = v)
    }

    /** Insert a regex snippet at the current cursor position. */
    fun insertSnippet(snippet: String) {
        val s = _uiState.value
        val text = s.patternField.text
        val cursor = s.patternField.selection.end.coerceIn(0, text.length)
        val newText = text.substring(0, cursor) + snippet + text.substring(cursor)
        val newCursor = cursor + snippet.length
        _uiState.value = s.copy(
            patternField = TextFieldValue(newText, TextRange(newCursor)),
            testPassed = null,
        )
    }

    fun testPattern() {
        val s = _uiState.value
        val pattern = s.patternField.text.trim()
        val source = s.sourceLog?.let { "${it.title} ${it.body}" } ?: ""
        val passed = runCatching { Regex(pattern).containsMatchIn(source) }.getOrNull()
        _uiState.value = s.copy(testPassed = passed)
    }

    fun save() {
        val s = _uiState.value
        val log = s.sourceLog ?: return
        val pattern = s.patternField.text.trim()
        if (pattern.isBlank()) return
        val passed = runCatching { Regex(pattern).containsMatchIn("${log.title} ${log.body}") }.getOrNull()
        viewModelScope.launch {
            patternRepository.insert(
                RegexPatternEntity(
                    patternString = pattern,
                    patternType = s.selectedType.name,
                    sourceBody = "${log.title} ${log.body}".trim(),
                    sourcePackageName = log.packageName,
                    sourceAppLabel = log.appLabel,
                    testPassed = passed,
                    note = s.note.trim(),
                )
            )
            _uiState.value = s.copy(saved = true)
        }
    }
}
