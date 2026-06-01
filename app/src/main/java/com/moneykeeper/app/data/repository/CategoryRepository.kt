package com.moneykeeper.app.data.repository

import com.moneykeeper.app.data.database.dao.CategoryDao
import com.moneykeeper.app.data.database.entity.toDomain
import com.moneykeeper.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val dao: CategoryDao) {

    fun getAll(): Flow<List<Category>> =
        dao.getAll().map { it.map { e -> e.toDomain() } }

    fun getByType(type: String): Flow<List<Category>> =
        dao.getByType(type).map { it.map { e -> e.toDomain() } }

    suspend fun getById(id: Long): Category? = dao.getById(id)?.toDomain()
}
