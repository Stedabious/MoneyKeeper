package com.moneykeeper.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_log")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val parsedAmount: Double? = null,
    val parserName: String? = null,
    val confidence: Float? = null,
    val eventSource: String = "REAL_NOTIFICATION",
    val parseStatus: String = "UNPARSED",
    val createdAt: Long = System.currentTimeMillis(),
    // Source filtering (added in migration 6→7)
    val category: String = "UNKNOWN",
    val isFiltered: Boolean = false,
    val filteredReason: String? = null,
    val parseTrace: String? = null,
    // Reparse tracking (added in migration 7→8)
    val parseVersion: Int = 0,
    val lastParsedAt: Long? = null,
    val lineSenderType: String? = null,
)
