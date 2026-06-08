package com.moneykeeper.app.data.notification

import com.moneykeeper.app.data.notification.parser.CathayParser
import com.moneykeeper.app.data.notification.parser.CTBCParser
import com.moneykeeper.app.data.notification.parser.GenericBankParser
import com.moneykeeper.app.data.notification.parser.IncomeParser
import com.moneykeeper.app.data.notification.parser.LineBankParser
import com.moneykeeper.app.data.notification.parser.LinePayParser
import com.moneykeeper.app.data.notification.parser.NotificationParserStrategy
import com.moneykeeper.app.data.notification.parser.TransferParser
import com.moneykeeper.app.domain.model.ParsedEvent
import javax.inject.Inject
import javax.inject.Singleton

data class ParseResult(
    val event: ParsedEvent?,
    val parserName: String,
    val status: ParseStatus,
    val parseTrace: String = "",
    val extractedKeywords: List<String> = emptyList(),
    val relevanceScore: Int = 0,
    val extractionScore: Int = 0,
    val contextScore: Int = 0,
) {
    val totalScore: Int get() = (relevanceScore + extractionScore + contextScore).coerceIn(0, 100)
}

@Singleton
class ParserRegistry @Inject constructor(
    private val sourceRegistry: NotificationSourceRegistry,
    private val userPatternEngine: UserPatternEngine,
) {

    private val parsers: List<NotificationParserStrategy> = listOf(
        IncomeParser(),        // first — explicit income keywords win before transfer detection
        TransferParser(),      // second — intercepts outgoing transfers
        LinePayParser(),
        LineBankParser(),
        CathayParser(),
        CTBCParser(),
        GenericBankParser(),   // last — broad expense fallback
    )

    fun parse(packageName: String, title: String, text: String): ParseResult {
        val combined = "$title $text"
        val isWhitelisted = sourceRegistry.isWhitelisted(packageName) ||
                            packageName == "jp.naver.line.android"
        val keywords = extractKeywords(combined)
        val trace = StringBuilder()

        for (parser in parsers) {
            if (!parser.canHandle(packageName, title, text)) {
                trace.appendLine("✗ ${parser.name}: canHandle=false")
                continue
            }

            val event = parser.parse(packageName, title, text)
            val isGeneric = parser is GenericBankParser

            return when {
                // Parser produced an event with amount
                event != null -> {
                    val amountDetail = AmountPatterns.parseAmountWithDetail(combined)
                    val merchantDetail = MerchantExtractor.extractWithDetail(combined)
                    val breakdown = buildFullBreakdown(
                        combined, event, isWhitelisted, !isGeneric, amountDetail, merchantDetail,
                    )
                    val updatedEvent = event.copy(confidence = breakdown.normalized)
                    val status = statusFromScore(breakdown.score, hasAmount = true)

                    trace.appendLine("✓ ${parser.name}: amount=${event.amount}, merchant=${event.merchant}, type=${event.transactionType}")
                    trace.appendLine(breakdown.format())
                    appendKeywordTrace(trace, keywords)
                    appendUserPatternTrace(trace, combined)
                    trace.append("→ $status (${breakdown.score}/100)")

                    ParseResult(
                        event = updatedEvent,
                        parserName = parser.name,
                        status = status,
                        parseTrace = trace.toString().trimEnd(),
                        extractedKeywords = keywords,
                        relevanceScore = breakdown.relevanceScore,
                        extractionScore = breakdown.extractionScore,
                        contextScore = breakdown.contextScore,
                    )
                }

                // TransferParser claims this is a transfer (no expense event expected)
                parser is TransferParser -> {
                    appendUserPatternTrace(trace, combined)
                    trace.append("✓ ${parser.name}: claimed → TRANSFER")
                    ParseResult(
                        event = null,
                        parserName = parser.name,
                        status = ParseStatus.TRANSFER,
                        parseTrace = trace.toString().trimEnd(),
                        extractedKeywords = keywords,
                    )
                }

                // Parser claimed but couldn't extract amount — try user-pattern rescue
                else -> {
                    trace.appendLine("✓ ${parser.name}: claimed, amount=null (no built-in match)")
                    val userAmount = userPatternEngine.tryMatchAmount(combined)
                    val userMerchant = userPatternEngine.tryMatchMerchant(combined)

                    if (userAmount != null) {
                        val amount = userAmount.extractedValue?.replace(",", "")?.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val rescuedEvent = ParsedEvent(
                                amount = amount,
                                merchant = userMerchant?.extractedValue,
                                categoryId = null,
                                confidence = 0.4f,
                                rawSource = combined,
                                sourcePackage = packageName,
                            )
                            val breakdown = buildFullBreakdown(
                                combined, rescuedEvent, isWhitelisted, !isGeneric,
                                amountDetail = null, merchantDetail = null,
                                amountLabelOverride = "用戶 Pattern: ${userAmount.patternString.take(30)}",
                                amountTextOverride = userAmount.matchedText,
                            )
                            val status = statusFromScore(breakdown.score, hasAmount = true)

                            appendUserPatternTrace(trace, combined)
                            trace.appendLine(breakdown.format())
                            appendKeywordTrace(trace, keywords)
                            trace.append("→ $status (${breakdown.score}/100) — 用戶 Pattern 救援")

                            return ParseResult(
                                event = rescuedEvent.copy(confidence = breakdown.normalized),
                                parserName = "${parser.name}+UserPattern",
                                status = status,
                                parseTrace = trace.toString().trimEnd(),
                                extractedKeywords = keywords,
                                relevanceScore = breakdown.relevanceScore,
                                extractionScore = breakdown.extractionScore,
                                contextScore = breakdown.contextScore,
                            )
                        }
                    }

                    // No rescue available → PARTIAL_PARSE
                    val baseBreakdown = buildRelevanceBreakdown(combined, isWhitelisted, !isGeneric)
                    trace.appendLine(baseBreakdown.format())
                    appendKeywordTrace(trace, keywords)
                    appendUserPatternTrace(trace, combined)
                    trace.append("→ PARTIAL_PARSE (${baseBreakdown.score}/100) — 金額未解析")

                    ParseResult(
                        event = null,
                        parserName = parser.name,
                        status = ParseStatus.PARTIAL_PARSE,
                        parseTrace = trace.toString().trimEnd(),
                        extractedKeywords = keywords,
                        relevanceScore = baseBreakdown.relevanceScore,
                        contextScore = baseBreakdown.contextScore,
                    )
                }
            }
        }

        trace.append("— no parser matched → UNKNOWN")
        return ParseResult(
            event = null,
            parserName = "none",
            status = ParseStatus.UNKNOWN,
            parseTrace = trace.toString().trimEnd(),
            extractedKeywords = keywords,
        )
    }

    // ---------------------------------------------------------------------------
    // Score computation
    // ---------------------------------------------------------------------------

    private fun buildFullBreakdown(
        combined: String,
        event: ParsedEvent,
        isWhitelisted: Boolean,
        isSpecific: Boolean,
        amountDetail: AmountPatterns.AmountMatchResult?,
        merchantDetail: MerchantExtractor.MerchantMatchResult?,
        amountLabelOverride: String? = null,
        amountTextOverride: String? = null,
    ): ConfidenceBreakdown {
        val signals = mutableListOf<ConfidenceSignal>()

        // RELEVANCE
        if (isWhitelisted) signals.add(ConfidenceSignal("白名單 App", 25, SignalCategory.RELEVANCE))
        addKeywordSignals(signals, combined)

        // EXTRACTION
        if (event.amount != null) {
            val label = amountLabelOverride
                ?: amountDetail?.let { "金額: ${it.patternName}" }
                ?: "金額擷取"
            val matchText = amountTextOverride
                ?: amountDetail?.matchedText
                ?: "NT$${event.amount}"
            signals.add(ConfidenceSignal(label, 30, SignalCategory.EXTRACTION, matchText))
        }
        if (event.merchant != null) {
            val label = merchantDetail?.let { "商家: ${it.patternName}" } ?: "商家擷取"
            signals.add(ConfidenceSignal(label, 8, SignalCategory.EXTRACTION,
                merchantDetail?.matchedText ?: event.merchant))
        }

        // CONTEXT
        if (isSpecific) signals.add(ConfidenceSignal("專屬解析器", 15, SignalCategory.CONTEXT))
        if (!isWhitelisted) signals.add(ConfidenceSignal("未知 App", -5, SignalCategory.CONTEXT))

        return ConfidenceBreakdown(signals)
    }

    private fun buildRelevanceBreakdown(
        combined: String,
        isWhitelisted: Boolean,
        isSpecific: Boolean,
    ): ConfidenceBreakdown {
        val signals = mutableListOf<ConfidenceSignal>()
        if (isWhitelisted) signals.add(ConfidenceSignal("白名單 App", 25, SignalCategory.RELEVANCE))
        addKeywordSignals(signals, combined)
        if (isSpecific) signals.add(ConfidenceSignal("專屬解析器", 15, SignalCategory.CONTEXT))
        if (!isWhitelisted) signals.add(ConfidenceSignal("未知 App", -5, SignalCategory.CONTEXT))
        return ConfidenceBreakdown(signals)
    }

    private fun addKeywordSignals(signals: MutableList<ConfidenceSignal>, combined: String) {
        val matchedStrong = STRONG_KEYWORDS.filter { combined.contains(it) }
        val matchedWeak = WEAK_KEYWORDS.filter { combined.contains(it) }
        when {
            matchedStrong.isNotEmpty() -> signals.add(
                ConfidenceSignal(
                    "強金融關鍵字: ${matchedStrong.joinToString(", ")}",
                    15, SignalCategory.RELEVANCE,
                    matchedStrong.joinToString("/"),
                )
            )
            matchedWeak.isNotEmpty() -> signals.add(
                ConfidenceSignal(
                    "弱金融關鍵字: ${matchedWeak.joinToString(", ")}",
                    8, SignalCategory.RELEVANCE,
                    matchedWeak.joinToString("/"),
                )
            )
        }
    }

    private fun statusFromScore(score: Int, hasAmount: Boolean): ParseStatus {
        if (!hasAmount) return ParseStatus.PARTIAL_PARSE
        return when {
            score >= 60 -> ParseStatus.HIGH_CONFIDENCE
            score >= 40 -> ParseStatus.MEDIUM_CONFIDENCE
            score >= 25 -> ParseStatus.LOW_CONFIDENCE
            else        -> ParseStatus.PARTIAL_PARSE
        }
    }

    private fun appendKeywordTrace(trace: StringBuilder, keywords: List<String>) {
        if (keywords.isNotEmpty()) trace.appendLine("Keywords: ${keywords.joinToString(", ")}")
    }

    private fun appendUserPatternTrace(trace: StringBuilder, combined: String) {
        if (!userPatternEngine.hasPatterns) return
        val attempts = userPatternEngine.tryAll(combined)
        if (attempts.isEmpty()) return
        trace.appendLine("[用戶自定義 Patterns]")
        attempts.forEach { attempt ->
            val icon = if (attempt.matched) "✓" else "✗"
            val matchInfo = attempt.matchedText?.let { "  →«$it»" } ?: ""
            trace.appendLine("  $icon ${attempt.entity.patternType}: ${attempt.entity.patternString.take(45)}$matchInfo")
        }
    }

    private fun extractKeywords(combined: String): List<String> =
        (STRONG_KEYWORDS + WEAK_KEYWORDS + TRANSFER_KEYWORDS)
            .filter { combined.contains(it) }
            .distinct()

    companion object {
        const val PARSE_VERSION = 4   // bump: income detection via IncomeParser

        private val STRONG_KEYWORDS = listOf(
            // Expense
            "消費", "刷卡", "扣款", "付款",
            // Income
            "入帳", "薪資", "退款", "股利", "配息", "收款", "利息",
        )
        private val WEAK_KEYWORDS = listOf(
            "交易", "帳單", "餘額", "ATM", "NT$",
            "存入", "匯入",
        )
        private val TRANSFER_KEYWORDS = listOf("轉帳", "轉入", "轉出", "代扣", "代繳")
    }
}
