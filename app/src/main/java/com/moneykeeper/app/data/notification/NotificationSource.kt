package com.moneykeeper.app.data.notification

data class NotificationSource(
    val packageName: String,
    val appName: String,
    val category: NotificationCategory,
    val isWhitelisted: Boolean,
)
