# Changelog

所有重要的版本變更都記錄在此文件中。  
格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.0.0/)。

---

## [Unreleased]

### 計劃中
- 通知授權引導頁（偵測 `isNotificationListenerAccessGranted`）
- CSV 匯出
- 搜尋 / 篩選交易記錄
- 更多銀行 Parser（台灣銀行、華南、彰化）

---

## [0.9.0] — 2026-06-02 (DB v10, PARSE_VERSION 3)

### Added
- **收入類別**：新增 6 個系統收入類別（薪資、獎金、股票、基金、被動收入、其他收入），id 9–14
- `CategoryEntity.categoryType` 欄位（EXPENSE / INCOME），DB 遷移 9→10
- `CategoryDao.getByType()` 依類型查詢
- `TransactionViewModel` 自動切換對應類別（支出↔收入）

### Changed
- **主頁面 4:6 分割**：上方總結區（固定）/ 下方細項區（可滑動）
  - 上方：月份標題、收支摘要卡片、圓環圓餅圖
  - 下方：日期導航 + 交易列表
- **新增/編輯頁面重新設計**：
  - 自訂 pill 切換（支出 / 收入）
  - 彩色金額顯示區（支出=紅、收入=綠）
  - TopAppBar 配色隨類型切換
  - 類別 Chip 顯示彩色圓點
  - 按鈕圓角、儲存按鈕配色

---

## [0.8.0] — 2026-05-31 (DB v9)

### Added
- **收入/支出支援**：`Transaction.transactionType` (EXPENSE / INCOME)
- `TransactionEntity.transactionType` 欄位，DB 遷移 8→9（DEFAULT 'EXPENSE'）
- 主頁面收支摘要（本月支出 / 本月收入 / 本月淨收支）
- 主頁面交易行顯示 +/- 符號與顏色
- **支出圓餅圖**：本月支出類別比例（Canvas `drawArc` 環形）
- **月/年日期選擇器**：點擊日期開啟自訂對話框（月份 Grid → 年份 Grid）
- **編輯現有記錄**：點選記錄列進入編輯模式（同新增頁面、預填資料）
  - `Screen.EditTransaction("edit_transaction/{transactionId}")`
  - `TransactionViewModel` 使用 `SavedStateHandle` 載入現有資料
- `TransactionRepository.getById()`、`update()`

---

## [0.7.0] — 2026-05-30 (DB v8, PARSE_VERSION 3)

### Added
- **重複通知偵測**：`DuplicateDetector`（5 秒窗口，同/跨 App 均偵測）
- `ParseStatus.DUPLICATE` — 重複通知不建立 `PendingEvent`
- `NotificationLogDao.findRecentWithAmount()` 查詢
- 通知日誌中重複通知顯示特殊標籤

### Changed
- `ParserRegistry.PARSE_VERSION` 升至 3
- DB 遷移 7→8：`notification_log` 新增 `parseVersion`、`lastParsedAt`、`lineSenderType`

---

## [0.6.0] — 2026-05-29

### Added
- **Pattern Match 詳細追蹤**：`AmountPatterns.parseAmountWithDetail()` 回傳 `AmountMatchResult`
- `MerchantExtractor.extractWithDetail()` 回傳 `MerchantMatchResult`
- `ConfidenceSignal.matchedText` 顯示匹配的原始文字
- **複製功能**：通知詳情頁長按複製 body、一鍵複製完整 Debug 報告
- **UserPatternEngine**：從 DB 載入使用者自訂 Regex，Parser 失敗時嘗試救援解析
- **台灣金融來源擴充**：`NotificationSourceRegistry` 涵蓋 40+ 銀行/支付平台
- `LineSenderAnalyzer.SECURITIES` SenderType，擴充證券類關鍵字

### Changed
- `ParserRegistry` 注入 `UserPatternEngine`，`appendUserPatternTrace()` 輸出嘗試記錄
- `ConfidenceBreakdown.format()` 顯示 `«matchedText»`

---

## [0.5.0] — 2026-05-27 (DB v7)

### Added
- **Regex Builder 頁面**：從通知日誌建立自訂解析規則
- **Pattern Library 頁面**：管理使用者自訂 Regex 規則
- `regex_patterns` 資料表（DB 遷移 5→6）
- `RegexPatternDao`、`RegexPatternRepository`
- **通知詳情頁面**：完整 parse trace、信心分解、原始文字
- `ParseStatus` 重命名與擴充（PARSED_EXPENSE → HIGH/MEDIUM/LOW_CONFIDENCE 等）
- **三維信心評分**：RELEVANCE / EXTRACTION / CONTEXT 各自計分
- `ConfidenceBreakdown`、`ConfidenceSignal` 結構
- `LineSenderAnalyzer`：LINE 訊息來源分析（銀行 / 支付 / 社群 / 廣告）

### Changed
- `notification_log` 新增 `category`、`isFiltered`、`filteredReason`、`parseTrace`（DB 遷移 6→7）
- `notification_log` 新增 `parseVersion`、`lastParsedAt`、`lineSenderType`（DB 遷移 7→8）

---

## [0.4.0] — 2026-05-25 (DB v6)

### Added
- **通知日誌頁面**：Tab 切換（金融 / 已過濾 / Debug）
- `NotificationSourceRegistry`：台灣銀行白名單
- `RelevanceFilter`：白名單 + 金融關鍵字過濾，過濾結果也寫入 DB
- `NotificationCategory` enum（FINANCIAL / SOCIAL / PROMOTIONAL / SYSTEM / UNKNOWN）
- `ParseStatus` 多狀態支援

### Changed
- `notification_log` 新增過濾相關欄位（DB 遷移 5→6）

---

## [0.3.0] — 2026-05-23 (DB v5)

### Added
- **CathayParser**：國泰世華 CUBE App 通知解析
- **CTBCParser**：中國信託通知解析
- **GenericBankParser**：通用銀行備援解析
- **TransferParser**：轉帳通知專用解析器（先於支出類解析）
- `MerchantExtractor`：商家名稱萃取
- `DebugNotificationSender`：發送模擬通知用於測試
- 軟刪除（`deletedAt`）+ 垃圾桶頁面（DB 遷移 3→4）

### Changed
- `ParserRegistry` 重構為 Strategy Chain，引入 `NotificationParserStrategy` 介面

---

## [0.2.0] — 2026-05-20 (DB v3)

### Added
- **LinePayParser**：LINE Pay 消費通知解析
- **LineBankParser**：LINE Bank 消費通知解析
- `AmountPatterns`：TWD 金額 regex 萃取（15 種模式）
- **待確認消費頁面** (`pending_events` table)：確認 / 拒絕
- `PendingEventRepository.insertFromParsed()`
- `EventSource` enum（MANUAL_INPUT / REAL_NOTIFICATION / DEBUG_*）
- DB 遷移 2→3：`eventSource` 欄位

---

## [0.1.0] — 2026-05-15 (DB v2)

### Added
- 專案初始化（Kotlin + Jetpack Compose + Material3 + Hilt + Room）
- **手動記帳**：計算機介面新增消費
- 8 個預設支出類別（飲食、交通、購物、娛樂、醫療、住宿、教育、其他）
- `transactions` / `categories` 資料表
- **Dashboard**：本月總覽、日期導航、交易列表
- `notification_log` 資料表（DB 遷移 1→2）
- `MoneyNotificationListenerService` 基本框架
- Hilt DI 初始化、`DatabaseModule` 預設類別 seed
