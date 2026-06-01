package com.moneykeeper.app.data.notification

/**
 * Analyzes LINE notification title/body to determine if it is from a financial sender
 * (bank official account, payment service) vs. a regular social chat.
 */
object LineSenderAnalyzer {

    enum class SenderType(val displayLabel: String) {
        OFFICIAL_BANK("官方銀行帳號"),
        PAYMENT_SERVICE("支付服務"),
        SECURITIES("證券/投資"),
        GROUP_CHAT("群組聊天"),
        NORMAL_CHAT("一般聊天"),
        UNKNOWN("未知"),
    }

    data class AnalysisResult(
        val senderType: SenderType,
        val matchedKeyword: String?,
        val isFinancial: Boolean,
    )

    private val BANK_KEYWORDS = listOf(
        // 大型商業銀行
        "國泰世華", "國泰銀行",
        "中國信託", "中信銀行", "中信",
        "玉山銀行", "玉山",
        "台新銀行", "台新", "Richart",
        "LINE Bank",
        // 公股銀行
        "台灣銀行", "台銀",
        "第一銀行", "一銀",
        "合作金庫", "合庫",
        "土地銀行", "土銀",
        "彰化銀行", "彰銀",
        "兆豐銀行", "兆豐",
        "華南銀行", "華銀",
        "中華郵政", "郵局",
        // 中小型銀行
        "永豐銀行", "永豐",
        "富邦銀行", "台北富邦", "北富銀",
        "聯邦銀行", "聯邦",
        "新光銀行", "新光",
        "遠東銀行", "遠東",
        "凱基銀行", "凱基",
        "華泰銀行", "華泰",
        "元大銀行", "元大",
        "上海商銀",
        // 外商銀行
        "星展銀行", "DBS",
        "渣打銀行", "渣打",
        "匯豐銀行", "HSBC",
        "花旗銀行", "花旗",
        // 信用卡關鍵字
        "信用卡消費", "刷卡通知", "帳單通知",
    )

    private val PAYMENT_KEYWORDS = listOf(
        "LINE Pay",
        "街口支付", "街口",
        "悠遊付",
        "Pi拍錢包", "Pi錢包", "拍錢包",
        "全支付",
        "icash Pay", "icash",
        "歐付寶", "綠界",
        "friDay錢包",
        "JKOS", "橘子支付",
    )

    private val SECURITIES_KEYWORDS = listOf(
        "Fugle", "富果",
        "永豐金證券",
        "富邦證券",
        "國泰證券",
        "元大證券",
        "對帳單", "成交通知", "委託成交",
    )

    private val GROUP_INDICATORS = listOf("的群組", "Group", "的社群", "群組通知")

    fun analyze(title: String, text: String): AnalysisResult {
        val combined = "$title $text"

        val bankMatch = BANK_KEYWORDS.firstOrNull { combined.contains(it) }
        if (bankMatch != null) return AnalysisResult(SenderType.OFFICIAL_BANK, bankMatch, true)

        val paymentMatch = PAYMENT_KEYWORDS.firstOrNull { combined.contains(it) }
        if (paymentMatch != null) return AnalysisResult(SenderType.PAYMENT_SERVICE, paymentMatch, true)

        val securitiesMatch = SECURITIES_KEYWORDS.firstOrNull { combined.contains(it) }
        if (securitiesMatch != null) return AnalysisResult(SenderType.SECURITIES, securitiesMatch, true)

        if (GROUP_INDICATORS.any { title.contains(it) }) {
            return AnalysisResult(SenderType.GROUP_CHAT, null, false)
        }

        return AnalysisResult(SenderType.UNKNOWN, null, false)
    }
}
