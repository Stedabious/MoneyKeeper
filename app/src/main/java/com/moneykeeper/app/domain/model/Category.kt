package com.moneykeeper.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val categoryType: String = "EXPENSE",
)

val DefaultExpenseCategories = listOf(
    Category(1,  "飲食",   "restaurant",    "#FF6B6B", "EXPENSE"),
    Category(2,  "交通",   "directions_car","#4ECDC4", "EXPENSE"),
    Category(3,  "購物",   "shopping_bag",  "#45B7D1", "EXPENSE"),
    Category(4,  "娛樂",   "sports_esports","#96CEB4", "EXPENSE"),
    Category(5,  "醫療",   "local_hospital","#FFEAA7", "EXPENSE"),
    Category(6,  "住宿",   "home",          "#DDA0DD", "EXPENSE"),
    Category(7,  "教育",   "school",        "#98D8C8", "EXPENSE"),
    Category(8,  "其他",   "more_horiz",    "#B0B0B0", "EXPENSE"),
)

val DefaultIncomeCategories = listOf(
    Category(9,  "薪資",   "payments",      "#4CAF50", "INCOME"),
    Category(10, "獎金",   "star",          "#FFC107", "INCOME"),
    Category(11, "股票",   "trending_up",   "#2196F3", "INCOME"),
    Category(12, "基金",   "account_balance","#9C27B0","INCOME"),
    Category(13, "被動收入","autorenew",     "#00BCD4", "INCOME"),
    Category(14, "其他收入","more_horiz",   "#78909C", "INCOME"),
)

val DefaultCategories = DefaultExpenseCategories + DefaultIncomeCategories
