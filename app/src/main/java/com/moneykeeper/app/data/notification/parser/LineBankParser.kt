package com.moneykeeper.app.data.notification.parser

import com.moneykeeper.app.data.notification.AmountPatterns
import com.moneykeeper.app.data.notification.MerchantExtractor
import com.moneykeeper.app.domain.model.ParsedEvent

class LineBankParser : NotificationParserStrategy {

    override fun canHandle(packageName: String, title: String, text: String): Boolean =
        packageName == "com.linebank.tw" ||
            (packageName.contains("linebank") && (text.contains("消費") || text.contains("刷卡")))

    override fun parse(packageName: String, title: String, text: String): ParsedEvent? {
        val combined = "$title $text"
        val (amount, confidence) = AmountPatterns.parseAmount(combined) ?: return null
        return ParsedEvent(
            amount = amount,
            merchant = MerchantExtractor.extract(combined),
            categoryId = null,
            confidence = confidence,
            rawSource = combined,
            sourcePackage = packageName,
        )
    }
}
