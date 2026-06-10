# MoneyKeeper — 專案快照 2026-06-10

> Claude Code 工作快照，用於跨 session 上下文延續。  
> 人工產生，反映當前程式碼實際狀況。

---

## 專案基本資訊

| 項目 | 值 |
|------|-----|
| Package | `com.moneykeeper.app` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| versionCode | **2** |
| versionName | `1.0.1` |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.03 (Material3 1.3.x) |
| Room DB 版本 | **11** |
| PARSE_VERSION | **4** |

---

## 目錄結構（完整）

```
app/src/main/java/com/moneykeeper/app/
├── MainActivity.kt                     @AndroidEntryPoint，enableEdgeToEdge
├── MoneyKeeperApplication.kt           @HiltAndroidApp
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt              version=11，10 個 Migration，exportSchema=false
│   │   ├── dao/
│   │   │   ├── CategoryDao.kt
│   │   │   ├── NotificationLogDao.kt
│   │   │   ├── PendingEventDao.kt
│   │   │   ├── RegexPatternDao.kt
│   │   │   └── TransactionDao.kt
│   │   └── entity/
│   │       ├── CategoryEntity.kt
│   │       ├── NotificationLogEntity.kt
│   │       ├── PendingEventEntity.kt   + transactionType 欄位（v11 新增）
│   │       ├── RegexPatternEntity.kt
│   │       └── TransactionEntity.kt
│   ├── notification/
│   │   ├── AmountPatterns.kt           15 種 TWD regex
│   │   ├── ConfidenceBreakdown.kt
│   │   ├── DuplicateDetector.kt        WINDOW_MS=5000
│   │   ├── FilterResult.kt
│   │   ├── LineSenderAnalyzer.kt       ★ 重寫：title-first 辨識，防個人訊息誤判
│   │   ├── MerchantExtractor.kt
│   │   ├── NotificationCategory.kt
│   │   ├── NotificationSource.kt
│   │   ├── NotificationSourceRegistry.kt  40+ 台灣金融來源白名單
│   │   ├── ParserRegistry.kt           PARSE_VERSION=4，7 個 Parser
│   │   ├── ParseStatus.kt
│   │   ├── PatternType.kt
│   │   ├── RelevanceFilter.kt          支出 + 收入關鍵字
│   │   ├── UserPatternEngine.kt
│   │   └── parser/
│   │       ├── NotificationParserStrategy.kt
│   │       ├── IncomeParser.kt         ★ 新增：收入偵測（第一順位）
│   │       ├── TransferParser.kt
│   │       ├── LinePayParser.kt        title-only canHandle（已收緊）
│   │       ├── LineBankParser.kt
│   │       ├── CathayParser.kt
│   │       ├── CTBCParser.kt
│   │       ├── GenericBankParser.kt
│   │       └── GenericAmountParser.kt  ← 舊版，未使用（待移除）
│   └── repository/
│       ├── CategoryRepository.kt
│       ├── NotificationLogRepository.kt
│       ├── PendingEventRepository.kt   insertFromParsed 傳遞 transactionType
│       ├── RegexPatternRepository.kt
│       └── TransactionRepository.kt
├── di/
│   └── DatabaseModule.kt               10 個 Migration 鏈
├── domain/
│   └── model/
│       ├── Category.kt                 14 個預設類別（8 支出 + 6 收入）
│       ├── EventSource.kt
│       ├── ParsedEvent.kt              + transactionType（EXPENSE/INCOME）
│       ├── PendingEvent.kt             + transactionType（EXPENSE/INCOME）
│       └── Transaction.kt             TransactionType(EXPENSE/INCOME)
├── presentation/
│   ├── navigation/
│   │   ├── NavGraph.kt                 9 個路由
│   │   └── Screen.kt
│   └── screen/
│       ├── dashboard/
│       │   ├── DashboardScreen.kt      ★ 橫向長條圖（支出+收入），權限警告 Banner
│       │   └── DashboardViewModel.kt   incomeCategoryBreakdown，權限狀態偵測
│       ├── notificationdetail/
│       ├── notificationlog/
│       ├── patternlibrary/
│       ├── pending/
│       │   ├── PendingEventScreen.kt   ★ 收入/支出 Badge，對應類別 chips
│       │   └── PendingEventViewModel.kt  expenseCategories + incomeCategories
│       ├── regexbuilder/
│       ├── transaction/
│       │   ├── AddTransactionScreen.kt ★ 緊湊版面：日期+備注合一 Card
│       │   └── TransactionViewModel.kt
│       ├── trash/
│       └── theme/
└── service/
    ├── DebugNotificationSender.kt      ★ 9 個模板（4 支出 + 5 收入）隨機發送
    └── MoneyNotificationListenerService.kt
```

---

## 資料流

```
User Action → ViewModel.onEvent()
                    ↓
              Repository (suspend)
                    ↓
              Room DAO → Room DB (SQLite)

DB 變化 → DAO (Flow) → Repository (Flow) → ViewModel (StateFlow)
                                                  ↓
                                        UI collectAsStateWithLifecycle()
```

---

## 通知解析流程

```
onNotificationPosted(sbn)
  → title  = EXTRA_TITLE
  → text   = EXTRA_BIG_TEXT ?: EXTRA_TEXT    （text blank → return）
  → appLabel = resolveAppLabel(packageName)   （純顯示用，不參與解析）

  → RelevanceFilter.filter(packageName, title, text)
      LINE App: LineSenderAnalyzer.analyze(title, text)  ← title-first
      其他 App: NotificationSourceRegistry 白名單 / 金融關鍵字掃描
      isAllowed=false → NotificationLog(isFiltered=true)  STOP

  → ParserRegistry.parse(packageName, title, text)
      1. IncomeParser       — title/text 含收入關鍵字 → transactionType=INCOME
      2. TransferParser      — 轉帳詞組 → TRANSFER，不建 PendingEvent
      3. LinePayParser       — title contains "LINE Pay"（已收緊）
      4. LineBankParser      — packageName=com.linebank.tw
      5. CathayParser        — 國泰 packageName 或 title+text
      6. CTBCParser          — 中信 packageName 或 title+text
      7. GenericBankParser   — 通用支出關鍵字備援

  → DuplicateDetector（5 秒窗口）
  → NotificationLogRepository.insert()
  → shouldCreatePendingEvent → PendingEventRepository.insertFromParsed()
```

---

## LineSenderAnalyzer（Title-First 策略）

```
analyze(title, text):
  Step 1: title 含群組標記 → GROUP_CHAT，拒絕
  Step 2: title 在已知官方帳號名單內 → 金融（主要防線）
           50+ 個銀行、支付、證券官方帳號名稱
  Step 3: title 以「銀行」/「信用卡」結尾
           AND body 含明確金融訊號（消費金額、NT$、帳單）→ 接受
  Step 4: 其他 → UNKNOWN，拒絕（一般聊天、廣告）
```

**改動前 vs 後：**
- 前：`"$title $text"` 組合掃描 → 朋友訊息「我用 LINE Pay 付了 NT$500」被誤判
- 後：title 為主 → 朋友名字（小明）不符合任何官方帳號名稱 → 正確拒絕

---

## 收入偵測（IncomeParser）

支援關鍵字（27 個）：
> 入帳、存入、匯入、收款、收到轉帳、收到款項、薪資、薪水、發薪、工資、退款、退費、退還、現金回饋、回饋金、紅利、紅包、股利、配息、股息、利息、孳息、獎金、補助、津貼、轉入成功、匯款入帳

類別推薦：

| 關鍵字 | 建議類別 |
|--------|----------|
| 薪資、薪水、發薪、工資 | 薪資 (id=9) |
| 獎金、紅利、補助、津貼 | 獎金 (id=10) |
| 股利、配息、股息、利息 | 股票 (id=11) |
| 退款、退費、現金回饋 | 被動收入 (id=13) |
| 其他 | 其他收入 (id=14) |

---

## 資料庫 Schema 重點

### `pending_events` 新增欄位（v11）
| Column | Type | Default |
|--------|------|---------|
| `transactionType` | TEXT NOT NULL | `'EXPENSE'` |

### 遷移歷史（完整）

| 版本 | 異動內容 |
|------|----------|
| 1→2 | 建立 `notification_log` 表 |
| 2→3 | 三張表新增 `eventSource` |
| 3→4 | `transactions.deletedAt`（軟刪除） |
| 4→5 | `notification_log.parseStatus` |
| 5→6 | 建立 `regex_patterns` 表 |
| 6→7 | `notification_log` 新增 category/isFiltered/filteredReason/parseTrace |
| 7→8 | `notification_log` 新增 parseVersion/lastParsedAt/lineSenderType |
| 8→9 | `transactions.transactionType` (DEFAULT 'EXPENSE') |
| 9→10 | `categories.categoryType` + 插入收入類別 id 9–14 |
| 10→11 | `pending_events.transactionType` (DEFAULT 'EXPENSE') |

---

## 重要常數

```kotlin
// ParserRegistry.kt
const val PARSE_VERSION = 4

// DuplicateDetector.kt
const val WINDOW_MS = 5_000L

// TransactionViewModel.kt / 各地
const val DEFAULT_EXPENSE_CATEGORY_ID = 8L   // 其他（支出）
const val DEFAULT_INCOME_CATEGORY_ID  = 14L  // 其他收入

// NotificationLogDao.kt
trimToLimit() 保留最新 500 筆
```

---

## Release Build 設定

```kotlin
// app/build.gradle.kts
versionCode = 2
versionName = "1.0.1"

signingConfigs.release {
    // 讀自 keystore.properties（不進 git）
    storeFile = file("moneykeeper-release.jks")
}

splits.abi {
    include("arm64-v8a", "armeabi-v7a", "x86_64")
    isUniversalApk = true
}
```

APK 輸出：`app/build/outputs/apk/release/app-universal-release.apk`（~11 MB）

---

## 近期重大變更（自 CLAUDE_0602.md 起）

| 日期 | 類型 | 說明 |
|------|------|------|
| 2026-06-10 | fix | LineSenderAnalyzer 改為 title-first，防個人訊息誤判 |
| 2026-06-10 | fix | LinePayParser.canHandle() 收緊：只依 title 觸發 |
| 2026-06-09 | build | Release APK 設定（signing + ABI splits + gradlew） |
| 2026-06-09 | refactor | AddTransactionScreen：日期+備注合一 Card，不需滑動 |
| 2026-06-08 | feat | IncomeParser + transactionType（DB v11, PARSE_VERSION 4） |
| 2026-06-08 | feat | PendingEventScreen 收入/支出 Badge + 對應類別 chips |
| 2026-06-08 | feat | DebugNotificationSender：9 個模板（支出 4 + 收入 5） |
| 2026-06-08 | feat | DashboardScreen：橫向長條圖替代圓餅圖（支出+收入各一區） |
| 2026-06-08 | fix | 長條圖總和列固定不動，類別區各自獨立滑動（LazyColumn） |
| 2026-06-04 | fix | Service stopWithTask=false + 電池優化請求 + 權限警告 Banner |
| 2026-06-04 | feat | PendingEventScreen 類別 chip 直接選擇 |
| 2026-06-04 | refactor | AddTransactionScreen：Header 縮小，計算機 28%，備注移底部 |

---

## 已知問題 / 待確認

1. **`GenericAmountParser.kt`** — 未在 ParserRegistry 使用，舊版遺留，待確認移除
2. **`isMinifyEnabled = false`** — release build 未啟用混淆，上架前需測試
3. **無 Unit Test** — Parser 邏輯（純 Kotlin）是最高優先測試目標
4. **通知授權引導頁** — 已有 Banner 提示，但缺少完整 onboarding 流程

---

## 下一步建議

| 優先 | 項目 |
|------|------|
| P0 | 實際 LINE 銀行通知測試（驗證 LineSenderAnalyzer title 名單完整性） |
| P1 | 搜尋 / 篩選交易記錄 |
| P1 | 重新解析功能（批次重跑 parseVersion < 4 的記錄） |
| P2 | Parser Unit Tests |
| P2 | CSV 匯出 |
| P3 | 移除 GenericAmountParser.kt（技術債） |
