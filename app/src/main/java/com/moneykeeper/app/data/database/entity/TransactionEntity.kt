package com.moneykeeper.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.moneykeeper.app.domain.model.EventSource
import com.moneykeeper.app.domain.model.Transaction
import com.moneykeeper.app.domain.model.TransactionSource
import com.moneykeeper.app.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val currency: String = "TWD",
    val categoryId: Long?,
    val merchant: String = "",
    val note: String = "",
    val source: String,
    val eventSource: String = EventSource.MANUAL_INPUT.name,
    val transactionType: String = "EXPENSE",
    val transactionDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    currency = currency,
    categoryId = categoryId ?: 8L,
    merchant = merchant,
    note = note,
    source = TransactionSource.valueOf(source),
    eventSource = EventSource.fromName(eventSource, EventSource.MANUAL_INPUT),
    transactionType = runCatching { TransactionType.valueOf(transactionType) }.getOrDefault(TransactionType.EXPENSE),
    transactionDate = transactionDate,
    createdAt = createdAt,
    deletedAt = deletedAt,
)

fun Transaction.toEntity(): TransactionEntity {
    val now = System.currentTimeMillis()
    return TransactionEntity(
        id = id,
        amount = amount,
        currency = currency,
        categoryId = categoryId,
        merchant = merchant,
        note = note,
        source = source.name,
        eventSource = eventSource.name,
        transactionType = transactionType.name,
        transactionDate = transactionDate,
        createdAt = if (id == 0L) now else createdAt,
        updatedAt = now,
        deletedAt = deletedAt,
    )
}
