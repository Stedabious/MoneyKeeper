package com.moneykeeper.app.data.notification

object MerchantExtractor {

    data class MerchantMatchResult(
        val merchant: String,
        val patternName: String,
        val regexPattern: String,
        val matchedText: String,
    )

    private data class NamedPattern(val name: String, val regex: Regex)

    private val PATTERNS = listOf(
        NamedPattern("於...消費/刷卡",  Regex("""於\s*(.{2,20}?)\s*(?:消費|刷卡|交易|付款)""")),
        NamedPattern("在...刷卡",       Regex("""在\s*(.{2,20}?)\s*刷卡""")),
        NamedPattern("向...付款",       Regex("""向\s*(.{2,20}?)\s*付款""")),
        NamedPattern("特店/商家標籤",   Regex("""(?:特店|商家|商店|店名)[：:]\s*(.{2,30})""")),
        NamedPattern("消費地點",        Regex("""消費地點[：:]\s*(.{2,30})""")),
        NamedPattern("您於...完成",     Regex("""您於\s*(.{2,20}?)\s*(?:完成|付款|消費)""")),
        NamedPattern("消費商店",        Regex("""消費商店[：:]\s*(.{2,30})""")),
        NamedPattern("交易商店",        Regex("""交易商店[：:]\s*(.{2,30})""")),
    )

    private val TRIM_SUFFIXES = Regex("""[\s,，。！!？?]+$""")

    /** Simple string result for callers that don't need pattern detail. */
    fun extract(combined: String): String? = extractWithDetail(combined)?.merchant

    /** Rich result — which regex pattern matched and what candidate text was extracted. */
    fun extractWithDetail(combined: String): MerchantMatchResult? {
        for (np in PATTERNS) {
            val candidate = np.regex.find(combined)
                ?.groupValues?.get(1)
                ?.replace(TRIM_SUFFIXES, "")
                ?.trim()
                ?: continue
            if (candidate.length in 2..30 && !candidate.all { it.isDigit() }) {
                return MerchantMatchResult(candidate, np.name, np.regex.pattern, candidate)
            }
        }
        return null
    }
}
