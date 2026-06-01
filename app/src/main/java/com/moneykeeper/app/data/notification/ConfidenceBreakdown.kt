package com.moneykeeper.app.data.notification

enum class SignalCategory { RELEVANCE, EXTRACTION, CONTEXT }

data class ConfidenceSignal(
    val label: String,
    val points: Int,
    val category: SignalCategory = SignalCategory.RELEVANCE,
    /** The actual text fragment that triggered this signal (shown in trace). */
    val matchedText: String? = null,
)

/**
 * Three-dimension confidence breakdown.
 *
 *  RELEVANCE  — is this notification financially relevant? (package whitelist, keywords)
 *  EXTRACTION — how much data was extracted?              (amount, merchant)
 *  CONTEXT    — quality of the parsing context?           (parser specificity, package familiarity)
 */
data class ConfidenceBreakdown(val signals: List<ConfidenceSignal>) {
    val relevanceScore: Int get() = signals
        .filter { it.category == SignalCategory.RELEVANCE }.sumOf { it.points }.coerceAtLeast(0)
    val extractionScore: Int get() = signals
        .filter { it.category == SignalCategory.EXTRACTION }.sumOf { it.points }.coerceAtLeast(0)
    val contextScore: Int get() = signals
        .filter { it.category == SignalCategory.CONTEXT }.sumOf { it.points }

    val score: Int get() = (relevanceScore + extractionScore + contextScore).coerceIn(0, 100)
    val normalized: Float get() = score / 100f

    fun format(): String = buildString {
        fun printGroup(header: String, cat: SignalCategory) {
            val group = signals.filter { it.category == cat }
            if (group.isEmpty()) return
            appendLine("[$header]")
            group.forEach { sig ->
                val sign = if (sig.points >= 0) "+" else ""
                val matchInfo = if (sig.matchedText != null) "  «${sig.matchedText}»" else ""
                appendLine("  ${sig.label}$matchInfo: $sign${sig.points}")
            }
        }
        printGroup("相關性", SignalCategory.RELEVANCE)
        printGroup("資料擷取", SignalCategory.EXTRACTION)
        printGroup("情境", SignalCategory.CONTEXT)
        append("合計: $score/100  (相關 $relevanceScore + 擷取 $extractionScore + 情境 $contextScore)")
    }
}
