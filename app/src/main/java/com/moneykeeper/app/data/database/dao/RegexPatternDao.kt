package com.moneykeeper.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.moneykeeper.app.data.database.entity.RegexPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexPatternDao {

    @Query("SELECT * FROM regex_patterns ORDER BY createdAt DESC")
    fun getAll(): Flow<List<RegexPatternEntity>>

    @Query("SELECT * FROM regex_patterns WHERE patternType = :type ORDER BY createdAt DESC")
    fun getByType(type: String): Flow<List<RegexPatternEntity>>

    @Insert
    suspend fun insert(entity: RegexPatternEntity): Long

    @Query("DELETE FROM regex_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM regex_patterns")
    suspend fun clearAll()
}
