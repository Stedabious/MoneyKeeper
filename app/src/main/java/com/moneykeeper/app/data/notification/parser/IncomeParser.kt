package com.moneykeeper.app.data.notification.parser

import com.moneykeeper.app.data.notification.AmountPatterns
import com.moneykeeper.app.domain.model.ParsedEvent
import com.moneykeeper.app.domain.model.TransactionType

class IncomeParser : NotificationParserStrategy {

    override val name = "IncomeParser"

    override fun canHandle(packageName: String, title: String, text: String): Boolean {
        val combined = "$title $text"
        return INCOME_KEYWORDS.any { combined.contains(it) }
    }

    override fun parse(packageName: String, title: String, text: String): ParsedEvent? {
        val combined = "$title $text"
        val amountResult = AmountPatterns.parseAmountWithDetail(combined) ?: return null
        return ParsedEvent(
            amount = amountResult.amount,
            merchant = null,
            categoryId = detectCategory(combined),
            confidence = 0.6f,
            rawSource = combined,
            sourcePackage = packageName,
            transactionType = TransactionType.INCOME,
        )
    }

    private fun detectCategory(text: String): Long = when {
        SALARY_KEYWORDS.any  { text.contains(it) } -> 9L   // 薪資
        BONUS_KEYWORDS.any   { text.contains(it) } -> 10L  // 獎金
        INVEST_KEYWORDS.any  { text.contains(it) } -> 11L  // 股票
        PASSIVE_KEYWORDS.any { text.contains(it) } -> 13L  // 被動收入
        else                                        -> 14L  // 其他收入
    }

    companion object {
        val INCOME_KEYWORDS = listOf(
            "入帳", "存入", "匯入", "收款", "收到轉帳", "收到款項",
            "薪資", "薪水", "發薪", "工資",
            "退款", "退費", "退還",
            "現金回饋", "回饋金", "紅利", "紅包",
            "股利", "配息", "股息",
            "利息", "孳息",
            "獎金", "補助", "津貼",
            "轉入成功", "匯款入帳",
        )

        private val SALARY_KEYWORDS  = listOf("薪資", "薪水", "發薪", "工資", "薪俸")
        private val BONUS_KEYWORDS   = listOf("獎金", "紅利", "補助", "津貼")
        private val INVEST_KEYWORDS  = listOf("股利", "配息", "股息", "利息", "孳息")
        private val PASSIVE_KEYWORDS = listOf("退款", "退費", "退還", "現金回饋", "回饋金")
    }
}
