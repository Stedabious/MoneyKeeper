package com.moneykeeper.app.data.notification

enum class PatternType(val displayLabel: String) {
    AMOUNT("金額擷取"),
    MERCHANT("商家擷取"),
    TRANSFER_KEYWORD("轉帳關鍵字"),
    OTHER("其他"),
    ;

    companion object {
        fun fromName(name: String): PatternType =
            entries.find { it.name == name } ?: OTHER
    }
}
