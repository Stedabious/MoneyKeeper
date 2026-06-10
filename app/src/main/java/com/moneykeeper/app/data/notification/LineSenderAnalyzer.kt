package com.moneykeeper.app.data.notification

/**
 * Identifies whether a LINE notification originates from an official financial sender.
 *
 * Strategy (in priority order):
 *  1. title contains GROUP_INDICATORS → GROUP_CHAT, not financial
 *  2. title contains a known official sender name → financial (most reliable)
 *  3. title ends with institution suffixes (e.g. "銀行") AND body has explicit
 *     financial signals → financial (safety net for unlisted senders)
 *  4. Everything else → UNKNOWN, not financial
 *
 * Key principle: EXTRA_TITLE is the sender's identity in LINE (account name or
 * contact name). Scanning title-first prevents false positives where a friend
 * mentions "LINE Pay" or "NT$" in a personal message.
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

    // ── Known official LINE sender names (as they appear in EXTRA_TITLE) ──────

    private val BANK_OFFICIAL_TITLES = listOf(
        // 國泰
        "國泰世華銀行", "國泰世華", "國泰CUBE",
        // 中信
        "中國信託銀行", "中國信託", "中信銀行",
        // 玉山
        "玉山銀行", "玉山e.Bank", "玉山 e.Bank",
        // 台新
        "台新銀行", "Richart",
        // 永豐
        "永豐銀行", "永豐金",
        // 富邦
        "台北富邦銀行", "台北富邦", "富邦銀行",
        // 公股
        "臺灣銀行", "台灣銀行",
        "第一銀行", "一銀",
        "合作金庫銀行", "合作金庫", "合庫",
        "土地銀行",
        "彰化銀行", "彰銀",
        "兆豐銀行", "兆豐國際商銀",
        "華南銀行", "華南商銀",
        "臺灣中小企業銀行",
        "中華郵政", "郵局",
        // 其他
        "新光銀行",
        "凱基銀行",
        "遠東銀行",
        "聯邦銀行",
        "上海商業儲蓄銀行", "上海商銀",
        "陽信銀行",
        "元大銀行",
        "安泰銀行",
        "台灣工業銀行",
        // 外商
        "星展銀行", "DBS 銀行", "DBS",
        "渣打銀行", "渣打",
        "匯豐銀行", "HSBC",
        "花旗銀行",
        "滙豐銀行",
    )

    private val PAYMENT_OFFICIAL_TITLES = listOf(
        "LINE Pay",
        "街口支付", "街口",
        "悠遊付",
        "Pi拍錢包", "Pi 拍錢包",
        "全支付",
        "icash Pay",
        "歐付寶", "綠界",
        "friDay錢包",
        "JKOS", "橘子支付",
    )

    private val SECURITIES_OFFICIAL_TITLES = listOf(
        "元大證券", "元大e點通",
        "富邦證券",
        "國泰證券",
        "永豐金證券",
        "凱基證券",
        "統一證券",
        "兆豐證券",
        "Fugle", "富果",
    )

    // ── Structural suffix check (safety net for senders not in explicit list) ──
    // Only applied when title itself ends with these — very unlikely in personal names
    private val BANK_TITLE_SUFFIXES = listOf("銀行", "信用卡")

    // High-confidence financial body signals — used only as secondary confirmation
    // when title passes structural check but isn't in the explicit list
    private val FINANCIAL_BODY_SIGNALS = listOf(
        "消費金額", "刷卡金額", "交易金額", "付款金額", "扣款金額",
        "信用卡消費", "帳單金額", "繳費通知",
        "NT$", "NTD", "新台幣",
    )

    // Title patterns that clearly indicate a group/social chat
    private val GROUP_INDICATORS = listOf("的群組", "的社群", " Group", "群組通知", "聊天室")

    // ── Main analysis ─────────────────────────────────────────────────────────

    fun analyze(title: String, text: String): AnalysisResult {
        // Step 1: Group/social chat → always reject
        if (GROUP_INDICATORS.any { title.contains(it) }) {
            return AnalysisResult(SenderType.GROUP_CHAT, null, false)
        }

        // Step 2: Title matches a known official bank/payment/securities sender
        BANK_OFFICIAL_TITLES.firstOrNull { title.contains(it) }?.let { matched ->
            return AnalysisResult(SenderType.OFFICIAL_BANK, matched, true)
        }
        PAYMENT_OFFICIAL_TITLES.firstOrNull { title.contains(it) }?.let { matched ->
            return AnalysisResult(SenderType.PAYMENT_SERVICE, matched, true)
        }
        SECURITIES_OFFICIAL_TITLES.firstOrNull { title.contains(it) }?.let { matched ->
            return AnalysisResult(SenderType.SECURITIES, matched, true)
        }

        // Step 3: Title ends with institution suffix (e.g. "第三方銀行" not yet in list)
        //         AND body contains an explicit financial signal
        val titleHasInstitutionSuffix = BANK_TITLE_SUFFIXES.any { title.endsWith(it) }
        if (titleHasInstitutionSuffix && FINANCIAL_BODY_SIGNALS.any { text.contains(it) }) {
            return AnalysisResult(SenderType.OFFICIAL_BANK, title, true)
        }

        // Step 4: No match — personal message, ad, or unknown account
        return AnalysisResult(SenderType.UNKNOWN, null, false)
    }
}
