package com.moneykeeper.app.data.notification

data class FilterResult(
    val isAllowed: Boolean,
    val category: NotificationCategory,
    val ignoredReason: String? = null,
    val lineSenderType: String? = null,
)
