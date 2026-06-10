package com.moneykeeper.app.data.notification.parser

import com.moneykeeper.app.data.notification.AmountPatterns
import com.moneykeeper.app.data.notification.MerchantExtractor
import com.moneykeeper.app.domain.model.ParsedEvent

class LinePayParser : NotificationParserStrategy {

    override fun canHandle(packageName: String, title: String, text: String): Boolean =
        packageName == "jp.naver.line.android" &&
            title.contains("LINE Pay")

    override fun parse(packageName: String, title: String, text: String): ParsedEvent? {
        val combined = "$title $text"
        val (amount, confidence) = AmountPatterns.parseAmount(combined) ?: return null
        return ParsedEvent(
            amount = amount,
            merchant = MerchantExtractor.extract(combined),
            categoryId = null,
            confidence = (confidence + 0.1f).coerceAtMost(1.0f),
            rawSource = combined,
            sourcePackage = packageName,
        )
    }
}
