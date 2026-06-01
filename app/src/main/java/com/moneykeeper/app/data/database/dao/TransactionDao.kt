package com.moneykeeper.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.moneykeeper.app.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY transactionDate DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionDate BETWEEN :from AND :to AND deletedAt IS NULL ORDER BY transactionDate DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionDate BETWEEN :from AND :to AND eventSource NOT LIKE 'DEBUG_%' AND deletedAt IS NULL ORDER BY transactionDate DESC")
    fun getRealByDateRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionDate BETWEEN :from AND :to AND deletedAt IS NULL")
    fun sumByDateRange(from: Long, to: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionDate BETWEEN :from AND :to AND eventSource NOT LIKE 'DEBUG_%' AND deletedAt IS NULL")
    fun sumRealByDateRange(from: Long, to: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionDate BETWEEN :from AND :to AND eventSource NOT LIKE 'DEBUG_%' AND deletedAt IS NULL AND transactionType = 'EXPENSE'")
    fun sumExpenseRealByDateRange(from: Long, to: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionDate BETWEEN :from AND :to AND eventSource NOT LIKE 'DEBUG_%' AND deletedAt IS NULL AND transactionType = 'INCOME'")
    fun sumIncomeRealByDateRange(from: Long, to: Long): Flow<Double?>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun deletePermanently(entity: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("UPDATE transactions SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("SELECT * FROM transactions WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrash(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE deletedAt IS NOT NULL")
    fun getTrashCount(): Flow<Int>

    @Query("DELETE FROM transactions WHERE deletedAt IS NOT NULL AND deletedAt <= :cutoff")
    suspend fun cleanOldTrash(cutoff: Long)

    @Query("DELETE FROM transactions WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    @Query("DELETE FROM transactions WHERE eventSource LIKE 'DEBUG_%'")
    suspend fun deleteDebugData()
}
