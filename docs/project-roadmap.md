# Project Roadmap

MoneyKeeper 的功能發展方向與優先順序。

> **原則：** Local-first、無帳號、最小化手動輸入。  
> 不在路線圖中：雲端同步、AI 模型、多幣別、GPS。

---

## 現況（v0.9.0）

已完成的核心功能：

- [x] 手動記帳（計算機介面、支出/收入、軟刪除）
- [x] 通知自動解析（6 個 Parser、三維信心評分）
- [x] 40+ 台灣金融來源白名單
- [x] 重複通知偵測（5 秒窗口）
- [x] 待確認消費流程（Pending Events）
- [x] 通知完整 Debug Log（parse trace、filter 原因）
- [x] 使用者自訂 Regex 規則（Regex Builder + Pattern Library）
- [x] 14 個預設類別（8 支出 + 6 收入）
- [x] 本月支出圓餅圖
- [x] 月/年日期選擇器
- [x] 垃圾桶（還原 / 定期清理）

---

## 短期目標（v1.0）

> 目標：讓通知授權流程完整，可正式使用。

### P0 — 必做
- [ ] **通知授權引導頁**  
  偵測 `NotificationListenerManager.isNotificationListenerAccessGranted()`  
  → 未授權時顯示引導畫面，附帶「前往設定」按鈕  
  → 授權後即時更新 UI

- [ ] **Service 狀態指示器**  
  Dashboard 或設定頁顯示「通知監聽：運作中 / 已停止」  
  使用 `MoneyNotificationListenerService.isConnected StateFlow`

### P1 — 高優先
- [ ] **搜尋 / 篩選**  
  TransactionDao 新增 `searchByKeyword(query)` 查詢  
  Dashboard 新增搜尋欄（摺疊式）

- [ ] **重新解析功能**  
  NotificationLog 頁面「全部重新解析」按鈕  
  批次重跑 `parseVersion < PARSE_VERSION` 的記錄

- [ ] **月份切換後 Day 定位**  
  切換月份後，若當月有記錄，自動跳至最近一筆交易的日期

### P2 — 一般
- [ ] **CSV 匯出**  
  匯出 `transactions` 為 CSV  
  使用 `FileOutputStream`，分享至其他 App

- [ ] **收入 Parser 支援**  
  目前通知解析預設為支出，研究如何從薪資/入帳通知識別收入事件

---

## 中期目標（v1.1–v1.5）

### 統計與視覺化
- [ ] **月份趨勢圖**：過去 6 個月收/支長條圖（Canvas）
- [ ] **類別趨勢**：特定類別的月度消費趨勢折線圖
- [ ] **每日消費熱度圖**（Calendar Heatmap 風格）

### 記帳體驗
- [ ] **快速記帳 Widget**  
  Android App Widget 直接開啟計算機記帳頁
- [ ] **重複記帳（訂閱）**  
  固定日期自動建立記錄（如月租費、訂閱服務）
- [ ] **預算設定**  
  各類別月度預算上限，超過時顯示警示

### 通知解析
- [ ] **更多銀行 Parser**：台灣銀行、華南銀行、彰化銀行、遠東銀行
- [ ] **Parser 命中率統計**：各 Parser 的解析成功率儀表板
- [ ] **使用者回饋機制**：從 PendingEvent 拒絕操作學習，改善過濾規則

---

## 長期目標（v2.0+）

### 資料安全
- [ ] **本地加密**：Room 整合 SQLCipher（不上雲）
- [ ] **自動備份**：匯出加密的 ZIP 至本機 / Google Drive（使用者手動觸發）
- [ ] **App Lock**：生物辨識解鎖（BiometricPrompt）

### 進階分析
- [ ] **財務健康分數**：基於收支比例、儲蓄率的月度評估
- [ ] **異常消費偵測**：單筆遠高於同類別平均時提醒
- [ ] **帳戶餘額追蹤**：手動輸入各帳戶餘額，比對消費記錄

### 生態系統
- [ ] **通知解析規則社群庫**：使用者可匯出/匯入 Regex Pattern 包
- [ ] **Wear OS 伴侶 App**：手錶快速記帳

---

## 技術債 / 品質改善

- [ ] **Unit Test for Parsers**  
  `AmountPatterns`、各 Parser 的 `canHandle()`/`parse()` 純 Kotlin 單元測試
- [ ] **Parser 回歸測試集**  
  建立真實通知文字樣本庫，防止改版後誤判
- [ ] **ProGuard 設定**  
  啟用 release 混淆，確保 Hilt / Room 不被誤混
- [ ] **MVVM 分層嚴格化**  
  目前 Repository 直注 DAO（MVP 簡化），長期可考慮引入 UseCase 層
- [ ] **Compose Preview 補全**  
  各 Screen 補充 `@Preview`，加速 UI 開發

---

## 已排除功能（MVP 外）

| 功能 | 排除原因 |
|------|----------|
| 雲端同步 | 增加複雜度、隱私疑慮、需後端維護 |
| 使用者帳號 | 個人工具不需要，Local-first |
| AI 模型 | APK 大小、效能、離線可靠性問題 |
| GPS / 地點 | 電池耗電、隱私疑慮 |
| 台灣電子發票載具 | API 對接複雜，非核心路徑 |
| 多幣別 | 個人台灣使用場景不需要 |
