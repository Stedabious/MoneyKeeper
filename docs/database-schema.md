# Database Schema

**Engine:** SQLite (Room ORM)  
**Current Version:** 10  
**File:** `moneykeeper.db`  
**Export Schema:** false

---

## Tables

### 1. `transactions`

已確認的收支記錄。

| Column | Type | Nullable | Default | 說明 |
|--------|------|----------|---------|------|
| `id` | INTEGER PK | — | autoGenerate | 主鍵 |
| `amount` | REAL | NO | — | 金額（恆正，符號由 transactionType 決定） |
| `currency` | TEXT | NO | `'TWD'` | 幣別 |
| `categoryId` | INTEGER FK | YES | NULL | → `categories.id`（ON DELETE SET NULL） |
| `merchant` | TEXT | NO | `''` | 商家/來源名稱 |
| `note` | TEXT | NO | `''` | 備注 |
| `source` | TEXT | NO | — | `MANUAL` / `NOTIFICATION` |
| `eventSource` | TEXT | NO | `'MANUAL_INPUT'` | 細分來源，見 EventSource enum |
| `transactionType` | TEXT | NO | `'EXPENSE'` | `EXPENSE` / `INCOME` |
| `transactionDate` | INTEGER | NO | — | 交易日期（local start-of-day，ms） |
| `createdAt` | INTEGER | NO | — | 建立時間（ms） |
| `updatedAt` | INTEGER | NO | — | 最後更新時間（ms） |
| `deletedAt` | INTEGER | YES | NULL | 軟刪除時間，NULL = 未刪除 |

**Index:** `categoryId`

**預設類別 fallback:** categoryId = `8`（其他支出）、`14`（其他收入）

---

### 2. `categories`

消費類別，分為支出與收入兩種類型。

| Column | Type | Nullable | Default | 說明 |
|--------|------|----------|---------|------|
| `id` | INTEGER PK | — | 手動指定 | 主鍵（系統類別固定 1–14） |
| `name` | TEXT | NO | — | 類別名稱 |
| `icon` | TEXT | NO | — | Material icon name（字串） |
| `colorHex` | TEXT | NO | — | 顏色 `#RRGGBB` |
| `isSystem` | INTEGER | NO | `0` | 是否為系統預設 |
| `sortOrder` | INTEGER | NO | `0` | 排列順序 |
| `categoryType` | TEXT | NO | `'EXPENSE'` | `EXPENSE` / `INCOME` |

**預設類別（id 固定）：**

| id | name | categoryType | colorHex |
|----|------|--------------|----------|
| 1 | 飲食 | EXPENSE | #FF6B6B |
| 2 | 交通 | EXPENSE | #4ECDC4 |
| 3 | 購物 | EXPENSE | #45B7D1 |
| 4 | 娛樂 | EXPENSE | #96CEB4 |
| 5 | 醫療 | EXPENSE | #FFEAA7 |
| 6 | 住宿 | EXPENSE | #DDA0DD |
| 7 | 教育 | EXPENSE | #98D8C8 |
| 8 | 其他 | EXPENSE | #B0B0B0 |
| 9 | 薪資 | INCOME | #4CAF50 |
| 10 | 獎金 | INCOME | #FFC107 |
| 11 | 股票 | INCOME | #2196F3 |
| 12 | 基金 | INCOME | #9C27B0 |
| 13 | 被動收入 | INCOME | #00BCD4 |
| 14 | 其他收入 | INCOME | #78909C |

---

### 3. `pending_events`

從通知解析出、等待使用者確認的暫存事件。

| Column | Type | Nullable | Default | 說明 |
|--------|------|----------|---------|------|
| `id` | INTEGER PK | — | autoGenerate | 主鍵 |
| `amount` | REAL | YES | NULL | 解析出的金額 |
| `currency` | TEXT | NO | `'TWD'` | 幣別 |
| `merchant` | TEXT | YES | NULL | 商家名稱 |
| `categoryId` | INTEGER | YES | NULL | 預測類別 |
| `confidence` | REAL | NO | — | 解析信心度 0.0–1.0 |
| `rawSource` | TEXT | NO | — | 原始通知文字 |
| `sourcePackage` | TEXT | YES | NULL | 通知來源 packageName |
| `status` | TEXT | NO | `'PENDING'` | `PENDING` / `CONFIRMED` / `REJECTED` |
| `eventSource` | TEXT | NO | `'REAL_NOTIFICATION'` | 來源類型 |
| `eventTime` | INTEGER | NO | — | 通知時間（ms） |
| `createdAt` | INTEGER | NO | — | 建立時間（ms） |

**查詢方式（DAO）：**
- `getPending()` → status = PENDING，非 DEBUG，按時間倒序
- `pendingRealCount()` → PENDING 且非 DEBUG 的數量（用於 badge）

---

### 4. `notification_log`

所有通知的完整歷史記錄（含已過濾、未解析）。  
容量上限：500 筆（`trimToLimit()` 刪除最舊記錄）。

| Column | Type | Nullable | Default | 說明 |
|--------|------|----------|---------|------|
| `id` | INTEGER PK | — | autoGenerate | 主鍵 |
| `packageName` | TEXT | NO | — | 通知來源 packageName |
| `appLabel` | TEXT | NO | — | 應用程式顯示名稱 |
| `title` | TEXT | NO | — | 通知標題 |
| `body` | TEXT | NO | — | 通知內文 |
| `timestamp` | INTEGER | NO | — | 通知發布時間（ms） |
| `parsedAmount` | REAL | YES | NULL | 解析出的金額 |
| `parserName` | TEXT | YES | NULL | 成功解析的 parser 名稱 |
| `confidence` | REAL | YES | NULL | 信心度 0.0–1.0 |
| `eventSource` | TEXT | NO | `'REAL_NOTIFICATION'` | 來源類型 |
| `parseStatus` | TEXT | NO | `'UNPARSED'` | 解析狀態（見 ParseStatus） |
| `createdAt` | INTEGER | NO | — | 寫入 DB 時間（ms） |
| `category` | TEXT | NO | `'UNKNOWN'` | 通知分類（DB v6+） |
| `isFiltered` | INTEGER | NO | `0` | 是否被過濾器攔截 |
| `filteredReason` | TEXT | YES | NULL | 過濾原因文字 |
| `parseTrace` | TEXT | YES | NULL | 完整解析追蹤日誌 |
| `parseVersion` | INTEGER | NO | `0` | 解析引擎版本（DB v8+） |
| `lastParsedAt` | INTEGER | YES | NULL | 最後重新解析時間（ms） |
| `lineSenderType` | TEXT | YES | NULL | LINE 訊息來源類型 |

**ParseStatus 值：**

| 值 | 顯示標籤 | 建立 PendingEvent |
|----|----------|:-:|
| `HIGH_CONFIDENCE` | 高信心 | ✅ |
| `MEDIUM_CONFIDENCE` | 中信心 | ✅ |
| `LOW_CONFIDENCE` | 低信心 | ✅ |
| `PARTIAL_PARSE` | 部分解析 | ❌ |
| `TRANSFER` | 轉帳 | ❌ |
| `UNKNOWN` | 未識別 | ❌ |
| `IGNORED` | 已過濾 | ❌ |
| `DUPLICATE` | 重複通知 | ❌ |

---

### 5. `regex_patterns`

使用者自訂的解析 Regex 規則。

| Column | Type | Nullable | Default | 說明 |
|--------|------|----------|---------|------|
| `id` | INTEGER PK | — | autoGenerate | 主鍵 |
| `patternString` | TEXT | NO | — | Regex 字串 |
| `patternType` | TEXT | NO | — | `AMOUNT` / `MERCHANT` |
| `sourceBody` | TEXT | NO | — | 來源通知原文 |
| `sourcePackageName` | TEXT | NO | — | 來源 packageName |
| `sourceAppLabel` | TEXT | NO | — | 來源 App 顯示名稱 |
| `testPassed` | INTEGER | YES | NULL | 測試是否通過（null=未測試） |
| `note` | TEXT | NO | `''` | 使用者備注 |
| `createdAt` | INTEGER | NO | — | 建立時間（ms） |

---

## 遷移歷史

| 版本 | 異動內容 |
|------|----------|
| 1→2 | 新增 `notification_log` 表 |
| 2→3 | 三張表新增 `eventSource` 欄位 |
| 3→4 | `transactions` 新增 `deletedAt`（軟刪除） |
| 4→5 | `notification_log.parseStatus` 欄位 |
| 5→6 | 新增 `regex_patterns` 表 |
| 6→7 | `notification_log` 新增 `category`、`isFiltered`、`filteredReason`、`parseTrace` |
| 7→8 | `notification_log` 新增 `parseVersion`、`lastParsedAt`、`lineSenderType` |
| 8→9 | `transactions` 新增 `transactionType`（DEFAULT 'EXPENSE'） |
| 9→10 | `categories` 新增 `categoryType`；插入 id 9–14 收入類別 |

---

## 注意事項

- `exportSchema = false` — 不輸出 JSON schema 檔案
- 所有 Boolean 在 SQLite 中以 `INTEGER`（0/1）儲存
- 新增 schema 變更時必須建立 `Migration` 物件，禁止使用 `fallbackToDestructiveMigration()`
- `trimToLimit()` 在每次 `insert()` 後觸發，保留最新 500 筆 notification_log
