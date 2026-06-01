# MoneyKeeper — CLAUDE.md

AI 驅動的自動化記帳 Android App。
核心理念：自動偵測消費事件，最小化手動輸入。

## 專案基本資訊

- **Package:** `com.moneykeeper.app`
- **Min SDK:** 26 (Android 8.0)
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Architecture:** MVVM + Clean Architecture (single module)
- **DI:** Hilt
- **DB:** Room (SQLite, local-first, no cloud)
- **Async:** Kotlin Coroutines + Flow

## 不在 MVP 範圍內（不要加）

- 雲端同步 / 後端 server
- 使用者登入 / 帳號系統
- AI 模型（on-device 或 API）
- GPS / Geofencing
- 台灣電子發票載具
- 多幣別
- 帳戶管理（現金 vs 信用卡）

## 目錄結構

```
app/src/main/java/com/moneykeeper/app/
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt          — Room @Database，version=1，exportSchema=false
│   │   ├── dao/                    — TransactionDao, CategoryDao, PendingEventDao
│   │   └── entity/                 — Entity + toDomain() / toEntity() 轉換函數
│   ├── notification/
│   │   ├── AmountPatterns.kt       — TWD regex 解析，回傳 (amount, confidence)
│   │   ├── ParserRegistry.kt       — Strategy chain，@Singleton，Hilt inject
│   │   └── parser/                 — NotificationParserStrategy interface + 各銀行實作
│   └── repository/                 — TransactionRepository, PendingEventRepository, CategoryRepository
├── domain/
│   └── model/                      — 純 Kotlin data class：Transaction, Category, PendingEvent, ParsedEvent
├── presentation/
│   ├── navigation/                 — Screen (sealed class routes), NavGraph
│   ├── screen/
│   │   ├── dashboard/              — DashboardScreen + DashboardViewModel
│   │   ├── transaction/            — AddTransactionScreen + TransactionViewModel
│   │   └── pending/                — PendingEventScreen + PendingEventViewModel
│   └── theme/                      — Color.kt, Theme.kt (MoneyKeeperTheme)
├── di/
│   └── DatabaseModule.kt           — Room build + 8 種預設類別 seed
├── service/
│   └── MoneyNotificationListenerService.kt
├── MainActivity.kt                 — @AndroidEntryPoint, enableEdgeToEdge
└── MoneyKeeperApplication.kt       — @HiltAndroidApp
```

## 資料模型

### 核心 Tables

| Table | 用途 |
|-------|------|
| `transactions` | 已確認的消費記錄 |
| `pending_events` | 從通知解析出、待使用者確認的事件 |
| `categories` | 消費類別（8 種預設，id 1-8） |

### 重要 Entity 規則

- `TransactionEntity.categoryId` 為 nullable（FK ON DELETE SET_NULL）
- `PendingEventEntity.status` 存字串：`"PENDING"` / `"CONFIRMED"` / `"REJECTED"`
- Room 使用 camelCase 作為 column name（SQLite case-insensitive，查詢可不管大小寫）
- `exportSchema = false` — 不需配置 schema export 路徑
- 新增 schema 變更時，在 `AppDatabase` 建立 `Migration` 物件，不使用 `fallbackToDestructiveMigration()`

### 預設類別（id 固定）

```
1=飲食, 2=交通, 3=購物, 4=娛樂, 5=醫療, 6=住宿, 7=教育, 8=其他(default)
```

預設 categoryId 用 `8L` 作為 fallback，出現在 `TransactionViewModel`、`PendingEventViewModel`。

## 通知解析流程

```
onNotificationPosted(sbn)
  → 取 packageName, title, EXTRA_BIG_TEXT (fallback: EXTRA_TEXT)
  → ParserRegistry.parse(packageName, title, text)
      → LinePayParser (jp.naver.line.android + "LINE Pay"/"付款成功")
      → LineBankParser (com.linebank.tw)
      → GenericAmountParser (fallback，含金融關鍵字時觸發)
  → ParsedEvent? (confidence 0.0-1.0)
  → confidence < 0.4 → 丟棄
  → PendingEventRepository.insertFromParsed()
```

### 信心度規則

- `confidence >= 0.4` → 寫入 pending_events，等待使用者確認
- `confidence < 0.4` → 靜默丟棄
- 各 parser 可對信心度做微調（LinePayParser +0.1，GenericAmountParser ×0.8）
- 新增 parser 時加到 `ParserRegistry.parsers` list，**`GenericAmountParser` 必須放最後**

### 新增銀行 Parser

1. 建立 `data/notification/parser/XxxParser.kt`，實作 `NotificationParserStrategy`
2. 在 `ParserRegistry.parsers` list 中加入（在 `GenericAmountParser` 之前）
3. `canHandle()` 用 packageName 精確匹配優先，避免誤觸發

## MVVM 資料流

```
User Action → ViewModel.onEvent() → Repository → Room DAO
                                                      ↓
UI ← ViewModel.uiState (StateFlow) ← Repository (Flow)
```

- ViewModel 使用 `SharingStarted.WhileSubscribed(5_000)`
- Repository 直接注入 DAO（無 UseCase 層，MVP 簡化）
- Screen 使用 `collectAsStateWithLifecycle()`（非 `collectAsState()`）
- DB 寫入使用 `viewModelScope.launch { ... }`，不需指定 dispatcher（Room 內部處理）

## Hilt DI 規則

- `@HiltAndroidApp` → `MoneyKeeperApplication`
- `@AndroidEntryPoint` → `MainActivity`, `MoneyNotificationListenerService`
- `@HiltViewModel` → 所有 ViewModel
- `@Singleton` → 所有 Repository, `ParserRegistry`
- Repository 使用 `@Inject constructor`，Hilt 自動發現，不需 `@Provides`
- DAO / Database 在 `DatabaseModule` 手動 `@Provides`

## Notification Listener 注意事項

- `NotificationListenerService` 由 Android OS 系統綁定，不需 `startForeground()`
- 使用者必須在系統設定手動授權（`Settings → Notification access`），App 無法程式碼授權
- Service scope：`CoroutineScope(SupervisorJob() + Dispatchers.IO)`，在 `onDestroy()` cancel
- 不儲存完整通知文字到長期 table（隱私原則）

## 開發規範

### 程式碼風格

- 不寫說明性 comment，只在 WHY 不明顯時才加
- extension function (`toDomain()`, `toEntity()`) 定義在 entity 檔案底部，同檔管理
- 新增 Screen 時：建立對應 ViewModel，透過 `hiltViewModel()` 注入，在 `NavGraph` 加 route

### 版本管理

- 所有依賴版本統一在 `gradle/libs.versions.toml` 管理
- 新增依賴時先在 `[versions]` 加版本，`[libraries]` 加定義，再在 `app/build.gradle.kts` 引用

### 測試

目前無測試。新增功能時，parser 邏輯（`AmountPatterns`, 各 Parser）優先建立 unit test，
因為這是純 Kotlin，不依賴 Android，最易測試。

## 權限

| 權限 | 原因 |
|------|------|
| `POST_NOTIFICATIONS` | 未來發送確認提醒（API 33+） |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | 在 service 的 `android:permission` 屬性，非 uses-permission |

## 下一步擴充方向

- **新增銀行 parser**：實作 `NotificationParserStrategy`，加入 `ParserRegistry`
- **通知授權引導頁**：偵測 `NotificationListenerManager.isNotificationListenerAccessGranted()`，引導用戶開啟
- **服務狀態診斷**：Settings 頁顯示 Service 是否運作中
- **搜尋/篩選**：`TransactionDao` 加 query，`DashboardScreen` 加 filter UI
- **CSV 匯出**：讀取所有 transactions，用 `FileOutputStream` 寫出
