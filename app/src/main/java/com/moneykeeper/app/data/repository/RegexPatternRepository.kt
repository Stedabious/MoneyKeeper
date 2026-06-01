package com.moneykeeper.app.data.repository

import com.moneykeeper.app.data.database.dao.RegexPatternDao
import com.moneykeeper.app.data.database.entity.RegexPatternEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegexPatternRepository @Inject constructor(private val dao: RegexPatternDao) {

    fun getAll(): Flow<List<RegexPatternEntity>> = dao.getAll()

    fun getByType(type: String): Flow<List<RegexPatternEntity>> = dao.getByType(type)

    suspend fun insert(entity: RegexPatternEntity): Long = dao.insert(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
