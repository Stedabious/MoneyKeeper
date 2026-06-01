package com.moneykeeper.app.data.repository

import com.moneykeeper.app.data.database.dao.TransactionDao
import com.moneykeeper.app.data.database.entity.toDomain
import com.moneykeeper.app.data.database.entity.toEntity
import com.moneykeeper.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val dao: TransactionDao) {

    fun getAll(): Flow<List<Transaction>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    fun getByDateRange(from: Long, to: Long): Flow<List<Transaction>> =
        dao.getByDateRange(from, to).map { list -> list.map { it.toDomain() } }

    fun getRealByDateRange(from: Long, to: Long): Flow<List<Transaction>> =
        dao.getRealByDateRange(from, to).map { list -> list.map { it.toDomain() } }

    fun sumByDateRange(from: Long, to: Long): Flow<Double?> =
        dao.sumByDateRange(from, to)

    fun sumRealByDateRange(from: Long, to: Long): Flow<Double?> =
        dao.sumRealByDateRange(from, to)

    fun sumExpenseRealByDateRange(from: Long, to: Long): Flow<Double?> =
        dao.sumExpenseRealByDateRange(from, to)

    fun sumIncomeRealByDateRange(from: Long, to: Long): Flow<Double?> =
        dao.sumIncomeRealByDateRange(from, to)

    suspend fun getById(id: Long): Transaction? =
        dao.getById(id)?.toDomain()

    suspend fun insert(transaction: Transaction): Long =
        dao.insert(transaction.toEntity())

    suspend fun update(transaction: Transaction) =
        dao.update(transaction.toEntity())

    suspend fun delete(transaction: Transaction) =
        dao.softDelete(transaction.id, System.currentTimeMillis())

    suspend fun restore(id: Long) =
        dao.restore(id)

    fun getTrash(): Flow<List<Transaction>> =
        dao.getTrash().map { list -> list.map { it.toDomain() } }

    fun getTrashCount(): Flow<Int> =
        dao.getTrashCount()

    suspend fun cleanOldTrash(cutoff: Long) =
        dao.cleanOldTrash(cutoff)

    suspend fun emptyTrash() =
        dao.emptyTrash()

    suspend fun clearDebugData() = dao.deleteDebugData()
}
