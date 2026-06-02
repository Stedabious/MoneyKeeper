# MoneyKeeper — 專案快照 2026-05-30

> 這份文件是給未來 AI session 的完整上下文快照。
> 反映本日（0530）session 結束時的真實程式碼狀態。

---

## 專案基本資訊

| 項目 | 內容 |
|------|------|
| Package | `com.moneykeeper.app` |
| Min SDK | 26 (Android 8.0) |
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM + Clean Architecture (single module) |
| DI | Hilt |
| DB | Room SQLite (local-first, no cloud) |
| Async | Kotlin Coroutines + Flow |

---

## 資料庫

### 版本：8（schema 未變，PARSE_VERSION 程式碼版本已升至 3）

### Migration 歷史

| Migration | 變更內容 |
|-----------|----------|
| 1→2 | 新增 `notification_log` table |
| 2→3 | 新增 `eventSource` 欄位（notification_log、pending_events、transactions） |
| 3→4 | transactions 加 `deletedAt` |
| 4→5 | notification_log 加 `parseStatus` |
| 5→6 | 新增 `regex_patterns` table |
| 6→7 | notification_log 加 `category`, `isFiltered`, `filteredReason`, `parseTrace` |
| 7→8 | notification_log 加 `parseVersion`, `lastParsedAt`, `lineSenderType` |

### Tables

| Table | 用途 |
|-------|------|
| `transactions` | 已確認消費記錄（可軟刪除 `deletedAt`） |
| `pending_events` | 解析出的待確認事件（status: PENDING/CONFIRMED/REJECTED） |
| `categories` | 8 種預設類別（id 1–8，id 8 為「其他」fallback） |
| `notification_log` | 所有通知記錄（含過濾結果、解析結果） |
| `regex_patterns` | 用戶自定義 Regex Pattern Library |

### `notification_log` Entity 完整欄位

```kotlin
data class NotificationLogEntity(
    id: Long,                    // PK autoGenerate
    packageName: String,
    appLabel: String,
    title: String,
    body: String,
    timestamp: Long,             // sbn.postTime
    parsedAmount: Double?,
    parserName: String?,
    confidence: Float?,
    eventSource: String,         // "REAL_NOTIFICATION" / "DEBUG_*"
    parseStatus: String,         // ParseStatus.name
    createdAt: Long,
    // v7 fields
    category: String,            // NotificationCategory.name
    isFiltered: Boolean,
    filteredReason: String?,
    parseTrace: String?,
    // v8 fields
    parseVersion: Int,           // 0 = 舊資料; 目前 PARSE_VERSION = 3
    lastParsedAt: Long?,
    lineSenderType: String?,     // LineSenderAnalyzer.SenderType.name
)
```

---

## 通知解析流程（完整）

```
onNotificationPosted(sbn)
  ↓
  取 packageName, title, EXTRA_BIG_TEXT (fallback: EXTRA_TEXT)
  ↓
  RelevanceFilter.filter(packageName, title, text)
    ├─ LINE (jp.naver.line.android) → LineSenderAnalyzer
    │     OFFICIAL_BANK / PAYMENT_SERVICE / SECURITIES → allowed
    │     GROUP_CHAT / NORMAL_CHAT / UNKNOWN → blocked
    ├─ 已知白名單 App → allowed
    ├─ 已知非白名單 App → blocked（category 記錄）
    └─ 未知 App → 檢查金融關鍵字 → allowed/blocked
  ↓
  [blocked] → insert(isFiltered=true, parseStatus=UNKNOWN)  → 結束
  ↓
  [allowed] → ParserRegistry.parse(packageName, title, text)
    ├─ TransferParser    → TRANSFER (amount=null 正常)
    ├─ LinePayParser     → LINE Pay 消費
    ├─ LineBankParser    → LINE Bank
    ├─ CathayParser      → 國泰世華
    ├─ CTBCParser        → 中國信託
    ├─ GenericBankParser → 廣泛金融關鍵字 fallback
    └─ UserPatternEngine → (parser 返回 null 時) 用戶自定義 pattern 救援
  ↓
  → ParseResult { event, status, parseTrace, scores }
  ↓
  insert(notification_log)
  ↓
  if status.shouldCreatePendingEvent && amount != null
    → PendingEventRepository.insertFromParsed(event)
```

---

## Parser Architecture

### PARSE_VERSION = 3

版本歷史：
- v1：原始實作（已廢棄）
- v2：三維信心分數系統（RELEVANCE/EXTRACTION/CONTEXT）
- v3：Rich signal labels + 用戶 pattern 整合（本日新增）

### ParseStatus（完整 enum）

| 值 | displayLabel | shouldCreatePendingEvent |
|----|-------------|--------------------------|
| HIGH_CONFIDENCE | 高信心 | ✓ |
| MEDIUM_CONFIDENCE | 中信心 | ✓ |
| LOW_CONFIDENCE | 低信心 | ✓ |
| PARTIAL_PARSE | 部分解析 | ✗ |
| TRANSFER | 轉帳 | ✗ |
| UNKNOWN | 未識別 | ✗ |
| IGNORED | 已過濾 | ✗ |

Backward compat in `fromName()`:
- `PARSED_EXPENSE` → MEDIUM_CONFIDENCE
- `PARSED_TRANSFER` → TRANSFER
- `BELOW_THRESHOLD` → PARTIAL_PARSE
- `UNPARSED` → UNKNOWN

### 信心評分系統（0–100）

```
RELEVANCE 維度：
  白名單 App         +25
  強金融關鍵字        +15  (消費/刷卡/扣款/付款)
  弱金融關鍵字        +8   (交易/帳單/餘額/入帳/ATM/NT$)

EXTRACTION 維度：
  金額擷取           +30
  商家擷取           +8

CONTEXT 維度：
  專屬解析器         +15
  未知 App           -5
```

**狀態閾值**（amount 必須存在才能到 HIGH/MED/LOW）：

| 分數 | 有 amount | 狀態 |
|------|-----------|------|
| ≥ 60 | ✓ | HIGH_CONFIDENCE |
| ≥ 40 | ✓ | MEDIUM_CONFIDENCE |
| ≥ 25 | ✓ | LOW_CONFIDENCE |
| < 25 | ✓ | PARTIAL_PARSE |
| 任何 | ✗ | PARTIAL_PARSE |

典型場景：白名單 App (25) + 強關鍵字 (15) + 金額 (30) + 專屬解析器 (15) = **85 → HIGH**

### AmountPatterns（TWD 金額 Regex 清單）

```
NT$ 格式      NT\$\s*([\d,]+(?:\.\d+)?)
NTD 格式      NTD\s*([\d,]+(?:\.\d+)?)
新台幣格式    新台幣\s*([\d,]+(?:\.\d+)?)\s*元
消費金額      消費金額[：:]\s*\$?([\d,]+(?:\.\d+)?)
刷卡金額      刷卡金額[：:]\s*\$?([\d,]+(?:\.\d+)?)
付款金額      付款金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)
交易金額      交易金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)
扣款金額      扣款金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)
轉帳金額      轉帳金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)
帳單金額      帳單金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)
應繳金額      應[繳還]金額[：:]\s*NT?\$?\s*([\d,]+(?:\.\d+)?)
金額冒號      金額[：:]\s*\$?([\d,]+(?:\.\d+)?)
扣款格式      扣款\s*NT?\$?\s*([\d,]+(?:\.\d+)?)
$數字格式     \$\s*([\d,]+(?:\.\d+)?)
消費數字元    消費\s*[NT$]*\s*([\d,]+(?:\.\d+)?)\s*元
```

`parseAmountWithDetail()` 返回 `AmountMatchResult(amount, patternName, regexPattern, matchedText)`。

### MerchantExtractor（商家 Regex 清單）

```
於...消費/刷卡   於\s*(.{2,20}?)\s*(?:消費|刷卡|交易|付款)
在...刷卡        在\s*(.{2,20}?)\s*刷卡
向...付款        向\s*(.{2,20}?)\s*付款
特店/商家標籤    (?:特店|商家|商店|店名)[：:]\s*(.{2,30})
消費地點         消費地點[：:]\s*(.{2,30})
您於...完成      您於\s*(.{2,20}?)\s*(?:完成|付款|消費)
消費商店         消費商店[：:]\s*(.{2,30})
交易商店         交易商店[：:]\s*(.{2,30})
```

`extractWithDetail()` 返回 `MerchantMatchResult(merchant, patternName, regexPattern, matchedText)`。

### ConfidenceSignal（新格式）

```kotlin
data class ConfidenceSignal(
    val label: String,
    val points: Int,
    val category: SignalCategory,    // RELEVANCE / EXTRACTION / CONTEXT
    val matchedText: String? = null, // 命中的文字片段
)
```

`ConfidenceBreakdown.format()` 輸出範例：
```
[相關性]
  白名單 App: +25
  強金融關鍵字: 消費, 刷卡  «消費/刷卡»: +15

[資料擷取]
  金額: NT$ 格式  «NT$1,234»: +30
  商家: 特店/商家標籤  «全家便利商店»: +8

[情境]
  專屬解析器: +15

合計: 88/100  (相關 40 + 擷取 38 + 情境 15)
```

### UserPatternEngine（新 2026-05-30）

`@Singleton`，啟動時從 `RegexPatternRepository` collect Flow 並快取到記憶體。

功能：
- `tryMatchAmount(text)` → `UserMatch?`：找第一個命中的用戶 AMOUNT pattern
- `tryMatchMerchant(text)` → `UserMatch?`：找第一個命中的用戶 MERCHANT pattern
- `tryAll(text)` → `List<PatternAttempt>`：全部 pattern 的嘗試結果（用於 parseTrace）

**用戶 pattern 救援邏輯**：若內建 parser 返回 amount=null，但用戶 AMOUNT pattern 命中，則：
1. 用命中值建立 `ParsedEvent`
2. 跑 `buildFullBreakdown` 計算分數
3. 結果升級至 LOW_CONFIDENCE 以上
4. parserName 標記為 `"${parserName}+UserPattern"`

---

## 金融來源 Whitelist（NotificationSourceRegistry）

### 台灣銀行（部分有多個 package 備用）

| 銀行 | 代表 package |
|------|-------------|
| LINE Bank | `com.linebank.tw` |
| 國泰世華 | `com.cathaybk.cube`, `com.cathaybk.android` |
| 中國信託 | `com.ctbcbank.mobile`, `com.ctbcbank.tw` |
| 玉山銀行 | `com.esunbank`, `com.esunbank.android` |
| 台新/Richart | `com.taishinbank.online`, `tw.com.taishinbank` |
| 永豐銀行 | `com.sinopac.app.android`, `com.sinopac.mobile` |
| 富邦/台北富邦 | `com.fubon.mobile`, `com.fubon.banking`, `tw.com.fubon` |
| 第一銀行 | `com.firstbank.firstbankmobile` |
| 兆豐銀行 | `com.megabank.android`, `tw.com.megabank` |
| 合作金庫 | `com.tcb.android`, `tw.com.tcb` |
| 土地銀行 | `tw.com.landbank` |
| 中華郵政 | `com.post.com.tw.postmobile` |
| 新光銀行 | `com.skbank.android` |
| 凱基銀行 | `com.kgi.android` |
| 星展銀行 | `tw.com.dbs.dbstw` |
| 渣打銀行 | `com.sc.boc.tw` |
| 彰化銀行 | `tw.com.chb` |
| 華南銀行 | `tw.com.hnb` |
| 聯邦銀行 | `tw.com.ubot` |
| 遠東銀行 | `com.fepg.android` |
| 華泰銀行 | `tw.com.hwataibank.mobile` |
| 台灣銀行 | `tw.gov.bot.botapp` |
| 元大銀行 | `com.yuanta.android` |

### 支付平台

| 平台 | Package |
|------|---------|
| 街口支付 | `com.jkos.network`, `com.jkos.jkopay` |
| Pi 拍錢包 | `com.ruten.pi` |
| 悠遊付 | `com.easycard.android` |
| 全支付 | `com.taiwanpay.android` |
| icash Pay | `com.icash.android` |

### 證券

| 機構 | Package |
|------|---------|
| Fugle 富果 | `com.fugle.trade` |
| 永豐金證券 | `com.sinopac.sinofund` |
| 富邦證券 | `com.fubon.securities` |
| 國泰證券 | `com.cathaybk.securities` |

### LINE 特殊處理

`jp.naver.line.android` 在 registry 標記為 `SOCIAL, whitelisted=false`。
`RelevanceFilter` 對 LINE 單獨走 `LineSenderAnalyzer` 分析發送者類型：

| SenderType | isFinancial | 觸發詞（銀行關鍵字） |
|-----------|-------------|---------------------|
| OFFICIAL_BANK | ✓ | 國泰世華、中信、玉山、台新、Richart、土銀、兆豐、永豐、富邦、彰銀、新光、凱基、星展 DBS、渣打、合庫、第一銀行… |
| PAYMENT_SERVICE | ✓ | LINE Pay、街口、悠遊付、Pi錢包、全支付、icash… |
| SECURITIES | ✓ | Fugle、富果、永豐金證券、對帳單、成交通知… |
| GROUP_CHAT | ✗ | 標題含「的群組」「Group」 |
| UNKNOWN | ✗ | 其他 |

---

## 導覽路由

```
dashboard
add_transaction
pending_events
notification_log
trash
pattern_library
regex_builder/{logId}
notification_detail/{logId}
```

---

## 畫面列表

| 畫面 | ViewModel | 主要功能 |
|------|-----------|----------|
| DashboardScreen | DashboardViewModel | 本月消費統計、最近記錄 |
| AddTransactionScreen | TransactionViewModel | 手動新增消費 |
| PendingEventScreen | PendingEventViewModel | 確認/拒絕解析出的事件 |
| NotificationLogScreen | NotificationLogViewModel | 通知記錄 (ALL/FINANCIAL/IGNORED/DEBUG 篩選) + Reparse |
| NotificationDetailScreen | NotificationDetailViewModel | 通知完整詳情 + Copy 功能 + 重新解析 |
| PatternLibraryScreen | PatternLibraryViewModel | Regex Pattern Library 瀏覽/複製/刪除 |
| RegexBuilderScreen | RegexBuilderViewModel | 從通知 body 快速建立 regex pattern |
| TrashScreen | TrashViewModel | 軟刪除的消費記錄 |

---

## NotificationLogScreen 功能

- 四個 filter tab：ALL / FINANCIAL / IGNORED / DEBUG
- 每筆 log 顯示 `ParseStatusChip`（顏色依 status）
- DEBUG tab 有「注入測試通知」按鈕
- 頂部 actions：
  - 重解析失敗記錄（UNKNOWN/PARTIAL_PARSE/LOW_CONFIDENCE + 舊值）
  - 重解析舊版本（parseVersion < PARSE_VERSION = 3）
  - 清除 Debug 資料
  - 清除已過濾記錄
- `ReparseProgress` StateFlow 顯示批次重解析進度

## NotificationDetailScreen Copy 功能（新 2026-05-30）

| 操作 | 複製內容 |
|------|---------|
| 內文標題旁圖示 | 標題 + 內文 |
| 長按內文卡片 | 僅內文 |
| 解析追蹤旁圖示 | parseTrace 全文 |
| 「複製完整 Debug 報告」按鈕 | Package、標題、內文、parse 結果、parseTrace |

---

## Reparse 機制

### `reparseFailedLogs()`（NotificationLogViewModel）
- 對象：`parseStatus` IN ('UNKNOWN', 'PARTIAL_PARSE', 'LOW_CONFIDENCE', 'UNPARSED', 'BELOW_THRESHOLD')
- 重跑 `relevanceFilter.filter()` 和 `parserRegistry.parse()`

### `reparseOutdatedLogs()`（NotificationLogViewModel）
- 對象：`parseVersion < PARSE_VERSION`（目前 = 3，故 v0/1/2 全部排入）
- 相同流程

### 單筆重解析（NotificationDetailViewModel.reparse()）
- 點 detail 畫面的 Refresh 鍵
- 立即更新畫面

---

## DI 注入鏈（關鍵 Singleton）

```
AppDatabase (DatabaseModule @Provides)
  ├─ NotificationLogDao → NotificationLogRepository (@Inject)
  ├─ PendingEventDao   → PendingEventRepository (@Inject)
  ├─ RegexPatternDao   → RegexPatternRepository (@Inject)
  │                         └─ UserPatternEngine (@Singleton @Inject)
  │                               └─ ParserRegistry (@Singleton @Inject)
  │                                    └─ NotificationSourceRegistry (@Singleton @Inject)
  └─ ... (其他 DAO/Repo)

RelevanceFilter (@Singleton @Inject)
  └─ NotificationSourceRegistry

MoneyNotificationListenerService (@AndroidEntryPoint)
  ├─ ParserRegistry
  ├─ RelevanceFilter
  ├─ PendingEventRepository
  └─ NotificationLogRepository
```

---

## Pattern Library

### DB entity: `regex_patterns`

```kotlin
data class RegexPatternEntity(
    id: Long,
    patternString: String,       // regex 字串
    patternType: String,         // PatternType.name (AMOUNT / MERCHANT / TRANSFER_KEYWORD / OTHER)
    sourceBody: String,          // 來源通知內文
    sourcePackageName: String,
    sourceAppLabel: String,
    testPassed: Boolean?,        // 儲存時是否命中 sourceBody
    note: String,
    createdAt: Long,
)
```

### RegexBuilderScreen 流程
從 NotificationDetailScreen 點進 `regex_builder/{logId}`，預填通知內文，
讓使用者自由撰寫/測試 regex，儲存後進入 Pattern Library。

### UserPatternEngine 整合
- Pattern Library 的 AMOUNT pattern 會在 parser 返回 null 時自動嘗試救援
- 所有 pattern 的嘗試結果（✓/✗）都附加在 parseTrace 的 `[用戶自定義 Patterns]` 區塊

---

## 程式碼慣例

- ViewModel：`@HiltViewModel` + `@Inject constructor`，`SharingStarted.WhileSubscribed(5_000)`
- Screen 訂閱：`collectAsStateWithLifecycle()`（非 `collectAsState()`）
- DB 寫入：`viewModelScope.launch { }` 不指定 dispatcher
- extension 函數（`toDomain()`, `toEntity()`）定義在 entity 檔案底部
- 不寫說明性 comment，只在 WHY 不明顯時才加

---

## 已知限制 / 下一步

- **package 名稱不確定**：部分台灣銀行 App 的 package 是推測值；若不匹配，會 fallback 到關鍵字偵測（仍可辨識）
- **merchant 擷取**：仍依賴固定 regex，對非典型格式（如 CTBC 特殊格式）可能失敗 → 用戶可用 Pattern Library 補充
- **GenericAmountParser**：存在但不在 parser chain 中（已被 GenericBankParser 取代），可考慮清理
- **無 Unit Test**：parser 邏輯（AmountPatterns、各 Parser）是最高優先測試目標

---

*快照生成時間：2026-05-30*
