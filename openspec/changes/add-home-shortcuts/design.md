# Design: add-home-shortcuts

## Context

Bite 已經有一個「別人呼叫我」的入口：`CapabilityActivity`——透明主題、讀 extras、做事、`finish()`，
不進返回堆疊也不進最近使用。家族的 `open_today` 走這條路，已經實機驗證過。

桌面捷徑要的是同一個形狀：使用者在 App 外面按一下，App 做一件事，不要給他一個畫面看。
所以這一版不發明新做法，直接把 `CapabilityActivity` 的形狀再做一次成 `ShortcutActivity`。

沿用既有決策：紀錄存快照（add-bite-app D3）、份數是唯一真值（D5）、預設餐別依時間（D4）、
視覺與刪除規範（D10）；add-bite-charts 的 D5（中文不用字重）與 D6（間距 4/8/12/16/24，組內 < 組間）；
inline-entry-form 的 D40（新增紀錄頁一進來就是表單，所以「記一餐」導到那一頁就等於可以打字了）。

下面只寫這次新增的決策，編號接在 D46 之後。

## Goals / Non-Goals

**Goals:**
- 喝水與「記 1 份最近吃的食物」從「開 App → 找卡片 → 按鈕」壓成一次點擊，而且不閃任何畫面
- 「記一餐」在 App 沒開著的時候也能用，而且落在今天、餐別對
- 動態捷徑跟著資料走：吃了新東西順序就變，食物刪掉就消失
- 零新權限、零新相依
- App 外的入口不會崩潰：任何失敗都只有一個 Toast

**Non-Goals:**
- 不做 widget、不做通知、不做提醒
- 不做可設定的捷徑清單（要放哪幾個由這一版定死）
- 不處理「已釘選但來源消失」的顯示，交給系統既有行為

## Decisions

### D47. 捷徑分兩類：靜態兩個固定、動態兩個跟著「最近吃的」

```
長按 App 圖示
├─ 記一餐          靜態 rank 0 → MainActivity（開新增紀錄頁）
├─ 喝水            靜態 rank 1 → ShortcutActivity（不開畫面）
├─ <最近食物 1>    動態 rank 0 → ShortcutActivity（不開畫面）
└─ <最近食物 2>    動態 rank 1 → ShortcutActivity（不開畫面）
```

靜態的兩個寫在 `res/xml/shortcuts.xml`，裝好就有，**不必先開過 App**。
動態的兩個由 `ShortcutManagerCompat.setDynamicShortcuts` 發，內容是 `recentFoods(entries, foods, 2)`。

**為什麼是「最近」不是「常吃」**：捷徑要解決的是「剛剛吃的那個我等一下還要再記一次」
（早餐的蛋白粉、每天同一家的便當）。常吃是整季的統計，變動太慢，對捷徑沒有意義。

**為什麼動態只放兩個**：多數 launcher 的長按選單在四到五格就要捲，靜態已經佔兩格。
放三個食物會把「記一餐」擠下去，而「記一餐」是唯一一個「我要記的東西不在清單裡」時的出口。

### D48. 喝水與記食物走隱形跳板，不開任何畫面

`ShortcutActivity` 照抄 `CapabilityActivity` 的形狀：`android:theme="@style/Theme.Bite.Invisible"`、
`noHistory="true"`、`excludeFromRecents="true"`、`taskAffinity=""`、`exported="true"`，
`onCreate` 裡做完事直接 `finish()`。

**不開 intent-filter**：捷徑的 Intent 帶的是明確的 `ComponentName`（`targetPackage` + `targetClass`），
launcher 用不到 action 比對。開一個自訂 action 等於多一個對外入口，而沒有人需要它。
`exported="true"` 仍然必要——按下捷徑而真正呼叫 `startActivity` 的是 launcher，不是 Bite 自己。

extras 的契約（`ShortcutActivity` 的 companion object，也是 adb 驗證時用的鍵）：

| extra | 值 |
| --- | --- |
| `com.routina.bite.extra.SHORTCUT_ACTION` | `"water"` 或 `"log_food"` |
| `com.routina.bite.extra.ML` | 喝水的毫升數，沒帶時 250 |
| `com.routina.bite.extra.FOOD_ID` | 要記哪一個食物 |

**任何失敗都只 Toast 不崩**：這是 App 外的入口，使用者按下去的當下 Bite 可能整個 process 都還沒起來。
`SHORTCUT_ACTION` 不認得、食物已經被刪掉、`application` 不是 `BiteApp`——三種都各自有出口，沒有一個會丟例外。

考慮過讓喝水捷徑開一個小對話框讓使用者確認毫升數。否決：那樣就又變成「開畫面再操作」，
與這一版的目的完全相反。按錯的代價是回 App 按一下「−250」。

### D49. 日期一律 `todayDate()`，餐別一律 `defaultMeal()`

捷徑的語意就是「現在」。App 裡的「正在看的那一天」（`BiteViewModel.selectedDate`）
可能停在上週某一天——使用者昨晚在看歷史，今天早上按捷徑喝水，水絕對要記在今天。

所以 `ShortcutActivity` **不碰 ViewModel**，直接用 `BiteApp.repository` 加上 `todayDate()`；
`MainActivity` 收到 `EXTRA_OPEN_ADD` 時也是先 `backToToday()` 再導到 `add/{todayDate()}/{defaultMeal()}`。
這一點在程式碼裡有註解，因為「為什麼不用 selectedDate」是看程式碼看不出來的。

餐別用 `defaultMeal()`（D4 的時間區間），與 App 裡按 ＋ 的預設完全一致：
使用者在 12:15 按「記 1 份便當」，它就該落在午餐。

### D50. 動態捷徑由 `BiteApp` 觀察資料流重發，不在每個入口各自呼叫

```kotlin
// BiteApp.onCreate
scope.launch {
    combine(repository.entries, repository.foods, ::Pair).collect { (entries, foods) ->
        updateDynamicShortcuts(this@BiteApp, entries, foods)
    }
}
```

`scope` 是 `Dispatchers.Default`，所以 `setDynamicShortcuts`（一次 IPC）不會卡 UI。

這一條同時滿足兩個時機：**App 啟動時**（StateFlow 的第一次發送）與**新增或刪除紀錄之後**
（`_entries` 一變就再發一次）。選它而不是「在 `addEntry` / `deleteEntry` 後各自呼叫一次」，
是因為新增紀錄的入口有五個（今日頁、新增紀錄頁、複製昨天、匯入備份、捷徑自己），
逐個記得呼叫遲早會漏。觀察資料流就一定不會漏，而且捷徑自己記完一筆也會順便把自己重排——
不必在 `ShortcutActivity` 裡多寫一行。

食物庫也一起觀察，因為食物被刪掉時 `recentFoods` 才會把它濾掉（紀錄還在，食物沒了）。

`updateDynamicShortcuts` 內部先比對一次目前已發佈的動態捷徑（id 與 shortLabel），
一樣就直接 return：`setDynamicShortcuts` 有系統層的呼叫次數限制，匯入備份那種一次改一大批的情況
不該打掉這個額度。

放在 `BiteApp` 而不是 `BiteRepository`：repository 是純資料層，它不該知道桌面捷徑這種東西存在。
`BiteApp` 本來就是把各層接起來的地方。

### D51. 「記一餐」用 intent extra 叫 `MainActivity` 導到新增紀錄頁，冷啟動也要吃

`MainActivity` 多一個 `EXTRA_OPEN_ADD`，與既有的 `EXTRA_OPEN_TODAY` 同一套機制（計數器 +1，
畫面端 `LaunchedEffect` 看到值變了就動作）。

**既有的 `EXTRA_OPEN_TODAY` 只在 `onNewIntent` 處理，冷啟動是漏的**——家族跳板永遠是把既有的 task
帶到前景，所以之前沒被發現。桌面捷徑不一樣：使用者多半是在 App 沒開著的時候按的，
那一次只會進 `onCreate`。所以兩個旗標都改成在 `onCreate` 讀 launch intent 也處理一次，順手補掉舊的那個洞。

旗標讀完 `intent.removeExtra(...)`：轉螢幕時 Activity 重建會拿到同一個 intent 實例，
不清掉就會再跳一次新增紀錄頁。（計數器本身在重建時也歸零，兩層都擋，但清旗標是講得出道理的那一層。）

導覽是 `popBackStack(TODAY)` 之後再 `navigate(add/...)`，不是直接 `navigate`：
使用者從新增紀錄頁按返回時應該看到今日頁，而不是回到他上次離開 App 時停的那個畫面。

### D52. 釘到桌面：設定頁兩顆按鈕，id 與靜態捷徑相同

`ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)`，
`androidx.core` 已經是相依，**不加新相依、不需要任何權限**。

傳進去的 `ShortcutInfoCompat` 用與 `shortcuts.xml` 相同的 id（`water_250`、`log_meal`），
系統就會把既有的那個靜態捷徑釘上去，而不是多長一個只存在於桌面的副本。

先問 `isRequestPinShortcutSupported(context)`，不支援就 toast
「這個桌面不支援釘選捷徑，請改用長按 App 圖示」。**不做「已加到桌面」的成功回饋**：
同不同意、放在哪一格，決定權在桌面那支 App，Bite 收不到結果，報「成功」會是假的。

只給喝水與記一餐兩個。食物捷徑不給釘——它是會變的，釘一個之後那個食物掉出「最近」就變成死的捷徑。

### D53. 三個 vector drawable，單色家族主色，不做 adaptive icon

`ic_shortcut_water`（一滴水）、`ic_shortcut_meal`（叉子與湯匙）、`ic_shortcut_food`（一個盤子／圓環），
都是 24dp viewport、透明背景、單一 `#4B5699`（家族主色，與 `FamilyColors.Primary` 同值）。
用 `IconCompat.createWithResource(...)` 帶進捷徑。

launcher 會自己幫捷徑圖示加白底圓形，所以不需要做 adaptive bitmap：
為了一個只會出現在長按選單裡、直徑約 24dp 的圖示去產五個密度的點陣圖與前景／背景兩層，
複雜度與收益完全不成比例。

顏色寫死十六進位而不是 `?attr/colorPrimary`：捷徑圖示是給 launcher 畫的，
不在 Bite 的主題底下，attr 解不出來。

### D54. 設定頁「桌面捷徑」放在「備份」下面

設定頁由上到下是「每天可能會調的」到「設定一次就不回來的」：每日目標 → 備份 → 桌面捷徑 → 版本。
釘選是一次性動作，不該和每日目標爭注意力。

版面沿用這一頁既有的規則：`Column` 的 `spacedBy(12.dp)` 是組內間距，
組與組之間隔一條 `HorizontalDivider`（12 + 線 + 12 ≈ 24dp 以上），組內 12 < 組間 24，符合 1:2。
每一列是「說明文字（`weight(1f)`）＋ `OutlinedButton`」，按鈕與「匯出／匯入」同一級，
不用 `Button` —— 這一頁只有「儲存目標」是主要動作。

## Risks / Trade-offs

- [動態捷徑的內容會外洩到 launcher] → 捷徑的 shortLabel 就是食物名稱，
  長按圖示時旁邊的人看得到「記 1 份 高蛋白」。這是所有 App 的動態捷徑都有的性質，
  而且使用者可以不理它（不按就不會變成桌面上的東西）。不另外做開關。
- [`setDynamicShortcuts` 有系統呼叫次數限制] → 已用「內容沒變就不發」擋掉重複呼叫；
  而且限制在 App 被帶到前景時會重置，Bite 的資料變動幾乎都發生在前景。
- [捷徑記下去的那一筆沒有「份數不是 1」的選擇] → 就是 1 份。要記 1.5 份請走「記一餐」。
  給捷徑一個數量選擇器等於又開一個畫面（D48）。
- [`ShortcutActivity` 記完一筆就 `finish()`，寫檔是非同步的] → 與 App 裡任何一次新增走的是同一條
  `BiteRepository.persistDiary()`（IO 執行緒 + Mutex + tmp/rename），process 不會在 `finish()` 的當下被殺。
  這一點與既有行為一致，沒有新風險。
- [BlueStacks 的 launcher 不顯示 App 捷徑、也不支援釘選] → 長按選單只有它自己的
  「資訊／桌面捷徑／解除安裝」，`requestPinShortcut` 走到「不支援」那條。
  所以**捷徑在真正的 launcher 上長什麼樣子、釘上去之後好不好按，沒能在這台裝置上看到**，
  要等真機。機制本身已用 `dumpsys shortcut` 與 `am start` 直接驗過（見 tasks 6）。
- [`shortcuts.xml` 的 `<extra>` 會被系統存成 `PersistableBundle`] → 只支援 String/int/boolean，
  這一版用到的剛好都在裡面（`"water"`、`250`、`true`）。已在裝置上確認三個值都正確送達。

## Migration Plan

沒有資料遷移：資料模型、檔案格式、備份格式都沒有動，`diary.json`、`water.json` 與既有備份原樣可讀。

1. 在 main 上 commit（不 push、不 tag）。
2. 使用者驗收後決定何時打 `v0.7.0`；CI 會比對 tag 與 `app/build.gradle.kts` 的 versionName，
   所以 versionName 這一波先改成 `0.7.0`（versionCode 9）。

回滾：裝回 v0.6.0，資料完全相容（沒有任何新欄位）。舊版沒有捷徑，
系統會在下次掃描時把靜態捷徑收掉；已釘選的會被標成停用，使用者自行移除。

## Open Questions

- **要不要讓使用者自己選要放哪幾個捷徑**？現在定死（記一餐、喝水、最近兩個食物）。
  如果使用者回報「我根本不喝水，那一格浪費了」，做法是設定頁給一個三選二的開關，
  而不是做成完整的捷徑編輯器。
- **喝水的毫升數**：現在固定 250（一杯）。`EXTRA_ML` 已經是參數，
  真的要「喝水 +500」只要在 `shortcuts.xml` 多一個 `<shortcut>`，但那會擠掉一個食物（D47）。
