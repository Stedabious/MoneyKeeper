package com.moneykeeper.app.data.notification.parser

import com.moneykeeper.app.data.notification.AmountPatterns
import com.moneykeeper.app.domain.model.ParsedEvent

class GenericAmountParser : NotificationParserStrategy {

    private val financialKeywords = listOf("消費", "刷卡", "扣款", "付款", "交易", "帳單")

    override fun canHandle(packageName: String, title: String, text: String): Boolean =
        financialKeywords.any { "$title $text".contains(it) }

    override fun parse(packageName: String, title: String, text: String): ParsedEvent? {
        val combined = "$title $text"
        val (amount, confidence) = AmountPatterns.parseAmount(combined) ?: return null
        if (confidence < 0.5f) return null
        return ParsedEvent(
            amount = amount,
            merchant = AmountPatterns.parseMerchant(combined),
            categoryId = null,
            confidence = confidence * 0.8f,  // penalize unknown source
            rawSource = combined,
            sourcePackage = packageName,
        )
    }
}
