# MoneyKeeper 💰

> AI 驅動的自動化記帳 Android App  
> 核心理念：**自動偵測消費事件，最小化手動輸入**

[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-blue)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

---

## 功能概覽

| 功能 | 狀態 | 說明 |
|------|------|------|
| 手動記帳 | ✅ | 計算機介面、收入/支出分類、自訂類別 |
| 通知自動記帳 | ✅ | 監聽金融 App 通知，自動解析金額 |
| 通知過濾 | ✅ | 40+ 台灣銀行白名單 + 關鍵字過濾 |
| Parser Engine | ✅ | 6 個解析器、三維信心分數、重複偵測 |
| 待確認消費 | ✅ | 解析成功的通知等待使用者確認/拒絕 |
| 支出圓餅圖 | ✅ | 本月支出類別比例視覺化 |
| 通知 Debug Log | ✅ | 所有通知記錄、parse trace、Filter 原因 |
| Regex 自訂規則 | ✅ | 使用者自建解析規則，儲存至 DB |
| 垃圾桶 | ✅ | 軟刪除、還原、定期清理 |

---

## 架構

```
app/
├── data/
│   ├── database/          — Room DB (5 tables, version 10)
│   │   ├── dao/           — TransactionDao, CategoryDao, PendingEventDao,
│   │   │                    NotificationLogDao, RegexPatternDao
│   │   └── entity/        — Entity + toDomain() / toEntity()
│   ├── notification/      — Parser Engine
│   │   ├── parser/        — 6 解析器 (Strategy Pattern)
│   │   ├── AmountPatterns.kt      — TWD regex 萃取
│   │   ├── ConfidenceBreakdown.kt — 三維評分
│   │   ├── DuplicateDetector.kt   — 5 秒重複偵測
│   │   ├── RelevanceFilter.kt     — 白名單 + 關鍵字過濾
│   │   └── UserPatternEngine.kt   — 使用者自訂 regex
│   └── repository/        — 5 個 Repository
├── domain/model/          — 純 Kotlin data class
├── presentation/
│   ├── navigation/        — NavGraph + Screen sealed class
│   └── screen/            — 9 個 Screen + ViewModel
├── di/                    — Hilt DatabaseModule
└── service/               — NotificationListenerService
```

**Tech Stack:** Kotlin · Jetpack Compose · Material3 · Room · Hilt · Coroutines · Flow

---

## 快速開始

### 環境需求
- Android Studio Hedgehog (2023.1.1) 以上
- JDK 17
- Android SDK 26+

### 建置步驟
```bash
git clone https://github.com/<your-username>/money-keeper.git
cd money-keeper
# 用 Android Studio 開啟，或：
./gradlew assembleDebug
```

### 啟用通知監聽
1. 安裝 App 後，前往 **設定 → 通知存取權**
2. 找到 **MoneyKeeper** 並開啟

---

## 通知解析流程

```
通知到達 (onNotificationPosted)
  ↓
RelevanceFilter — 白名單 / 關鍵字過濾
  ↓ 通過
ParserRegistry — 按序執行 6 個 Parser
  TransferParser → LinePayParser → LineBankParser
  → CathayParser → CTBCParser → GenericBankParser
  ↓ 解析成功
DuplicateDetector — 5 秒內相同金額？
  ↓ 非重複
NotificationLogRepository.insert()
PendingEventRepository.insertFromParsed()  ← 信心度 ≥ 0.4
  ↓
使用者在「待確認消費」頁面確認 → Transaction
```

---

## 資料庫

| Table | 用途 |
|-------|------|
| `transactions` | 已確認的收支記錄（含軟刪除） |
| `categories` | 8 支出類別 + 6 收入類別（系統預設） |
| `pending_events` | 待使用者確認的解析結果 |
| `notification_log` | 所有通知的完整日誌 |
| `regex_patterns` | 使用者自訂解析規則 |

DB 版本：**10**　|　PARSE_VERSION：**3**

---

## 支援的金融來源（部分）

LINE Bank、國泰世華、中國信託、玉山銀行、台新 Richart、永豐銀行、台北富邦、第一銀行、兆豐銀行、合作金庫、LINE Pay、街口支付、Pi 拍錢包、悠遊付 ⋯ 共 40+ 個來源

---

## 不在 MVP 範圍

- 雲端同步 / 後端 server
- 使用者登入 / 帳號系統
- AI 模型 (on-device 或 API)
- GPS / Geofencing
- 台灣電子發票載具
- 多幣別
- 帳戶管理

---

## 貢獻

目前為個人專案，歡迎開 Issue 回報問題或建議功能。

---

## License

MIT License — see [LICENSE](LICENSE)
