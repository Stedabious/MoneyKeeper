# Parser Architecture

通知解析引擎設計說明。

---

## 概覽

```
onNotificationPosted(sbn)
        │
        ▼
┌───────────────────┐
│  RelevanceFilter  │  ← 白名單 + 關鍵字過濾
└───────────────────┘
   │ isAllowed=false → NotificationLog(isFiltered=true)  STOP
   │ isAllowed=true
        ▼
┌───────────────────┐
│  ParserRegistry   │  ← Strategy Chain，按序嘗試各 Parser
│  .parse()         │
└───────────────────┘
        │
        ├── parser.canHandle() = false → 跳過，記錄 trace
        │
        └── parser.canHandle() = true
              │
              ▼
        parser.parse() → ParsedEvent? + confidence
              │
              ▼
        ConfidenceBreakdown (三維評分)
              │
              ▼
┌─────────────────────┐
│  DuplicateDetector  │  ← 5 秒內相同金額？
└─────────────────────┘
   │ isDuplicate=true → ParseStatus.DUPLICATE
   │ isDuplicate=false
        ▼
   NotificationLogRepository.insert()
        │
        ├── shouldCreatePendingEvent=true
        │       ▼
        │  PendingEventRepository.insertFromParsed()
        │
        └── shouldCreatePendingEvent=false  STOP
```

---

## Parser 順序

`ParserRegistry` 維護有序的 Strategy List，按序嘗試，**第一個 `canHandle()` = true 且回傳非 null event 的即為結果**。

```kotlin
private val parsers: List<NotificationParserStrategy> = listOf(
    TransferParser(),    // 1. 優先攔截轉帳（避免誤記為支出）
    LinePayParser(),     // 2. LINE Pay / LINE Bank Pay
    LineBankParser(),    // 3. LINE Bank 消費通知
    CathayParser(),      // 4. 國泰世華 CUBE
    CTBCParser(),        // 5. 中國信託
    GenericBankParser(), // 6. 通用備援（最後）
)
```

### NotificationParserStrategy 介面

```kotlin
interface NotificationParserStrategy {
    val name: String
    fun canHandle(packageName: String, title: String, text: String): Boolean
    fun parse(packageName: String, title: String, text: String): ParsedEvent?
}
```

---

## 各 Parser 說明

### TransferParser
- **目的：** 識別轉帳通知，防止誤判為消費支出
- **觸發條件：** title 或 text 含「轉帳」、「匯款」、「ATM 提款」等關鍵字
- **回傳 ParseStatus：** `TRANSFER`（不建立 PendingEvent）

### LinePayParser
- **目的：** 解析 LINE Pay 消費通知
- **packageName：** `jp.naver.line.android`
- **觸發條件：** title 含「LINE Pay」或「付款成功」
- **金額格式：** `NT$1,234` 或 `NTD 1,234`
- **信心度加成：** +0.1（明確的 LINE Pay 來源）

### LineBankParser
- **目的：** 解析 LINE Bank 消費/帳戶通知
- **packageName：** `com.linebank.tw`
- **格式：** LINE Bank 特有的通知結構

### CathayParser
- **目的：** 國泰世華 CUBE App 刷卡通知
- **packageName：** `com.cathaybk.cube`、`com.cathaybk.android`
- **格式：** 「消費金額：NT$xxx」+「消費地點：xxx」

### CTBCParser
- **目的：** 中國信託刷卡消費通知
- **packageName：** `com.ctbcbank.mobile`、`com.ctbcbank.tw`

### GenericBankParser（備援）
- **目的：** 無專屬 Parser 時的通用備援
- **觸發條件：** 任何通過 RelevanceFilter 的通知
- **信心度係數：** ×0.8（可靠度較低）
- **必須放最後**，避免攔截應由專屬 Parser 處理的通知

---

## RelevanceFilter

決定通知是否應送入解析流程。

```
通知到達
  │
  ├── packageName == "jp.naver.line.android"
  │       → LineSenderAnalyzer.analyze() → isFinancial?
  │
  ├── NotificationSourceRegistry.findSource(packageName) != null
  │       → isWhitelisted?  YES → 允許；NO → 過濾
  │
  └── 未知來源
          → 掃描 title + text 是否含金融關鍵字（15 個）
            YES → 允許；NO → 過濾
```

**金融關鍵字：**  
消費、刷卡、扣款、付款、轉帳、交易、帳單、餘額、入帳、ATM、NT$、NTD、信用卡、簽帳、扣繳

---

## LineSenderAnalyzer

LINE App 的特殊處理。  
同一個 packageName (`jp.naver.line.android`) 可能是銀行帳號或朋友聊天，需逐一分析。

```kotlin
enum class SenderType {
    BANK,       // 銀行官方帳號
    PAYMENT,    // 支付服務
    SECURITIES, // 證券/投資
    UNKNOWN,    // 社群、廣告等（非金融）
}
```

**判斷方式：** 掃描 title + text 中的關鍵字群組
- `BANK_KEYWORDS`：銀行、信用卡、刷卡、扣款 ⋯
- `PAYMENT_KEYWORDS`：LINE Pay、街口、Pi 拍錢包 ⋯
- `SECURITIES_KEYWORDS`：股票、基金、ETF、成交 ⋯

---

## AmountPatterns

TWD 金額萃取的核心正則表達式庫（15 種模式）。

```kotlin
object AmountPatterns {
    data class AmountMatchResult(
        val amount: Double,
        val patternName: String,  // 哪個 pattern 命中
        val regexPattern: String,
        val matchedText: String,  // 實際匹配的文字片段
    )
    
    fun parseAmount(text: String): Pair<Double, Float>?
    fun parseAmountWithDetail(text: String): AmountMatchResult?
}
```

**Pattern 優先順序（高 → 低特異性）：**

| 優先 | 模式名稱 | 範例 |
|------|----------|------|
| 1 | NT$ 格式 | `NT$1,234` |
| 2 | NTD 格式 | `NTD 1,234` |
| 3 | 新台幣格式 | `新台幣 1,234 元` |
| 4 | 消費金額 | `消費金額：$1,234` |
| 5 | 刷卡金額 | `刷卡金額：1,234` |
| 6 | 付款金額 | `付款金額：NT$1,234` |
| 7 | 交易金額 | `交易金額：1,234` |
| 8 | 扣款金額 | `扣款金額：1,234` |
| 9 | 轉帳金額 | `轉帳金額：1,234` |
| 10 | 帳單金額 | `帳單金額：1,234` |
| 11 | 應繳/應還金額 | `應繳金額：1,234` |
| 12 | 金額冒號 | `金額：$1,234` |
| 13 | 扣款格式 | `扣款 NT$1,234` |
| 14 | $數字格式 | `$1,234` |
| 15 | 消費數字元 | `消費 1,234 元` |

---

## ConfidenceBreakdown（三維評分）

最終信心度由三個維度的分數加總決定（0–100），再正規化為 0.0–1.0。

```
totalScore = relevanceScore + extractionScore + contextScore
confidence = totalScore / 100.0
```

**RELEVANCE（來源可信度）：**
- 白名單來源：+40
- 已知 App（非白名單）：+15
- 未知來源通過關鍵字：+5

**EXTRACTION（萃取品質）：**
- 金額萃取成功：+30
- 金額格式高特異性（NT$/NTD）：+10
- 金額格式中特異性（金額冒號）：+5
- 商家萃取成功：+10
- 強金融關鍵字命中：每個 +5（上限 +15）

**CONTEXT（上下文加分）：**
- 非 GenericBankParser（專屬 Parser）：+10
- 使用者自訂 pattern 命中：+10
- LineSenderAnalyzer = BANK/PAYMENT：+5

**ParseStatus 對應：**

| totalScore | ParseStatus |
|-----------|-------------|
| ≥ 70 | HIGH_CONFIDENCE |
| ≥ 50 | MEDIUM_CONFIDENCE |
| ≥ 30 | LOW_CONFIDENCE |
| < 30 且有金額 | PARTIAL_PARSE |
| 轉帳 pattern | TRANSFER |
| 無金額 | UNKNOWN |

---

## DuplicateDetector

防止相同消費因多 App 通知（如銀行 + LINE Pay）重複記帳。

```kotlin
companion object {
    const val WINDOW_MS = 5_000L  // 5 秒窗口
}

suspend fun check(amount: Double, packageName: String, windowMs: Long = WINDOW_MS): DuplicateResult
```

**判斷邏輯：**
1. 查詢 `notification_log`：5 秒內是否有相同 `parsedAmount` 且非 DUPLICATE 的記錄
2. 優先找同 App（`packageName` 相同）的重複
3. 找不到同 App 則找跨 App 重複
4. 無論哪種，都標記為 `ParseStatus.DUPLICATE`，不建立 `PendingEvent`

---

## UserPatternEngine

使用者在 Regex Builder 頁面建立的自訂解析規則，作為 Parser 的「救援層」。

```kotlin
@Singleton
class UserPatternEngine @Inject constructor(repository: RegexPatternRepository) {
    fun tryMatchAmount(text: String): UserMatch?
    fun tryMatchMerchant(text: String): UserMatch?
    fun tryAll(text: String): List<PatternAttempt>
    val hasPatterns: Boolean
}
```

**觸發時機：** Parser 無法萃取金額時，`UserPatternEngine.tryMatchAmount()` 嘗試救援；若成功，提升信心度評分。

**Pattern 類型：**
- `AMOUNT`：萃取金額的正則表達式
- `MERCHANT`：萃取商家名稱的正則表達式

---

## NotificationSourceRegistry

台灣金融機構 + 支付平台的白名單，共 40+ 個 packageName。

**涵蓋類別：**
- 台灣銀行（LINE Bank、國泰、中信、玉山、台新、永豐、富邦、第一、兆豐、合作金庫⋯）
- 電子支付（LINE Pay、街口、Pi 拍錢包、悠遊付、歐付寶⋯）
- 信用卡 App（聯邦、渣打、匯豐⋯）
- 證券 App（元大、富邦證券、凱基⋯）
- 電商（蝦皮、PChome⋯）

**新增銀行 Parser 步驟：**
1. 建立 `data/notification/parser/XxxParser.kt`，實作 `NotificationParserStrategy`
2. 在 `ParserRegistry.parsers` list 中加入（在 `GenericBankParser` 之前）
3. 在 `NotificationSourceRegistry` 新增 packageName（`isWhitelisted = true`）
4. `canHandle()` 使用 packageName 精確匹配

---

## Parse Trace 格式

每筆 `notification_log` 記錄包含完整的解析追蹤日誌：

```
✗ TransferParser: canHandle=false
✗ LinePayParser: canHandle=false
✓ CathayParser: amount=1234.0, merchant=全家便利商店
[RELEVANCE]
  ✓ 白名單來源: +40
[EXTRACTION]
  ✓ 金額萃取 (NT$ 格式 → «NT$1,234»): +30
  ✓ 商家萃取: +10
  ✓ 強金融關鍵字: 消費: +5
[CONTEXT]
  ✓ 專屬 Parser (CathayParser): +10
→ HIGH_CONFIDENCE (95/100)
```
