package com.moneykeeper.app.data.notification.parser

import com.moneykeeper.app.data.notification.ParseStatus
import com.moneykeeper.app.domain.model.ParsedEvent

class TransferParser : NotificationParserStrategy {

    override val intentOnNull: ParseStatus = ParseStatus.TRANSFER

    override fun canHandle(packageName: String, title: String, text: String): Boolean {
        val combined = "$title $text"
        return TRANSFER_PHRASES.any { combined.contains(it) }
    }

    override fun parse(packageName: String, title: String, text: String): ParsedEvent? = null

    companion object {
        val TRANSFER_PHRASES = listOf(
            // Transfer completion
            "轉帳成功", "轉出成功", "轉入成功", "轉入通知", "轉出通知",
            "跨行轉帳", "ATM轉出", "ATM轉入", "ATM 轉出", "ATM 轉入",
            // Salary / payroll
            "薪資入帳", "薪資匯入", "薪資轉入",
            // Refund
            "退款入帳", "退款成功",
            // Auto-debit / bill payment
            "代扣成功", "代繳成功", "帳款扣除", "定期扣款", "扣繳成功",
            // Incoming credit notifications
            "入帳通知", "出帳通知",
            // Agent collection
            "代收成功",
        )
    }
}
