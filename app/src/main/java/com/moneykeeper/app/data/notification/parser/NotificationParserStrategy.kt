package com.moneykeeper.app.data.notification.parser

import com.moneykeeper.app.data.notification.ParseStatus
import com.moneykeeper.app.domain.model.ParsedEvent

interface NotificationParserStrategy {
    val name: String get() = this::class.simpleName ?: "UnknownParser"

    /**
     * Status to record when parse() returns null but canHandle() was true.
     * Override in TransferParser; all expense parsers use PARTIAL_PARSE (amount not found).
     */
    val intentOnNull: ParseStatus get() = ParseStatus.PARTIAL_PARSE

    fun canHandle(packageName: String, title: String, text: String): Boolean
    fun parse(packageName: String, title: String, text: String): ParsedEvent?
}
