package com.moneykeeper.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moneykeeper.app.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val icon: String,
    val colorHex: String,
    val isSystem: Boolean = false,
    val sortOrder: Int = 0,
    val categoryType: String = "EXPENSE",
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    icon = icon,
    colorHex = colorHex,
    categoryType = categoryType,
)

fun Category.toEntity(sortOrder: Int = 0) = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    colorHex = colorHex,
    isSystem = true,
    sortOrder = sortOrder,
    categoryType = categoryType,
)
