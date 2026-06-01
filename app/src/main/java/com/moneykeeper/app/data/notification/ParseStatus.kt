package com.moneykeeper.app.data.notification

enum class ParseStatus(val displayLabel: String) {
    HIGH_CONFIDENCE("高信心"),
    MEDIUM_CONFIDENCE("中信心"),
    LOW_CONFIDENCE("低信心"),
    PARTIAL_PARSE("部分解析"),
    TRANSFER("轉帳"),
    UNKNOWN("未識別"),
    IGNORED("已過濾"),
    DUPLICATE("重複通知");

    /** True when this status warrants creating a PendingEvent (caller must also verify amount != null). */
    val shouldCreatePendingEvent: Boolean
        get() = this == HIGH_CONFIDENCE || this == MEDIUM_CONFIDENCE || this == LOW_CONFIDENCE

    companion object {
        fun fromName(name: String): ParseStatus = entries.find { it.name == name }
            ?: when (name) {
                "PARSED_EXPENSE"  -> MEDIUM_CONFIDENCE
                "PARSED_TRANSFER" -> TRANSFER
                "BELOW_THRESHOLD" -> PARTIAL_PARSE
                "UNPARSED"        -> UNKNOWN
                else              -> UNKNOWN
            }
    }
}
