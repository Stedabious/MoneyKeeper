package com.moneykeeper.app.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val currency: String = "TWD",
    val categoryId: Long,
    val merchant: String = "",
    val note: String = "",
    val source: TransactionSource,
    val eventSource: EventSource = EventSource.MANUAL_INPUT,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val transactionDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
)

enum class TransactionSource { MANUAL, NOTIFICATION }

enum class TransactionType { EXPENSE, INCOME }
