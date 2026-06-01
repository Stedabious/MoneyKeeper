package com.moneykeeper.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.moneykeeper.app.data.database.entity.PendingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingEventDao {

    @Query("SELECT * FROM pending_events WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPending(): Flow<List<PendingEventEntity>>

    @Query("SELECT * FROM pending_events WHERE status = 'PENDING' AND eventSource NOT LIKE 'DEBUG_%' ORDER BY createdAt DESC")
    fun getPendingReal(): Flow<List<PendingEventEntity>>

    @Query("SELECT COUNT(*) FROM pending_events WHERE status = 'PENDING'")
    fun pendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_events WHERE status = 'PENDING' AND eventSource NOT LIKE 'DEBUG_%'")
    fun pendingRealCount(): Flow<Int>

    @Insert
    suspend fun insert(entity: PendingEventEntity): Long

    @Query("UPDATE pending_events SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM pending_events WHERE eventSource LIKE 'DEBUG_%'")
    suspend fun deleteDebugData()
}
