package com.moneykeeper.app.data.notification

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelevanceFilter @Inject constructor(
    private val registry: NotificationSourceRegistry,
) {
    private val financialKeywords = listOf(
        "消費", "刷卡", "扣款", "付款", "轉帳", "交易", "帳單", "餘額",
        "入帳", "ATM", "NT$", "NTD", "信用卡", "簽帳", "扣繳",
    )

    fun filter(packageName: String, title: String, text: String): FilterResult {
        // LINE requires per-sender analysis — a single LINE package can be bank or social
        if (packageName == "jp.naver.line.android") {
            return filterLine(title, text)
        }

        val source = registry.findSource(packageName)

        return when {
            source != null && source.isWhitelisted ->
                FilterResult(isAllowed = true, category = source.category)

            source != null && !source.isWhitelisted ->
                FilterResult(
                    isAllowed = false,
                    category = source.category,
                    ignoredReason = "非金融 App (${source.category.displayLabel})",
                )

            else -> {
                val combined = "$title $text"
                val matchedKeyword = financialKeywords.firstOrNull { combined.contains(it) }
                if (matchedKeyword != null) {
                    FilterResult(isAllowed = true, category = NotificationCategory.FINANCIAL)
                } else {
                    FilterResult(
                        isAllowed = false,
                        category = NotificationCategory.UNKNOWN,
                        ignoredReason = "無金融關鍵字",
                    )
                }
            }
        }
    }

    private fun filterLine(title: String, text: String): FilterResult {
        val analysis = LineSenderAnalyzer.analyze(title, text)
        return if (analysis.isFinancial) {
            FilterResult(
                isAllowed = true,
                category = NotificationCategory.FINANCIAL,
                lineSenderType = analysis.senderType.name,
            )
        } else {
            FilterResult(
                isAllowed = false,
                category = NotificationCategory.SOCIAL,
                ignoredReason = "LINE 非金融帳號 (${analysis.senderType.displayLabel})",
                lineSenderType = analysis.senderType.name,
            )
        }
    }
}
