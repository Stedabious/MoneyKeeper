package com.moneykeeper.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.moneykeeper.app.data.database.entity.NotificationLogEntity
import com.moneykeeper.app.data.notification.DuplicateDetector
import com.moneykeeper.app.data.notification.ParseStatus
import com.moneykeeper.app.data.notification.ParserRegistry
import com.moneykeeper.app.data.notification.RelevanceFilter
import com.moneykeeper.app.data.repository.NotificationLogRepository
import com.moneykeeper.app.data.repository.PendingEventRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MoneyNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var parserRegistry: ParserRegistry
    @Inject lateinit var relevanceFilter: RelevanceFilter
    @Inject lateinit var duplicateDetector: DuplicateDetector
    @Inject lateinit var pendingEventRepository: PendingEventRepository
    @Inject lateinit var notificationLogRepository: NotificationLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isConnected.value = false
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: ""
        val timestamp = sbn.postTime

        if (text.isBlank()) return

        val appLabel = resolveAppLabel(packageName)
        Log.d(TAG, "▶ RECEIVED pkg=$packageName label=$appLabel title='$title'")

        scope.launch {
            val filterResult = relevanceFilter.filter(packageName, title, text)
            Log.d(TAG, "▶ FILTER allowed=${filterResult.isAllowed} category=${filterResult.category} line=${filterResult.lineSenderType}")

            val now = System.currentTimeMillis()

            if (!filterResult.isAllowed) {
                notificationLogRepository.insert(
                    NotificationLogEntity(
                        packageName = packageName,
                        appLabel = appLabel,
                        title = title,
                        body = text,
                        timestamp = timestamp,
                        parseStatus = ParseStatus.UNKNOWN.name,
                        category = filterResult.category.name,
                        isFiltered = true,
                        filteredReason = filterResult.ignoredReason,
                        lineSenderType = filterResult.lineSenderType,
                        parseVersion = ParserRegistry.PARSE_VERSION,
                        lastParsedAt = now,
                    )
                )
                Log.d(TAG, "▶ FILTERED pkg=$packageName reason=${filterResult.ignoredReason}")
                return@launch
            }

            val result = parserRegistry.parse(packageName, title, text)
            Log.d(TAG, "▶ PARSED status=${result.status} parser=${result.parserName} amount=${result.event?.amount}")

            // Duplicate detection: only when amount was successfully extracted
            val dupResult = if (result.event?.amount != null) {
                duplicateDetector.check(result.event.amount, packageName)
            } else null

            val isDuplicate = dupResult?.isDuplicate == true
            val effectiveStatus = if (isDuplicate) ParseStatus.DUPLICATE else result.status

            val effectiveTrace = if (isDuplicate && dupResult != null) {
                buildString {
                    if (!result.parseTrace.isNullOrBlank()) {
                        appendLine(result.parseTrace)
                        appendLine()
                    }
                    appendLine("[重複偵測]")
                    append(dupResult.reason ?: "重複通知")
                    if (dupResult.matchedAppLabel != null) {
                        append("  (原始：${dupResult.matchedAppLabel})")
                    }
                }.trimEnd()
            } else result.parseTrace

            if (isDuplicate) {
                Log.d(TAG, "▶ DUPLICATE detected amount=${result.event?.amount} reason=${dupResult?.reason}")
            }

            notificationLogRepository.insert(
                NotificationLogEntity(
                    packageName = packageName,
                    appLabel = appLabel,
                    title = title,
                    body = text,
                    timestamp = timestamp,
                    parsedAmount = result.event?.amount,
                    parserName = result.parserName.takeIf { it != "none" },
                    confidence = result.event?.confidence,
                    parseStatus = effectiveStatus.name,
                    category = filterResult.category.name,
                    isFiltered = false,
                    parseTrace = effectiveTrace,
                    lineSenderType = filterResult.lineSenderType,
                    parseVersion = ParserRegistry.PARSE_VERSION,
                    lastParsedAt = now,
                )
            )

            if (effectiveStatus.shouldCreatePendingEvent && result.event?.amount != null) {
                Log.d(TAG, "▶ INSERTING pending event amount=${result.event.amount} status=${effectiveStatus}")
                pendingEventRepository.insertFromParsed(result.event)
            } else {
                Log.d(TAG, "▶ SKIPPED pending event status=${effectiveStatus} isDuplicate=$isDuplicate hasAmount=${result.event?.amount != null}")
            }
        }
    }

    private fun resolveAppLabel(packageName: String): String = try {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, 0)
        ).toString()
    } catch (_: Exception) { packageName }

    companion object {
        private const val TAG = "MoneyNLS"

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected
    }
}
