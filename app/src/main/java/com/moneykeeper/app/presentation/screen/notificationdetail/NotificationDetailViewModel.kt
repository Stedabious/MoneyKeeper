package com.moneykeeper.app.presentation.screen.notificationdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneykeeper.app.data.database.entity.NotificationLogEntity
import com.moneykeeper.app.data.notification.ParseStatus
import com.moneykeeper.app.data.notification.ParserRegistry
import com.moneykeeper.app.data.notification.RelevanceFilter
import com.moneykeeper.app.data.repository.NotificationLogRepository
import com.moneykeeper.app.data.repository.PendingEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationDetailUiState(
    val log: NotificationLogEntity? = null,
    val isReparsing: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class NotificationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val logRepository: NotificationLogRepository,
    private val parserRegistry: ParserRegistry,
    private val relevanceFilter: RelevanceFilter,
    private val pendingEventRepository: PendingEventRepository,
) : ViewModel() {

    private val logId: Long = savedStateHandle.get<Long>("logId") ?: 0L

    private val _uiState = MutableStateFlow(NotificationDetailUiState())
    val uiState: StateFlow<NotificationDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val log = logRepository.getById(logId)
            _uiState.value = _uiState.value.copy(log = log)
        }
    }

    fun reparse() {
        val log = _uiState.value.log ?: return
        _uiState.value = _uiState.value.copy(isReparsing = true)
        viewModelScope.launch {
            val updated = reparseLog(log)
            logRepository.update(updated)
            _uiState.value = _uiState.value.copy(log = updated, isReparsing = false)
        }
    }

    fun delete() {
        viewModelScope.launch {
            logRepository.deleteById(logId)
            _uiState.value = _uiState.value.copy(deleted = true)
        }
    }

    internal suspend fun reparseLog(log: NotificationLogEntity): NotificationLogEntity {
        val now = System.currentTimeMillis()
        val filterResult = relevanceFilter.filter(log.packageName, log.title, log.body)

        return if (!filterResult.isAllowed) {
            log.copy(
                isFiltered = true,
                filteredReason = filterResult.ignoredReason,
                category = filterResult.category.name,
                lineSenderType = filterResult.lineSenderType,
                parseTrace = null,
                parseStatus = ParseStatus.UNKNOWN.name,
                parsedAmount = null,
                parserName = null,
                confidence = null,
                parseVersion = ParserRegistry.PARSE_VERSION,
                lastParsedAt = now,
            )
        } else {
            val result = parserRegistry.parse(log.packageName, log.title, log.body)
            if (result.status.shouldCreatePendingEvent && result.event?.amount != null) {
                pendingEventRepository.insertFromParsed(result.event)
            }
            log.copy(
                isFiltered = false,
                filteredReason = null,
                category = filterResult.category.name,
                lineSenderType = filterResult.lineSenderType,
                parseStatus = result.status.name,
                parsedAmount = result.event?.amount,
                parserName = result.parserName.takeIf { it != "none" },
                confidence = result.event?.confidence,
                parseTrace = result.parseTrace,
                parseVersion = ParserRegistry.PARSE_VERSION,
                lastParsedAt = now,
            )
        }
    }
}
