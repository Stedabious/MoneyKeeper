package com.moneykeeper.app.data.notification

enum class NotificationCategory(val displayLabel: String) {
    FINANCIAL("金融"),
    SOCIAL("社交"),
    ADVERTISEMENT("廣告"),
    SYSTEM("系統"),
    UNKNOWN("未知");

    companion object {
        fun fromName(name: String): NotificationCategory =
            entries.find { it.name == name } ?: UNKNOWN
    }
}
