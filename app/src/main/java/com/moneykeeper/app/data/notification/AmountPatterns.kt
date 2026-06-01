package com.moneykeeper.app.data.notification

object AmountPatterns {

    data class AmountMatchResult(
        val amount: Double,
        val patternName: String,
        val regexPattern: String,
        val matchedText: String,
    )

    private data class NamedPattern(val name: String, val regex: Regex)

    private val TWD_PATTERNS = listOf(
        // Explicit currency prefix (highest priority)
        NamedPattern("NT$ 格式",    Regex("""NT\$\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("NTD 格式",    Regex("""NTD\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("新台幣格式",   Regex("""新台幣\s*([\d,]+(?:\.\d+)?)\s*元""")),
        // Label-colon patterns (label makes context unambiguous)
        NamedPattern("消費金額",    Regex("""消費金額[：:]\s*\$?([\d,]+(?:\.\d+)?)""")),
        NamedPattern("刷卡金額",    Regex("""刷卡金額[：:]\s*\$?([\d,]+(?:\.\d+)?)""")),
        NamedPattern("付款金額",    Regex("""付款金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("交易金額",    Regex("""交易金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("扣款金額",    Regex("""扣款金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("轉帳金額",    Regex("""轉帳金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("帳單金額",    Regex("""帳單金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("應繳金額",    Regex("""應[繳還]金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("金額冒號",    Regex("""金額[：:]\s*\$?([\d,]+(?:\.\d+)?)""")),
        // Action-verb + amount (still fairly specific)
        NamedPattern("扣款格式",    Regex("""扣款\s*NT?\$?\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("\$數字格式",  Regex("""\$\s*([\d,]+(?:\.\d+)?)""")),
        NamedPattern("消費數字元",  Regex("""消費\s*[NT$]*\s*([\d,]+(?:\.\d+)?)\s*元""")),
    )

    /** Backward-compatible pair result; parsers that haven't been updated still use this. */
    fun parseAmount(text: String): Pair<Double, Float>? =
        parseAmountWithDetail(text)?.let { it.amount to 0.9f }

    /** Rich result — which regex matched and what text was captured. */
    fun parseAmountWithDetail(text: String): AmountMatchResult? {
        for (np in TWD_PATTERNS) {
            val match = np.regex.find(text) ?: continue
            val raw = match.groupValues[1].replace(",", "")
            val amount = raw.toDoubleOrNull()?.takeIf { it > 0 } ?: continue
            return AmountMatchResult(amount, np.name, np.regex.pattern, match.value.trim())
        }
        return null
    }

    // Legacy merchant helper — kept for backward compat; prefer MerchantExtractor
    private val LEGACY_MERCHANT_PATTERNS = listOf(
        Regex("""於\s*(.+?)\s*消費"""),
        Regex("""在\s*(.+?)\s*刷卡"""),
        Regex("""向\s*(.+?)\s*付款"""),
    )

    fun parseMerchant(text: String): String? {
        for (p in LEGACY_MERCHANT_PATTERNS) {
            val m = p.find(text) ?: continue
            return m.groupValues[1].trim().takeIf { it.isNotEmpty() }
        }
        return null
    }
}
