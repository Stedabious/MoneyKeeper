package com.moneykeeper.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.moneykeeper.app.data.database.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {

    @Query("SELECT * FROM notification_log ORDER BY createdAt DESC LIMIT 200")
    fun getAll(): Flow<List<NotificationLogEntity>>

    @Query("""
        SELECT * FROM notification_log
        WHERE isFiltered = 0 AND eventSource NOT LIKE 'DEBUG_%'
        ORDER BY createdAt DESC LIMIT 200
    """)
    fun getFinancial(): Flow<List<NotificationLogEntity>>

    @Query("""
        SELECT * FROM notification_log
        WHERE isFiltered = 1
        ORDER BY createdAt DESC LIMIT 200
    """)
    fun getIgnored(): Flow<List<NotificationLogEntity>>

    @Query("""
        SELECT * FROM notification_log
        WHERE eventSource LIKE 'DEBUG_%'
        ORDER BY createdAt DESC LIMIT 200
    """)
    fun getDebug(): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_log WHERE eventSource NOT LIKE 'DEBUG_%' ORDER BY createdAt DESC LIMIT 200")
    fun getRecentReal(): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_log WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NotificationLogEntity?

    // Reparse queries — return plain lists (not Flow), used in background batch operations
    @Query("""
        SELECT * FROM notification_log
        WHERE parseStatus IN (
            'UNKNOWN', 'PARTIAL_PARSE', 'LOW_CONFIDENCE',
            'UNPARSED', 'BELOW_THRESHOLD'
        )
        ORDER BY createdAt DESC
    """)
    suspend fun getFailedParsed(): List<NotificationLogEntity>

    @Query("SELECT * FROM notification_log WHERE parseVersion < :version ORDER BY createdAt DESC")
    suspend fun getOlderThanVersion(version: Int): List<NotificationLogEntity>

    // Duplicate detection — find recent non-duplicate logs with the same parsed amount
    @Query("""
        SELECT * FROM notification_log
        WHERE parsedAmount = :amount
        AND createdAt >= :since
        AND parseStatus != 'DUPLICATE'
        ORDER BY createdAt DESC
        LIMIT 5
    """)
    suspend fun findRecentWithAmount(amount: Double, since: Long): List<NotificationLogEntity>

    @Insert
    suspend fun insert(entity: NotificationLogEntity): Long

    @Update
    suspend fun update(entity: NotificationLogEntity)

    @Query("DELETE FROM notification_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_log WHERE id NOT IN (SELECT id FROM notification_log ORDER BY createdAt DESC LIMIT 500)")
    suspend fun trimToLimit()

    @Query("DELETE FROM notification_log WHERE eventSource LIKE 'DEBUG_%'")
    suspend fun deleteDebugData()

    @Query("DELETE FROM notification_log WHERE isFiltered = 1")
    suspend fun deleteIgnored()

    @Query("DELETE FROM notification_log")
    suspend fun clearAll()
}
