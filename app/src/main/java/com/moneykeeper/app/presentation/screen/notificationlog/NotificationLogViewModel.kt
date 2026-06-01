package com.moneykeeper.app.presentation.screen.notificationlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneykeeper.app.data.database.entity.NotificationLogEntity
import com.moneykeeper.app.data.notification.ParseStatus
import com.moneykeeper.app.data.notification.ParserRegistry
import com.moneykeeper.app.data.notification.RelevanceFilter
import com.moneykeeper.app.data.repository.NotificationLogRepository
import com.moneykeeper.app.data.repository.PendingEventRepository
import com.moneykeeper.app.data.repository.TransactionRepository
import com.moneykeeper.app.service.DebugNotificationSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LogFilter(val displayLabel: String) {
    ALL("全部"),
    FINANCIAL("金融"),
    IGNORED("已過濾"),
    DEBUG("Debug"),
}

data class ReparseProgress(
    val isRunning: Boolean = false,
    val total: Int = 0,
    val done: Int = 0,
) {
    val isDone: Boolean get() = !isRunning && done > 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationLogViewModel @Inject constructor(
    private val notificationLogRepository: NotificationLogRepository,
    private val pendingEventRepository: PendingEventRepository,
    private val transactionRepository: TransactionRepository,
    private val debugNotificationSender: DebugNotificationSender,
    private val parserRegistry: ParserRegistry,
    private val relevanceFilter: RelevanceFilter,
) : ViewModel() {

    private val _activeFilter = MutableStateFlow(LogFilter.ALL)
    val activeFilter: StateFlow<LogFilter> = _activeFilter.asStateFlow()

    private val _reparseProgress = MutableStateFlow(ReparseProgress())
    val reparseProgress: StateFlow<ReparseProgress> = _reparseProgress.asStateFlow()

    val logs: StateFlow<List<NotificationLogEntity>> = _activeFilter
        .flatMapLatest { filter ->
            when (filter) {
                LogFilter.ALL      -> notificationLogRepository.getAll()
                LogFilter.FINANCIAL -> notificationLogRepository.getFinancial()
                LogFilter.IGNORED  -> notificationLogRepository.getIgnored()
                LogFilter.DEBUG    -> notificationLogRepository.getDebug()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: LogFilter) {
        _activeFilter.value = filter
    }

    fun injectDebugNotification() {
        debugNotificationSender.sendTestNotification()
    }

    fun clearDebugData() {
        viewModelScope.launch {
            notificationLogRepository.clearDebugData()
            pendingEventRepository.clearDebugData()
            transactionRepository.clearDebugData()
        }
    }

    fun clearIgnored() {
        viewModelScope.launch { notificationLogRepository.clearIgnored() }
    }

    fun clearAllLogs() {
        viewModelScope.launch { notificationLogRepository.clearAll() }
    }

    fun reparseFailedLogs() {
        if (_reparseProgress.value.isRunning) return
        viewModelScope.launch {
            val failed = notificationLogRepository.getFailedParsed()
            if (failed.isEmpty()) return@launch
            _reparseProgress.value = ReparseProgress(isRunning = true, total = failed.size, done = 0)
            failed.forEachIndexed { index, log ->
                val updated = reparseLog(log)
                notificationLogRepository.update(updated)
                _reparseProgress.value = _reparseProgress.value.copy(done = index + 1)
            }
            _reparseProgress.value = ReparseProgress(isRunning = false, total = failed.size, done = failed.size)
        }
    }

    fun reparseOutdatedLogs() {
        if (_reparseProgress.value.isRunning) return
        viewModelScope.launch {
            val outdated = notificationLogRepository.getOlderThanVersion(ParserRegistry.PARSE_VERSION)
            if (outdated.isEmpty()) return@launch
            _reparseProgress.value = ReparseProgress(isRunning = true, total = outdated.size, done = 0)
            outdated.forEachIndexed { index, log ->
                val updated = reparseLog(log)
                notificationLogRepository.update(updated)
                _reparseProgress.value = _reparseProgress.value.copy(done = index + 1)
            }
            _reparseProgress.value = ReparseProgress(isRunning = false, total = outdated.size, done = outdated.size)
        }
    }

    fun clearReparseProgress() {
        _reparseProgress.value = ReparseProgress()
    }

    private suspend fun reparseLog(log: NotificationLogEntity): NotificationLogEntity {
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
