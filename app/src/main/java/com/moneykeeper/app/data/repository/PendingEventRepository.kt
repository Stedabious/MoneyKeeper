package com.moneykeeper.app.data.repository

import com.moneykeeper.app.data.database.dao.PendingEventDao
import com.moneykeeper.app.data.database.entity.toDomain
import com.moneykeeper.app.data.database.entity.toEntity
import com.moneykeeper.app.domain.model.ParsedEvent
import com.moneykeeper.app.domain.model.PendingEvent
import com.moneykeeper.app.domain.model.PendingEventStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingEventRepository @Inject constructor(private val dao: PendingEventDao) {

    fun getPending(): Flow<List<PendingEvent>> =
        dao.getPendingReal().map { it.map { e -> e.toDomain() } }

    fun pendingCount(): Flow<Int> = dao.pendingRealCount()

    suspend fun insertFromParsed(parsed: ParsedEvent): Long {
        val event = PendingEvent(
            amount = parsed.amount,
            currency = parsed.currency,
            merchant = parsed.merchant,
            categoryId = parsed.categoryId,
            confidence = parsed.confidence,
            rawSource = parsed.rawSource,
            sourcePackage = parsed.sourcePackage,
            status = PendingEventStatus.PENDING,
            eventSource = parsed.eventSource,
            eventTime = parsed.timestamp,
        )
        return dao.insert(event.toEntity())
    }

    suspend fun confirm(id: Long) = dao.updateStatus(id, PendingEventStatus.CONFIRMED.name)

    suspend fun reject(id: Long) = dao.updateStatus(id, PendingEventStatus.REJECTED.name)

    suspend fun clearDebugData() = dao.deleteDebugData()
}
