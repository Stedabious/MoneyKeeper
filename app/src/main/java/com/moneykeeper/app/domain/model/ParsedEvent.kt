package com.moneykeeper.app.domain.model

data class ParsedEvent(
    val amount: Double?,
    val currency: String = "TWD",
    val merchant: String?,
    val categoryId: Long?,
    val confidence: Float,
    val rawSource: String,
    val sourcePackage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventSource: EventSource = EventSource.REAL_NOTIFICATION,
    val transactionType: TransactionType = TransactionType.EXPENSE,
)
