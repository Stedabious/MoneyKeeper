package com.moneykeeper.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regex_patterns")
data class RegexPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patternString: String,           // the regex (e.g. "NT\$\s*([\d,]+)")
    val patternType: String,             // PatternType.name
    val sourceBody: String,              // notification body it was built from
    val sourcePackageName: String,
    val sourceAppLabel: String,
    val testPassed: Boolean?,            // did patternString match sourceBody at save time?
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
