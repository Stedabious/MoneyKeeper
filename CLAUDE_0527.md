# MoneyKeeper — CLAUDE_0527.md

AI 驅動的自動化記帳 Android App。
核心理念：自動偵測消費事件，最小化手動輸入。

**此文件更新於 2026-05-27，反映今日所有架構變更。**

---

## 專案基本資訊

- **Package:** `com.moneykeeper.app`
- **Min SDK:** 26 (Android 8.0)
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Architecture:** MVVM + Clean Architecture (single module)
- **DI:** Hilt
- **DB:** Room (SQLite, local-first, no cloud)，**目前版本 v3**
- **Async:** Kotlin Coroutines + Flow

## 不在 MVP 範圍內（不要加）

- 雲端同步 / 後端 server
- 使用者登入 / 帳號系統
- AI 模型（on-device 或 API）
- GPS / Geofencing
- 台灣電子發票載具
- 多幣別
- 帳戶管理（現金 vs 信用卡）

---

## 目錄結構（完整現況）

```
app/src/main/java/com/moneykeeper/app/
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt          — Room @Database，version=3，exportSchema=false
│   │   │                             含 MIGRATION_1_2、MIGRATION_2_3
│   │   ├── dao/
│   │   │   ├── TransactionDao.kt   — getAll / getByDateRange / getRealByDateRange
│   │   │   │                         sumByDateRange / sumRealByDateRange / deleteDebugData
│   │   │   ├── CategoryDao.kt      — getAll / getById / insertAll / count
│   │   │   ├── PendingEventDao.kt  — getPending / getPendingReal / pendingCount / pendingRealCount
│   │   │   │                         insert / updateStatus / deleteDebugData
│   │   │   └── NotificationLogDao.kt — getRecent / getRecentReal / insert / trimToLimit
│   │   │                               deleteDebugData / clearAll
│   │   └── entity/
│   │       ├── TransactionEntity.kt    — + eventSource: String
│   │       ├── CategoryEntity.kt
│   │       ├── PendingEventEntity.kt   — + eventSource: String
│   │       └── NotificationLogEntity.kt — 新，raw 通知 log + eventSource
│   ├── notification/
│   │   ├── AmountPatterns.kt       — TWD regex，回傳 (amount, confidence)
│   │   ├── ParserRegistry.kt       — 回傳 ParseResult(event, parserName)，@Singleton
│   │   └── parser/
│   │       ├── NotificationParserStrategy.kt — interface，含 val name: String default impl
│   │       ├── LinePayParser.kt    — jp.naver.line.android，LINE Pay
│   │       ├── LineBankParser.kt   — com.linebank.tw
│   │       ├── CathayParser.kt     — 國泰世華，com.cathaybk.android
│   │       ├── CTBCParser.kt       — 中國信託，com.ctbc.android 系列
│   │       └── GenericAmountParser.kt — fallback，必須放最後
│   └── repository/
│       ├── TransactionRepository.kt     — + getRealByDateRange / sumRealByDateRange / clearDebugData
│       ├── CategoryRepository.kt
│       ├── PendingEventRepository.kt    — insertFromParsed 攜帶 eventSource / clearDebugData
│       └── NotificationLogRepository.kt — 新，getRecent / getRecentReal / clearDebugData / clearAll
├── domain/
│   └── model/
│       ├── EventSource.kt          — 新，可擴充的 source type enum（含 metadata）
│       ├── Transaction.kt          — + eventSource: EventSource
│       ├── Category.kt             + DefaultCategories (id 1-8)
│       ├── PendingEvent.kt         — + eventSource: EventSource
│       └── ParsedEvent.kt          — + eventSource: EventSource
├── presentation/
│   ├── navigation/
│   │   ├── Screen.kt               — Dashboard / AddTransaction / PendingEvents / NotificationLog
│   │   └── NavGraph.kt             — 4 routes
│   ├── screen/
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt  — 日期導航 / 刪除記錄 / BugReport 按鈕
│   │   │   └── DashboardViewModel.kt — 改用 getRealByDateRange / sumRealByDateRange
│   │   ├── transaction/
│   │   │   ├── AddTransactionScreen.kt — 數字鍵盤 / 計算功能 / 日期選擇
│   │   │   └── TransactionViewModel.kt — 金額 numpad 邏輯 / eventSource=MANUAL_INPUT
│   │   ├── pending/
│   │   │   ├── PendingEventScreen.kt
│   │   │   └── PendingEventViewModel.kt — confirm 時建立 Transaction
│   │   └── notificationlog/
│   │       ├── NotificationLogScreen.kt  — 新，通知記錄 + Debug 工具面板
│   │       └── NotificationLogViewModel.kt — 新，injectDebugNotification / clearDebugData
│   └── theme/
│       ├── Color.kt
│       └── Theme.kt (MoneyKeeperTheme)
├── di/
│   └── DatabaseModule.kt           — 含兩個 migration，provide 4 個 DAO
├── service/
│   └── MoneyNotificationListenerService.kt — 記錄 raw log + parse + debug logging
├── MainActivity.kt                 — @AndroidEntryPoint, enableEdgeToEdge
└── MoneyKeeperApplication.kt       — @HiltAndroidApp
```

---

## EventSource 架構（2026-05-27 新增）

### 核心 enum

```kotlin
enum class EventSource(
    val isDebug: Boolean,
    val countedInStats: Boolean,
    val displayLabel: String,
) {
    REAL_NOTIFICATION(false, true,  "通知"),
    DEBUG_NOTIFICATION(true,  false, "[測試] 通知"),
    MANUAL_INPUT(      false, true,  "手動"),
    DEBUG_MANUAL(      true,  false, "[測試] 手動"),
    INVOICE_IMPORT(    false, true,  "發票匯入"),
}
```

### 命名慣例（關鍵設計）

所有 debug source 以 `DEBUG_` 開頭，SQL 可一律使用：

```sql
WHERE eventSource NOT LIKE 'DEBUG_%'
```

未來新增 debug source 只需在 enum 加入 `DEBUG_XXX`，無需修改任何 DAO。

### 各層傳遞路徑

```
Parser → ParsedEvent.eventSource (default: REAL_NOTIFICATION)
       → PendingEventRepository.insertFromParsed() → PendingEventEntity.eventSource
       → PendingEventViewModel.confirm() → Transaction.eventSource (需同步)

Service (debug inject) → ParsedEvent(eventSource=DEBUG_NOTIFICATION)
                       → 同上路徑，但攜帶 DEBUG 標記全程

TransactionViewModel.save() → Transaction(eventSource=MANUAL_INPUT)
```

### 統計隔離

| Query | 說明 |
|-------|------|
| `sumRealByDateRange` | 月統計 — 排除所有 DEBUG_ |
| `getRealByDateRange` | Dashboard 清單 — 排除所有 DEBUG_ |
| `pendingRealCount` | Notification badge — 排除 DEBUG_ |
| `deleteDebugData` | 清除 — `WHERE eventSource LIKE 'DEBUG_%'` |

---

## 資料模型

### 核心 Tables（DB v3）

| Table | 用途 |
|-------|------|
| `transactions` | 已確認的消費記錄 |
| `pending_events` | 從通知解析出、待使用者確認的事件 |
| `categories` | 消費類別（8 種預設，id 1-8） |
| `notification_log` | **新** 原始通知 log，含 parse 結果與 eventSource |

### Entity 欄位重點

**TransactionEntity** (`transactions`)
- `source: String` — `MANUAL` / `NOTIFICATION`（保留舊欄位）
- `eventSource: String` — `EventSource.name`，統計 filter 依此欄

**PendingEventEntity** (`pending_events`)
- `status: String` — `PENDING` / `CONFIRMED` / `REJECTED`
- `eventSource: String` — 從 `ParsedEvent` 攜帶

**NotificationLogEntity** (`notification_log`)
- `packageName`, `appLabel`, `title`, `body`, `timestamp`
- `parsedAmount: Double?`, `parserName: String?`, `confidence: Float?`
- `eventSource: String` — `REAL_NOTIFICATION` / `DEBUG_NOTIFICATION`
- `createdAt: Long`
- 自動 trim，保留最新 500 筆

### DB Migrations

```
v1 → v2 (MIGRATION_1_2): CREATE TABLE notification_log (無 eventSource)
v2 → v3 (MIGRATION_2_3): ALTER TABLE 三個 table ADD COLUMN eventSource
                          UPDATE transactions SET eventSource='MANUAL_INPUT' WHERE source='MANUAL'
```

### 預設類別（id 固定）

```
1=飲食, 2=交通, 3=購物, 4=娛樂, 5=醫療, 6=住宿, 7=教育, 8=其他(default)
```

預設 categoryId fallback 用 `8L`。

---

## 通知解析流程

```
onNotificationPosted(sbn)
  → packageName, title, EXTRA_BIG_TEXT (fallback: EXTRA_TEXT), postTime
  → resolveAppLabel(packageName)   // try-catch，fallback 用 packageName
  → parserRegistry.parse() → ParseResult?(event, parserName)
  → Log.d("MoneyNLS", ...)
  → notificationLogRepository.insert(NotificationLogEntity + eventSource)
  → if confidence >= 0.4 → pendingEventRepository.insertFromParsed(event)
```

### Parser chain（順序固定）

```
LinePayParser      — jp.naver.line.android + "LINE Pay"/"付款成功"
LineBankParser     — com.linebank.tw / linebank.*
CathayParser       — com.cathaybk.android / 國泰 + 消費關鍵字
CTBCParser         — com.ctbc.* / ctbc.* / 中國信託 + 消費關鍵字
GenericAmountParser — fallback，金融關鍵字觸發，confidence × 0.8
```

### Parser 擴充規則

1. 建 `XxxParser.kt`，實作 `NotificationParserStrategy`
2. `val name` 自動繼承 class simple name（不需 override）
3. 加入 `ParserRegistry.parsers`，在 `GenericAmountParser` 之前
4. `canHandle()` 優先用 packageName 精確匹配，再用關鍵字輔助

### 信心度規則

| 條件 | 動作 |
|------|------|
| `confidence >= 0.4` | 寫入 pending_events |
| `confidence < 0.4` | 靜默丟棄（仍寫 notification_log） |
| LinePayParser | +0.1 bonus |
| CathayParser / CTBCParser | +0.15 bonus |
| GenericAmountParser | × 0.8 penalty |

### 可取得 / 不可取得範圍

| 來源 | 通常可取得 | 可能無法取得 |
|------|----------|------------|
| LINE Pay | 金額、商家 | 鎖屏時內容 |
| LINE Bank | 金額、交易類型 | 加密欄位 |
| 國泰世華 | 金額、商家（部分） | VISIBILITY_SECRET |
| 中國信託 | 金額、類型 | 鎖屏遮蔽 |
| 一般銀行 | 視格式而定 | 加密通知 |

---

## UI 畫面清單

### Dashboard（主頁面）
- 本月支出卡片（`sumRealByDateRange`，排除 DEBUG）
- 日期導航列（`← 今天 / YYYY/MM/DD →`，最多到今天）
- 每日交易清單（`getRealByDateRange`，排除 DEBUG，顯示 `HH:mm`）
- 每筆右側垃圾桶刪除
- TopAppBar：BugReport icon（→ NotificationLog）、Notification badge（→ PendingEvents）

### 新增消費（AddTransactionScreen）
- 頂部大字金額顯示（右對齊，pending operator 顯示）
- 中間可捲動：商家、備注、類別 chips、日期選擇（DatePickerDialog）
- 底部數字鍵盤：`7 8 9 ⌫ / 4 5 6 + / 1 2 3 - / . 0 00 =`
- 支援加減計算，儲存前取最終金額

### 待確認消費（PendingEventScreen）
- 只顯示 `status=PENDING` + `eventSource NOT LIKE 'DEBUG_%'`
- 每筆可確認（→ Transaction）或拒絕

### 通知記錄（NotificationLogScreen）
- Debug 工具面板（頂部 Card）：「注入測試通知」、「清除 Debug 資料」
- 清單：全部 log（含 DEBUG），依 `createdAt DESC`
- DEBUG entry 顯示 tertiary 色系 + `[測試] XXX` chip
- 已解析 entry 顯示：NT$ 金額 chip、信心度 chip、Parser 名稱 chip
- 右上角清除全部按鈕

---

## MVVM 資料流

```
User Action → ViewModel.onEvent() → Repository → Room DAO
                                                      ↓
UI ← ViewModel.uiState (StateFlow) ← Repository (Flow)
```

- ViewModel 使用 `SharingStarted.WhileSubscribed(5_000)`
- Repository 直接注入 DAO（無 UseCase 層）
- Screen 使用 `collectAsStateWithLifecycle()`（非 `collectAsState()`）
- DB 寫入使用 `viewModelScope.launch { ... }`

---

## Hilt DI 規則

- `@HiltAndroidApp` → `MoneyKeeperApplication`
- `@AndroidEntryPoint` → `MainActivity`, `MoneyNotificationListenerService`
- `@HiltViewModel` → 所有 ViewModel
- `@Singleton` → 所有 Repository, `ParserRegistry`
- Repository 使用 `@Inject constructor`，Hilt 自動發現
- DAO / Database 在 `DatabaseModule` 手動 `@Provides`

---

## NotificationListenerService 注意事項

- 由 Android OS 系統綁定，不需 `startForeground()`
- 使用者必須手動授權（`Settings → 通知存取`），App 無法程式碼授權
- 已實作 `onListenerConnected()` / `onListenerDisconnected()` debug logging
- Service scope：`CoroutineScope(SupervisorJob() + Dispatchers.IO)`
- Android 11+ Package Visibility：`resolveAppLabel()` 包 try-catch，fallback 用 packageName
- 不儲存完整通知文字到 `transactions`（隱私原則），只存 `notification_log`
- Debug TAG：`"MoneyNLS"`

---

## 開發規範

### 程式碼風格
- 不寫說明性 comment，只在 WHY 不明顯時才加
- extension function (`toDomain()`, `toEntity()`) 定義在 entity 檔案底部，同檔管理
- 新增 Screen 時：建立對應 ViewModel，透過 `hiltViewModel()` 注入，在 `NavGraph` 加 route

### EventSource 規範
- 所有 debug source 必須以 `DEBUG_` 開頭（命名慣例 = SQL filter 基礎）
- 新增 source 只需在 `EventSource.kt` 加 enum，其餘 filter query 自動涵蓋
- `EventSource.fromName(name, fallback)` 做 safe parsing，避免 crash

### 版本管理
- 所有依賴版本統一在 `gradle/libs.versions.toml`
- DB schema 變更：在 `AppDatabase` 建立 `MIGRATION_X_Y`，加到 `DatabaseModule.addMigrations()`
- 永遠不使用 `fallbackToDestructiveMigration()`

### 測試
- 目前無測試
- Parser 邏輯（`AmountPatterns`、各 Parser）最易建 unit test（純 Kotlin，不依賴 Android）
- `EventSource` enum 邏輯可直接 unit test

---

## 已知待修 (Known Issues)

1. **`PendingEventViewModel.confirm()`** 建立 Transaction 時未攜帶 `eventSource`，應從 `event.eventSource` 傳入，否則 confirmed 的 notification 消費會被 `REAL_NOTIFICATION` 以外的邏輯當成 `MANUAL_INPUT`
2. **CathayParser / CTBCParser** package name 未經真實裝置驗證，可能需依實際 App 調整
3. **Notification badge** 目前用 `pendingRealCount` 排除 DEBUG，但若想測試 pending flow 需暫時改用 `pendingCount`

---

## 下一步擴充方向

### 短期
- **修 PendingEventViewModel.confirm()** 攜帶 `eventSource`（見 Known Issues #1）
- **通知授權引導頁**：偵測 `NotificationListenerManager.isNotificationListenerAccessGranted()`
- **服務狀態診斷**：Settings 頁顯示 Service 是否運作中

### 中期
- **新增銀行 Parser**：玉山、台新、富邦等，實作 `NotificationParserStrategy`
- **搜尋 / 篩選**：`TransactionDao` 加 query，`DashboardScreen` 加 filter UI
- **CSV 匯出**：讀取所有 real transactions，用 `FileOutputStream` 寫出

### 長期（EventSource 擴充）
- `INVOICE_IMPORT`：電子發票載具 API 匯入
- `FUTURE_EXTERNAL`：銀行 Open API 直接拉取
- 加入 `countedInStats = false` 的 source 時，所有統計 query 自動排除（命名慣例保障）

---

## 權限

| 權限 | 原因 |
|------|------|
| `POST_NOTIFICATIONS` | 未來發送確認提醒（API 33+） |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | 在 service 的 `android:permission` 屬性 |
