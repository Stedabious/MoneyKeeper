package com.moneykeeper.app.domain.model

/**
 * Canonical source type for all data events in the system.
 *
 * Naming convention: debug sources MUST start with "DEBUG_" so SQL can filter
 * them uniformly with `eventSource NOT LIKE 'DEBUG_%'`.
 *
 * countedInStats: whether this source contributes to monthly totals and statistics.
 */
enum class EventSource(
    val isDebug: Boolean,
    val countedInStats: Boolean,
    val displayLabel: String,
) {
    REAL_NOTIFICATION(isDebug = false, countedInStats = true,  displayLabel = "通知"),
    DEBUG_NOTIFICATION(isDebug = true,  countedInStats = false, displayLabel = "[測試] 通知"),
    MANUAL_INPUT(      isDebug = false, countedInStats = true,  displayLabel = "手動"),
    DEBUG_MANUAL(      isDebug = true,  countedInStats = false, displayLabel = "[測試] 手動"),
    INVOICE_IMPORT(    isDebug = false, countedInStats = true,  displayLabel = "發票匯入"),
    ;

    companion object {
        fun fromName(name: String, fallback: EventSource = REAL_NOTIFICATION): EventSource =
            values().find { it.name == name } ?: fallback

        fun debugSources(): List<EventSource> = values().filter { it.isDebug }
        fun realSources(): List<EventSource> = values().filter { !it.isDebug }
    }
}
