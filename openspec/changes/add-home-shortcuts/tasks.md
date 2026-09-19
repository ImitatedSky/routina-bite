# Tasks: add-home-shortcuts

## 1. 圖示與靜態捷徑

- [x] 1.1 `res/drawable/ic_shortcut_water.xml`：一滴水，24dp viewport、單色 `#4B5699`、透明背景（D53）
- [x] 1.2 `res/drawable/ic_shortcut_meal.xml`：叉子與湯匙（D53）
- [x] 1.3 `res/drawable/ic_shortcut_food.xml`：一個盤子（圓環，`fillType="evenOdd"`）（D53）
- [x] 1.4 `res/xml/shortcuts.xml`：`log_meal`（→ `MainActivity` + `OPEN_ADD`）與
      `water_250`（→ `ShortcutActivity` + `SHORTCUT_ACTION=water` + `ML=250`），順序即選單順序（D47）
- [x] 1.5 `AndroidManifest.xml`：`MainActivity` 掛 `android.app.shortcuts` meta-data

## 2. 隱形跳板（`ShortcutActivity.kt`）

- [x] 2.1 新檔，形狀照 `CapabilityActivity`：`Activity` + `onCreate` 做事 + `finish()`（D48）
- [x] 2.2 manifest 條目：`exported`、`noHistory`、`excludeFromRecents`、`taskAffinity=""`、
      `Theme.Bite.Invisible`，**不開 intent-filter**（D48）
- [x] 2.3 extras 常數：`EXTRA_ACTION` / `EXTRA_ML` / `EXTRA_FOOD_ID`、`ACTION_WATER` / `ACTION_LOG_FOOD`（D48）
- [x] 2.4 water：`repository.addWater(todayDate(), ml)` → toast `+%1$d ml · 今天 %2$d / %3$d`（D49）
- [x] 2.5 log_food：找不到食物就 toast `這個食物已經不在食物庫了`；找得到就用 `todayDate()` +
      `defaultMeal()` 存一筆 1 份的快照 → toast `已記 1 份 %1$s · %2$d 大卡`（D49）
- [x] 2.6 認不得的 action 也只 toast；`application` 不是 `BiteApp` 時直接收工，全程不丟例外（D48）
- [x] 2.7 `proguard-rules.pro` 加 `-keep class com.routina.bite.ShortcutActivity`

## 3. 動態捷徑（`data/Shortcuts.kt`）

- [x] 3.1 `updateDynamicShortcuts(context, entries, foods)`：`recentFoods(entries, foods, 2)` →
      `ShortcutManagerCompat.setDynamicShortcuts`，id 為 `food_<foodId>`、`rank` 依序（D47）
- [x] 3.2 內容與已發佈的一樣就不重發（`setDynamicShortcuts` 有呼叫次數限制）（D50）
- [x] 3.3 整段包 try/catch：捷徑發不出去不影響 App（D48）
- [x] 3.4 `waterPinShortcut(context)` / `logMealPinShortcut(context)`：id 與 `shortcuts.xml` 相同（D52）
- [x] 3.5 `BiteApp.onCreate`：`combine(entries, foods)` 在 `Dispatchers.Default` 上 collect 並重發，
      一條路同時覆蓋「啟動時」與「新增／刪除紀錄之後」（D50）

## 4. 「記一餐」（`MainActivity.kt`）

- [x] 4.1 `EXTRA_OPEN_ADD` 常數與 `openAddRequests` 計數器（D51）
- [x] 4.2 `readRequests(intent)`：`onCreate` 讀 launch intent、`onNewIntent` 也讀，
      讀完 `removeExtra`；`EXTRA_OPEN_TODAY` 一起補上冷啟動這條路（D51）
- [x] 4.3 `LaunchedEffect(openAddRequests)`：`backToToday()` →
      `popBackStack(TODAY)` → `navigate(add/{todayDate()}/{defaultMeal()})`（D49、D51）

## 5. 設定頁與資源

- [x] 5.1 `ui/SettingsScreen.kt`：「桌面捷徑」區塊放在「備份」與版本之間，
      一行說明 + 兩列「名稱 + 加到桌面」（D54）
- [x] 5.2 `pinShortcut(...)`：先問 `isRequestPinShortcutSupported`，不支援就提示改用長按圖示；
      沒有「已加到桌面」這種假成功（D52）
- [x] 5.3 中文文字套 `TextStyle.zh()`，間距沿用這一頁的 12dp 組內 / 分隔線分組（D54）
- [x] 5.4 `res/values/strings.xml`：捷徑標籤、三段 toast、設定頁四個字串，附在檔尾
- [x] 5.5 `app/build.gradle.kts`：versionCode 8 → 9、versionName `0.6.0` → `0.7.0`
- [x] 5.6 確認 manifest 仍然零 `uses-permission`、`gradle/libs.versions.toml` 一個相依都沒加

## 6. 驗證

- [x] 6.1 `./gradlew :app:assembleDebug` 與 `:app:assembleRelease` 綠燈
- [x] 6.2 裝置：`dumpsys shortcut` 看得到 4 個——靜態 `log_meal`(rank 0) / `water_250`(rank 1)、
      動態 `food_<id>`(rank 0/1)，label 與 intent extras 都正確
      （BlueStacks 是 API 28，沒有 `cmd shortcut get-shortcuts`，改用 `dumpsys shortcut`）
- [x] 6.3 裝置：`am start .ShortcutActivity --es …SHORTCUT_ACTION water --ei …ML 250` →
      不開畫面、toast `+250 ml · 今天 500 / 2000`、今日頁喝水從 0 → 250 → 500（按兩次會累加）
- [x] 6.4 裝置：`--es …SHORTCUT_ACTION log_food --es …FOOD_ID <真實 id>` → toast
      `已記 1 份 BSN Syntha-6 香草 · 193 大卡`，落在「點心」（03:01，依時間）、顯示「1 份 · 47.0 g」、193 大卡
- [x] 6.5 裝置：`--es …FOOD_ID no-such-food-id` → toast `這個食物已經不在食物庫了`、沒有多出紀錄、沒崩
- [x] 6.6 裝置：`am start .MainActivity --ez …OPEN_ADD true` 熱啟動與 `force-stop` 後的冷啟動
      都開到「新增紀錄」且餐別是「點心」；返回回到今日頁；轉螢幕後停在今日頁不會再跳
- [x] 6.7 裝置：記一筆新的食物之後 `dumpsys shortcut` 的動態順序變了
      （BSN Syntha-6 升到 rank 0），刪掉那一筆之後又變回原本的兩個
- [x] 6.8 裝置：設定頁兩顆「加到桌面」都有反應——BlueStacks 的 launcher
      （`com.uncube.launcher3`）不支援釘選，顯示「這個桌面不支援釘選捷徑，請改用長按 App 圖示」
- [x] 6.9 裝置：release（R8 minify + shrinkResources）APK 也裝起來驗過，4 個捷徑照樣發佈、
      喝水捷徑照樣運作，確認 `-keep` 與資源沒被 shrink 掉
- [x] 6.10 三個 vector 圖示以相同 path 在裝置的瀏覽器上放大檢視：水滴、叉匙、圓環都正確
- [x] 6.11 全程 `logcat -b crash` 乾淨
- [x] 6.12 測完刪掉自己造的測試紀錄、把喝水歸零；食物庫仍是 78 筆
      （Subway 36 + 參考值 3 + 好事多 5 + 池上便當 2 + 蛋白粉 3 + 超商 1 + 麥當勞 8 + 未分類 20）、
      歷史紀錄原樣都在
- [x] 6.13 `openspec status --change add-home-shortcuts` 4/4、
      `openspec validate add-home-shortcuts --strict` 與 `openspec validate --all --strict` 全綠
- [x] 6.14 commit（英文、無 Co-Authored-By）；不 push、不 tag
