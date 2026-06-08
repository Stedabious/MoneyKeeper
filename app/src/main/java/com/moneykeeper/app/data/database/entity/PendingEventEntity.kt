package com.moneykeeper.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moneykeeper.app.domain.model.EventSource
import com.moneykeeper.app.domain.model.PendingEvent
import com.moneykeeper.app.domain.model.PendingEventStatus
import com.moneykeeper.app.domain.model.TransactionType

@Entity(tableName = "pending_events")
data class PendingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double?,
    val currency: String = "TWD",
    val merchant: String?,
    val categoryId: Long?,
    val confidence: Float,
    val rawSource: String,
    val sourcePackage: String?,
    val status: String = "PENDING",
    val eventSource: String = EventSource.REAL_NOTIFICATION.name,
    val transactionType: String = "EXPENSE",
    val eventTime: Long,
    val createdAt: Long,
)

fun PendingEventEntity.toDomain() = PendingEvent(
    id = id,
    amount = amount,
    currency = currency,
    merchant = merchant,
    categoryId = categoryId,
    confidence = confidence,
    rawSource = rawSource,
    sourcePackage = sourcePackage,
    status = PendingEventStatus.valueOf(status),
    eventSource = EventSource.fromName(eventSource),
    transactionType = runCatching { TransactionType.valueOf(transactionType) }.getOrDefault(TransactionType.EXPENSE),
    eventTime = eventTime,
    createdAt = createdAt,
)

fun PendingEvent.toEntity() = PendingEventEntity(
    id = id,
    amount = amount,
    currency = currency,
    merchant = merchant,
    categoryId = categoryId,
    confidence = confidence,
    rawSource = rawSource,
    sourcePackage = sourcePackage,
    status = status.name,
    eventSource = eventSource.name,
    transactionType = transactionType.name,
    eventTime = eventTime,
    createdAt = if (id == 0L) System.currentTimeMillis() else createdAt,
)
