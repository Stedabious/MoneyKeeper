package com.moneykeeper.app.data.notification.parser

import com.moneykeeper.app.data.notification.AmountPatterns
import com.moneykeeper.app.data.notification.MerchantExtractor
import com.moneykeeper.app.domain.model.ParsedEvent

/**
 * Fallback parser for any bank/payment notification not handled by a specific parser.
 * Must be last in ParserRegistry — it matches broadly on financial keywords.
 * Applies a confidence penalty (×0.8) since the source is unknown.
 */
class GenericBankParser : NotificationParserStrategy {

    override fun canHandle(packageName: String, title: String, text: String): Boolean =
        FINANCIAL_KEYWORDS.any { "$title $text".contains(it) }

    override fun parse(packageName: String, title: String, text: String): ParsedEvent? {
        val combined = "$title $text"
        val (amount, confidence) = AmountPatterns.parseAmount(combined) ?: return null
        return ParsedEvent(
            amount = amount,
            merchant = MerchantExtractor.extract(combined),
            categoryId = null,
            confidence = (confidence * 0.8f).coerceIn(0.1f, 1.0f),
            rawSource = combined,
            sourcePackage = packageName,
        )
    }

    companion object {
        private val FINANCIAL_KEYWORDS = listOf("消費", "刷卡", "扣款", "付款", "交易", "帳單")
    }
}
