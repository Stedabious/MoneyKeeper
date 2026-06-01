package com.moneykeeper.app.data.notification.parser

import com.moneykeeper.app.data.notification.AmountPatterns
import com.moneykeeper.app.data.notification.MerchantExtractor
import com.moneykeeper.app.domain.model.ParsedEvent

class CTBCParser : NotificationParserStrategy {

    private val packages = setOf(
        "com.ctbc.android",
        "com.ctbcbank.android",
        "ctbc.tpay.android",
    )

    override fun canHandle(packageName: String, title: String, text: String): Boolean {
        if (packageName in packages) return true
        if (packageName.startsWith("com.ctbc") || packageName.startsWith("ctbc.")) return true
        val combined = "$title $text"
        return combined.contains("中國信託") &&
            (combined.contains("消費") || combined.contains("刷卡") || combined.contains("扣款"))
    }

    override fun parse(packageName: String, title: String, text: String): ParsedEvent? {
        val combined = "$title $text"
        val (amount, confidence) = AmountPatterns.parseAmount(combined) ?: return null
        return ParsedEvent(
            amount = amount,
            merchant = MerchantExtractor.extract(combined),
            categoryId = null,
            confidence = (confidence + 0.15f).coerceAtMost(1.0f),
            rawSource = combined,
            sourcePackage = packageName,
        )
    }
}
