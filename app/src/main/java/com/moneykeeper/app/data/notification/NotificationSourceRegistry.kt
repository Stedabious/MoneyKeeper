package com.moneykeeper.app.data.notification

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSourceRegistry @Inject constructor() {

    private val sources: List<NotificationSource> = listOf(

        // ═══ 台灣銀行 ═══

        // LINE Bank
        NotificationSource("com.linebank.tw",             "LINE Bank",         NotificationCategory.FINANCIAL, true),
        // 國泰世華 (CUBE app)
        NotificationSource("com.cathaybk.cube",           "國泰世華 CUBE",     NotificationCategory.FINANCIAL, true),
        NotificationSource("com.cathaybk.android",        "國泰世華",          NotificationCategory.FINANCIAL, true),
        // 中國信託 (CTBC)
        NotificationSource("com.ctbcbank.mobile",         "中國信託",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.ctbcbank.tw",             "中國信託",          NotificationCategory.FINANCIAL, true),
        // 玉山銀行
        NotificationSource("com.esunbank",                "玉山銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.esunbank.android",        "玉山銀行",          NotificationCategory.FINANCIAL, true),
        // 台新銀行 / Richart
        NotificationSource("com.taishinbank.online",      "台新 Richart",      NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.taishinbank",          "台新銀行",          NotificationCategory.FINANCIAL, true),
        // 永豐銀行
        NotificationSource("com.sinopac.app.android",     "永豐銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.sinopac.mobile",          "永豐銀行",          NotificationCategory.FINANCIAL, true),
        // 富邦銀行 / 台北富邦
        NotificationSource("com.fubon.mobile",            "台北富邦銀行",      NotificationCategory.FINANCIAL, true),
        NotificationSource("com.fubon.banking",           "富邦銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.fubon",                "富邦銀行",          NotificationCategory.FINANCIAL, true),
        // 第一銀行
        NotificationSource("com.firstbank.firstbankmobile", "第一銀行",        NotificationCategory.FINANCIAL, true),
        NotificationSource("com.firstbank.android",       "第一銀行",          NotificationCategory.FINANCIAL, true),
        // 兆豐銀行
        NotificationSource("com.megabank.android",        "兆豐銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.megabank",             "兆豐銀行",          NotificationCategory.FINANCIAL, true),
        // 合作金庫
        NotificationSource("com.tcb.android",             "合作金庫",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.tcb",                  "合作金庫",          NotificationCategory.FINANCIAL, true),
        // 土地銀行
        NotificationSource("tw.com.landbank",             "土地銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.landbank.android",        "土地銀行",          NotificationCategory.FINANCIAL, true),
        // 郵局 (中華郵政)
        NotificationSource("com.post.com.tw.postmobile",  "中華郵政",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.post",                 "中華郵政",          NotificationCategory.FINANCIAL, true),
        // 新光銀行
        NotificationSource("com.skbank.android",          "新光銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.skbank",               "新光銀行",          NotificationCategory.FINANCIAL, true),
        // 凱基銀行
        NotificationSource("com.kgi.android",             "凱基銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.kgi",                  "凱基銀行",          NotificationCategory.FINANCIAL, true),
        // 星展銀行 (DBS)
        NotificationSource("tw.com.dbs.dbstw",            "星展銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.dbs.android",             "星展銀行",          NotificationCategory.FINANCIAL, true),
        // 渣打銀行 (Standard Chartered)
        NotificationSource("com.sc.boc.tw",               "渣打銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.standardchartered.android","渣打銀行",         NotificationCategory.FINANCIAL, true),
        // 彰化銀行
        NotificationSource("tw.com.chb",                  "彰化銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.chb.android",             "彰化銀行",          NotificationCategory.FINANCIAL, true),
        // 華南銀行
        NotificationSource("tw.com.hnb",                  "華南銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.hnb.android",             "華南銀行",          NotificationCategory.FINANCIAL, true),
        // 聯邦銀行
        NotificationSource("tw.com.ubot",                 "聯邦銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.ubot.android",            "聯邦銀行",          NotificationCategory.FINANCIAL, true),
        // 遠東銀行
        NotificationSource("com.fepg.android",            "遠東銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.feib",                 "遠東銀行",          NotificationCategory.FINANCIAL, true),
        // 華泰銀行
        NotificationSource("tw.com.hwataibank.mobile",    "華泰銀行",          NotificationCategory.FINANCIAL, true),
        // 台灣銀行
        NotificationSource("tw.gov.bot.botapp",           "台灣銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.gov.bot.android",          "台灣銀行",          NotificationCategory.FINANCIAL, true),
        // 元大銀行
        NotificationSource("com.yuanta.android",          "元大銀行",          NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.yuanta",               "元大銀行",          NotificationCategory.FINANCIAL, true),

        // ═══ 支付平台 ═══

        // 街口支付
        NotificationSource("com.jkos.network",            "街口支付",          NotificationCategory.FINANCIAL, true),
        NotificationSource("com.jkos.jkopay",             "街口支付",          NotificationCategory.FINANCIAL, true),
        // Pi 拍錢包
        NotificationSource("com.ruten.pi",                "Pi 拍錢包",         NotificationCategory.FINANCIAL, true),
        // 悠遊付
        NotificationSource("com.easycard.android",        "悠遊付",            NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.easycard",             "悠遊付",            NotificationCategory.FINANCIAL, true),
        // 全支付
        NotificationSource("com.taiwanpay.android",       "全支付",            NotificationCategory.FINANCIAL, true),
        NotificationSource("tw.com.taiwanpay",            "全支付",            NotificationCategory.FINANCIAL, true),
        // icash Pay
        NotificationSource("com.icash.android",           "icash Pay",         NotificationCategory.FINANCIAL, true),

        // ═══ 證券 / 數位金融 ═══

        // Fugle 富果
        NotificationSource("com.fugle.trade",             "Fugle 富果",        NotificationCategory.FINANCIAL, true),
        // 永豐金證券
        NotificationSource("com.sinopac.sinofund",        "永豐金證券",        NotificationCategory.FINANCIAL, true),
        // 富邦證券
        NotificationSource("com.fubon.securities",        "富邦證券",          NotificationCategory.FINANCIAL, true),
        // 國泰證券
        NotificationSource("com.cathaybk.securities",    "國泰證券",          NotificationCategory.FINANCIAL, true),

        // ═══ LINE — SOCIAL；RelevanceFilter 做個別發送者分析 ═══
        NotificationSource("jp.naver.line.android",       "LINE",              NotificationCategory.SOCIAL,    false),

        // ═══ 社交 / 廣告 / 系統 ═══
        NotificationSource("com.facebook.katana",         "Facebook",          NotificationCategory.SOCIAL,    false),
        NotificationSource("com.facebook.orca",           "Messenger",         NotificationCategory.SOCIAL,    false),
        NotificationSource("com.instagram.android",       "Instagram",         NotificationCategory.SOCIAL,    false),
        NotificationSource("com.twitter.android",         "X (Twitter)",       NotificationCategory.SOCIAL,    false),
        NotificationSource("com.whatsapp",                "WhatsApp",          NotificationCategory.SOCIAL,    false),
        NotificationSource("com.google.android.gm",       "Gmail",             NotificationCategory.SOCIAL,    false),
        NotificationSource("com.android.systemui",        "系統 UI",           NotificationCategory.SYSTEM,    false),
        NotificationSource("com.android.vending",         "Google Play",       NotificationCategory.ADVERTISEMENT, false),
        NotificationSource("com.google.android.youtube",  "YouTube",           NotificationCategory.SOCIAL,    false),
        NotificationSource("com.shopee.tw",               "蝦皮購物",          NotificationCategory.ADVERTISEMENT, false),
        NotificationSource("com.yahoo.mobile",            "Yahoo",             NotificationCategory.SOCIAL,    false),
    )

    private val byPackage: Map<String, NotificationSource> = sources.associateBy { it.packageName }

    fun findSource(packageName: String): NotificationSource? = byPackage[packageName]

    fun isWhitelisted(packageName: String): Boolean = byPackage[packageName]?.isWhitelisted ?: false

    fun getCategory(packageName: String): NotificationCategory =
        byPackage[packageName]?.category ?: NotificationCategory.UNKNOWN

    fun getAllSources(): List<NotificationSource> = sources
}
