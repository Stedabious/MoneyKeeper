package com.moneykeeper.app.data.repository

import com.moneykeeper.app.data.database.dao.NotificationLogDao
import com.moneykeeper.app.data.database.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationLogRepository @Inject constructor(private val dao: NotificationLogDao) {

    fun getAll(): Flow<List<NotificationLogEntity>> = dao.getAll()
    fun getFinancial(): Flow<List<NotificationLogEntity>> = dao.getFinancial()
    fun getIgnored(): Flow<List<NotificationLogEntity>> = dao.getIgnored()
    fun getDebug(): Flow<List<NotificationLogEntity>> = dao.getDebug()
    fun getRecentReal(): Flow<List<NotificationLogEntity>> = dao.getRecentReal()

    suspend fun getById(id: Long): NotificationLogEntity? = dao.getById(id)

    suspend fun getFailedParsed(): List<NotificationLogEntity> = dao.getFailedParsed()

    suspend fun getOlderThanVersion(version: Int): List<NotificationLogEntity> = dao.getOlderThanVersion(version)

    suspend fun findRecentWithAmount(amount: Double, since: Long): List<NotificationLogEntity> =
        dao.findRecentWithAmount(amount, since)

    suspend fun insert(entity: NotificationLogEntity) {
        dao.insert(entity)
        dao.trimToLimit()
    }

    suspend fun update(entity: NotificationLogEntity) = dao.update(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearDebugData() = dao.deleteDebugData()

    suspend fun clearIgnored() = dao.deleteIgnored()

    suspend fun clearAll() = dao.clearAll()

    // Legacy alias
    fun getRecent(): Flow<List<NotificationLogEntity>> = dao.getAll()
}
