# MoneyKeeper — 專案快照 2026-06-02

> Claude Code 工作快照，用於跨 session 上下文延續。  
> 人工產生，反映當前程式碼實際狀況。

---

## 專案基本資訊

| 項目 | 值 |
|------|-----|
| Package | `com.moneykeeper.app` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| versionCode | 1 |
| versionName | `1.0` |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.03 (Material3 1.3.x) |
| Room DB 版本 | **10** |
| PARSE_VERSION | **3** |

---

## 目錄結構（完整）

```
app/src/main/java/com/moneykeeper/app/
├── MainActivity.kt                     @AndroidEntryPoint，enableEdgeToEdge
├── MoneyKeeperApplication.kt           @HiltAndroidApp
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt              version=10，9 個 Migration，exportSchema=false
│   │   ├── dao/
│   │   │   ├── CategoryDao.kt          getAll, getByType, getById, insertAll, count
│   │   │   ├── NotificationLogDao.kt   getAll/Financial/Ignored/Debug/RecentReal,
│   │   │   │                           getById, getFailedParsed, getOlderThanVersion,
│   │   │   │                           findRecentWithAmount, insert, update,
│   │   │   │                           deleteById, trimToLimit, deleteDebugData,
│   │   │   │                           deleteIgnored, clearAll
│   │   │   ├── PendingEventDao.kt      getPending, pendingRealCount, insertOrUpdate,
│   │   │   │                           updateStatus, deleteById
│   │   │   ├── RegexPatternDao.kt      getAll, getByType, getById, insert, update,
│   │   │   │                           deleteById
│   │   │   └── TransactionDao.kt       getAll, getByDateRange, getRealByDateRange,
│   │   │                               getById, sumByDateRange, sumRealByDateRange,
│   │   │                               sumExpenseRealByDateRange,
│   │   │                               sumIncomeRealByDateRange,
│   │   │                               insert, update, softDelete, restore,
│   │   │                               getTrash, getTrashCount, cleanOldTrash,
│   │   │                               emptyTrash, deleteDebugData
│   │   └── entity/
│   │       ├── CategoryEntity.kt       id,name,icon,colorHex,isSystem,sortOrder,categoryType
│   │       ├── NotificationLogEntity.kt  ← 20 個欄位，詳見 docs/database-schema.md
│   │       ├── PendingEventEntity.kt   + toDomain() / toEntity()
│   │       ├── RegexPatternEntity.kt
│   │       └── TransactionEntity.kt    + toDomain() / toEntity()
│   ├── notification/
│   │   ├── AmountPatterns.kt           15 種 TWD regex，parseAmount + parseAmountWithDetail
│   │   ├── ConfidenceBreakdown.kt      ConfidenceSignal(label,points,category,matchedText)
│   │   │                               三個 SignalCategory: RELEVANCE/EXTRACTION/CONTEXT
│   │   ├── DuplicateDetector.kt        WINDOW_MS=5000，check(amount,packageName)
│   │   ├── FilterResult.kt             isAllowed,category,ignoredReason,lineSenderType
│   │   ├── LineSenderAnalyzer.kt       BANK/PAYMENT/SECURITIES/UNKNOWN SenderType
│   │   ├── MerchantExtractor.kt        extract + extractWithDetail (MerchantMatchResult)
│   │   ├── NotificationCategory.kt     FINANCIAL/SOCIAL/PROMOTIONAL/SYSTEM/UNKNOWN
│   │   ├── NotificationSource.kt       data class(packageName,label,category,isWhitelisted)
│   │   ├── NotificationSourceRegistry.kt  40+ 台灣金融來源白名單
│   │   ├── ParserRegistry.kt           PARSE_VERSION=3，Strategy Chain
│   │   ├── ParseStatus.kt              8 個狀態，shouldCreatePendingEvent
│   │   ├── PatternType.kt              AMOUNT / MERCHANT
│   │   ├── RelevanceFilter.kt          白名單 + 15 個金融關鍵字
│   │   ├── UserPatternEngine.kt        @Singleton，載入 DB Regex，tryMatchAmount/Merchant
│   │   └── parser/
│   │       ├── NotificationParserStrategy.kt  介面：name,canHandle(),parse()
│   │       ├── TransferParser.kt       轉帳攔截（第一順位）
│   │       ├── LinePayParser.kt        jp.naver.line.android + LINE Pay
│   │       ├── LineBankParser.kt       com.linebank.tw
│   │       ├── CathayParser.kt         com.cathaybk.cube / .android
│   │       ├── CTBCParser.kt           com.ctbcbank.mobile / .tw
│   │       ├── GenericBankParser.kt    通用備援（最後）
│   │       └── GenericAmountParser.kt  ← 舊版，目前未使用（待確認是否移除）
│   └── repository/
│       ├── CategoryRepository.kt       getAll, getByType, getById
│       ├── NotificationLogRepository.kt  getAll/Financial/Ignored/Debug/RecentReal,
│       │                               getById,getFailedParsed,getOlderThanVersion,
│       │                               findRecentWithAmount,insert,update,deleteById,
│       │                               clearDebugData,clearIgnored,clearAll
│       ├── PendingEventRepository.kt   getPending, pendingCount, insertFromParsed,
│       │                               confirm, reject, deleteById
│       ├── RegexPatternRepository.kt   getAll,getByType,getById,insert,update,deleteById
│       └── TransactionRepository.kt    getAll,getByDateRange,getRealByDateRange,
│                                       getById,sumBy/sumRealBy/sumExpense/sumIncome,
│                                       insert,update,delete,restore,getTrash,
│                                       getTrashCount,cleanOldTrash,emptyTrash,
│                                       clearDebugData
├── di/
│   └── DatabaseModule.kt               provideDatabase + 5 個 DAO @Provides
├── domain/
│   └── model/
│       ├── Category.kt                 categoryType,DefaultExpenseCategories(8),
│       │                               DefaultIncomeCategories(6),DefaultCategories(14)
│       ├── EventSource.kt              MANUAL_INPUT/REAL_NOTIFICATION/DEBUG_*
│       ├── ParsedEvent.kt              amount,currency,merchant,categoryId,confidence,...
│       ├── PendingEvent.kt             + PendingEventStatus(PENDING/CONFIRMED/REJECTED)
│       └── Transaction.kt              + TransactionSource(MANUAL/NOTIFICATION)
│                                       + TransactionType(EXPENSE/INCOME)
├── presentation/
│   ├── navigation/
│   │   ├── NavGraph.kt                 9 個路由
│   │   └── Screen.kt                   Dashboard,AddTransaction,EditTransaction,
│   │                                   PendingEvents,NotificationLog,Trash,
│   │                                   PatternLibrary,RegexBuilder,NotificationDetail
│   └── screen/
│       ├── dashboard/
│       │   ├── DashboardScreen.kt      4:6 分割，上方總結/下方細項
│       │   │                           CompactPieChart,MonthYearPickerDialog
│       │   └── DashboardViewModel.kt   CategoryBreakdown,DashboardUiState,
│       │                               onDateSelected(yearMonth),buildBreakdown()
│       ├── notificationdetail/
│       │   ├── NotificationDetailScreen.kt  長按複製,複製 Debug 報告
│       │   └── NotificationDetailViewModel.kt
│       ├── notificationlog/
│       │   ├── NotificationLogScreen.kt   Tab(金融/已過濾/Debug)
│       │   └── NotificationLogViewModel.kt
│       ├── patternlibrary/
│       │   ├── PatternLibraryScreen.kt
│       │   └── PatternLibraryViewModel.kt
│       ├── pending/
│       │   ├── PendingEventScreen.kt
│       │   └── PendingEventViewModel.kt
│       ├── regexbuilder/
│       │   ├── RegexBuilderScreen.kt
│       │   └── RegexBuilderViewModel.kt
│       ├── transaction/
│       │   ├── AddTransactionScreen.kt    TypeToggle pill,彩色金額顯示,CategoryChip,
│       │   │                              支出/收入模式,編輯模式
│       │   └── TransactionViewModel.kt    expenseCategories,incomeCategories StateFlow,
│       │                                  SavedStateHandle(transactionId),
│       │                                  onTransactionTypeChanged() 自動切換類別
│       ├── trash/
│       │   ├── TrashScreen.kt
│       │   └── TrashViewModel.kt
│       └── theme/
│           ├── Color.kt
│           └── Theme.kt                  MoneyKeeperTheme
└── service/
    ├── DebugNotificationSender.kt        模擬通知（測試用）
    └── MoneyNotificationListenerService.kt  @AndroidEntryPoint，注入 5 個依賴，
                                            onNotificationPosted → Filter → Parse →
                                            DuplicateDetector → Log → PendingEvent

```

---

## 資料流

```
User Action → ViewModel.onEvent()
                    ↓
              Repository (suspend)
                    ↓
              Room DAO
                    ↓
              Room DB (SQLite)

DB 變化 → DAO (Flow) → Repository (Flow) → ViewModel (StateFlow)
                                                  ↓
                                        UI collectAsStateWithLifecycle()
```

---

## 已知問題 / 待確認

1. **`GenericAmountParser.kt`** 存在於 parser/ 目錄但未在 ParserRegistry 中使用，可能是舊版遺留，待確認是否可刪除。
2. **`isMinifyEnabled = false`** — release build 未啟用混淆，上架前需修正並測試 Hilt/Room 是否正常。
3. **無任何 Unit Test** — Parser 邏輯（純 Kotlin）是最高優先的測試目標。
4. **通知授權引導頁未實作** — 使用者首次安裝後沒有引導，體驗不完整。

---

## Hilt DI 注意事項

- `@HiltAndroidApp` → `MoneyKeeperApplication`
- `@AndroidEntryPoint` → `MainActivity`、`MoneyNotificationListenerService`
- `@HiltViewModel` → 所有 ViewModel（含 `SavedStateHandle` 注入）
- `@Singleton` → 所有 Repository、`ParserRegistry`、`UserPatternEngine`、`DuplicateDetector`、`RelevanceFilter`、`NotificationSourceRegistry`
- Repository 使用 `@Inject constructor`，DAO 在 `DatabaseModule` 手動 `@Provides`

---

## 重要常數

```kotlin
// ParserRegistry.kt
const val PARSE_VERSION = 3

// DuplicateDetector.kt
const val WINDOW_MS = 5_000L  // 重複偵測窗口

// TransactionViewModel.kt / 各地
const val DEFAULT_EXPENSE_CATEGORY_ID = 8L   // 其他（支出）
const val DEFAULT_INCOME_CATEGORY_ID  = 14L  // 其他收入

// NotificationLogDao.kt
trimToLimit() 保留最新 500 筆

// TransactionDao.kt（隱性）
cleanOldTrash(cutoff) 刪除 cutoff 之前軟刪除的記錄
```

---

## 近期重大變更（本 session）

| 日期 | 類型 | 說明 |
|------|------|------|
| 2026-06-02 | feat | 6 個收入類別（DB v10, MIGRATION_9_10） |
| 2026-06-02 | feat | 主頁 4:6 分割版面 |
| 2026-06-02 | refactor | AddTransactionScreen 全面重設計（pill toggle、彩色金額） |
| 2026-06-02 | docs | .gitignore、README、CHANGELOG、docs/ 目錄建立 |
| 2026-05-31 | feat | 收入/支出支援（DB v9, transactionType） |
| 2026-05-31 | feat | 支出圓餅圖、月/年日期選擇器、編輯現有記錄 |
| 2026-05-30 | feat | 重複通知偵測（DuplicateDetector, DB v8） |

---

## 下一步建議

優先順序：
1. 通知授權引導頁（P0，影響首次使用體驗）
2. Service 狀態指示（P0，使用者無法知道是否運作中）
3. 移除或整合 `GenericAmountParser.kt`（技術債）
4. Parser Unit Tests（長期品質保證）
5. 重新解析功能（通知日誌批次重跑）
