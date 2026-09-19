# Change: add-home-shortcuts

## Why

喝水與「記一餐」是一天要做很多次的事，但現在每一次都要：點圖示 → 等 App 開 → 今日頁 → 找到喝水那張卡 → 按 +250。
純粹為了加 250 ml，這個代價太高，高到會讓人乾脆不記。

Android 的長按 App 圖示選單（以及釘在桌面的捷徑）就是為這種事設計的：**把最常做的那一兩件事
從「開 App 再操作」壓成「一次點擊」**。這一版把 Bite 最常做的四件事放上去，其中兩件（喝水、記 1 份常吃的食物）
連畫面都不開。

## What Changes

- **兩個靜態捷徑**（永遠都在，裝好就有）：
  - 「記一餐」→ 開 App 並直接進今天的新增紀錄頁，餐別依當下時間帶入
  - 「喝水 +250 ml」→ **不開任何畫面**，把 250 ml 記進今天，用 Toast 回報目前水量
- **兩個動態捷徑**：最近吃過的前兩個食物，點下去 **不開畫面**，把 1 份記進今天（餐別依當下時間），
  用 Toast 回報食物名稱與熱量。食物被刪掉或不再是「最近吃的」時自然消失。
- **新增隱形跳板 `ShortcutActivity`**：喝水與記食物的收件人，做完事就 `finish()`，
  形狀與既有的 `CapabilityActivity` 相同（透明主題、`noHistory`、`excludeFromRecents`、不進最近使用）。
  沒有 intent-filter —— 捷徑用的是指名元件的 Intent，不必對外多開一個 action。
- **捷徑的日期一律是「今天」**，不是 App 裡正在看的那一天。桌面捷徑的語意就是「現在」。
- **設定頁新增「桌面捷徑」區塊**：喝水與記一餐各一顆「加到桌面」，走系統的釘選流程；
  桌面不支援釘選時誠實告知，請使用者改用長按 App 圖示。
- **`MainActivity` 補上冷啟動的 intent 處理**：既有的 `EXTRA_OPEN_TODAY` 只在 `onNewIntent` 讀，
  App 沒開著時按捷徑會漏掉。這一版兩個旗標（`OPEN_TODAY`、新的 `OPEN_ADD`）在 `onCreate` 也讀一次。
- **零新權限、零新相依**：`ShortcutManagerCompat` 來自已經在用的 `androidx.core`，
  捷徑與釘選都不需要任何 `uses-permission`。回饋一律用 Toast，不做通知
  （Android 13+ 的 `POST_NOTIFICATIONS` 會破壞「零權限」這個賣點）。
- **版本**：versionCode 8 → 9、versionName `0.6.0` → `0.7.0`。

## Non-goals（留給之後的版本）

- 不做 widget。捷徑先把「一次點擊記一筆」做出來，widget 是另一個量級的工作。
- 不做「喝水 +500」或自訂毫升數的捷徑。長按選單只有四格，多放一個就要擠掉一個食物。
- 不做通知、不做鬧鐘提醒（會需要權限）。
- 不把動態捷徑做成「常吃」而是「最近」：最近吃的才是「等一下可能會再記一次」的那些。
- 不處理已釘選捷徑的失效顯示：食物被刪掉時那個動態捷徑會消失，若使用者先前把它釘在桌面，
  系統會自己把它標成停用。這是 Android 既有的行為，不另外做。

## Capabilities

### New Capabilities
- `bite-home-shortcuts`: 桌面／長按圖示的捷徑——靜態與動態捷徑的內容與排序、
  不開畫面就記一筆的行為、日期與餐別的判定、失敗回饋、釘到桌面

### Modified Capabilities
（無。`bite-diary` 的既有需求一字未改：捷徑記下去的那一筆與 App 裡記的完全一樣，
走的是同一個資料模型與同一組預設餐別規則。）

## Impact

- 新增 `app/src/main/java/com/routina/bite/ShortcutActivity.kt`（喝水／記食物的隱形跳板）
- 新增 `app/src/main/java/com/routina/bite/data/Shortcuts.kt`（動態捷徑的計算與發佈、釘選用的捷徑）
- 新增 `app/src/main/res/xml/shortcuts.xml`（兩個靜態捷徑）
- 新增 `app/src/main/res/drawable/ic_shortcut_{water,meal,food}.xml`（三個單色 vector 圖示）
- 修改 `app/src/main/java/com/routina/bite/BiteApp.kt`（觀察紀錄與食物庫，重發動態捷徑）
- 修改 `app/src/main/java/com/routina/bite/MainActivity.kt`（`EXTRA_OPEN_ADD`、冷啟動也讀旗標）
- 修改 `app/src/main/java/com/routina/bite/ui/SettingsScreen.kt`（「桌面捷徑」區塊）
- 修改 `app/src/main/AndroidManifest.xml`（`ShortcutActivity`、靜態捷徑的 meta-data）
- 修改 `app/proguard-rules.pro`（保留 `ShortcutActivity`）、`res/values/strings.xml`、`app/build.gradle.kts`（版本）
- **無新相依、無新權限、無資料模型變更、無備份格式變更**（`schemaVersion` 維持 1）
