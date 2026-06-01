package com.moneykeeper.app.domain.model

data class PendingEvent(
    val id: Long = 0,
    val amount: Double?,
    val currency: String = "TWD",
    val merchant: String?,
    val categoryId: Long?,
    val confidence: Float,
    val rawSource: String,
    val sourcePackage: String?,
    val status: PendingEventStatus,
    val eventSource: EventSource = EventSource.REAL_NOTIFICATION,
    val eventTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class PendingEventStatus { PENDING, CONFIRMED, REJECTED }
