# Tasks: inline-entry-form

## 1. 表單狀態（`ui/EntryForm.kt`）

- [x] 1.1 `EntryFormState(initial: EntryDraft)`：把對話框裡那一堆 `remember` 的欄位搬進來，
      六個數字欄位 `private set`、名稱／備註／餐別／照片維持一般 `var`（D41）
- [x] 1.2 `amount`（`AmountEdit` 三態）、`valid`、`servingGrams`、`allowNoMeal` 改成 `get()`，
      計算規則原樣搬過來，一個字不改（D41）
- [x] 1.3 `updateServings` / `updateGrams` / `updateKcal` / `updateProtein` / `updateFat` / `updateCarbs`：
      連動邏輯（`fillNutrients`、`setBasis`）維持原樣；函式名用 `update*`，`set*` 會與欄位的 JVM setter 撞名（D41）
- [x] 1.4 `loadFrom(draft)`：重填每一個欄位並把 `edited` 歸 `NONE`；`reset()` = `loadFrom(draftOf(meal))`（D42）
- [x] 1.5 `toDraft(quickName)`：份數不合法回 null；名稱空白補「快速輸入」（沿用 D24）
- [x] 1.6 `addPhoto` / `newPhotos` / `forgetNewPhotos`：這次新寫進去的檔名記帳（D44）
- [x] 1.7 `rememberEntryFormState(initial)`
- [x] 1.8 `draftOf(meal: Meal?)` 與 `draftOf(food, meal: Meal?)`：餐別放寬成可為 null，呼叫端不必再 `?:`

## 2. 表單 UI 與對話框（`ui/EntryForm.kt`）

- [x] 2.1 `EntryFormFields(state, viewModel, modifier, showMeal)`：欄位順序與 v0.5.0 相同，
      `showMeal = false` 時不畫餐別（D40、D41）
- [x] 2.2 照片列與 `PickVisualMedia` launcher 搬進 `EntryFormFields`，選到就 `state.addPhoto(檔名)`（D44）
- [x] 2.3 `EntryFormDialog` 變成 `AlertDialog` 包 `EntryFormFields`，刪掉 `editing` 參數，
      標題固定「修改紀錄」、按鈕固定「儲存」（D41）
- [x] 2.4 `commitPhotos(viewModel)` / `dropNewPhotos(viewModel)` 兩個擴充函式，刪檔留在畫面層（D44）

## 3. 新增紀錄頁（`ui/AddFoodScreen.kt`）

- [x] 3.1 `rememberEntryFormState(draftOf(initialMeal))`，餐別晶片改成綁 `state.meal`
- [x] 3.2 第一個 item：餐別晶片 + `EntryFormFields(showMeal = false)` 放同一個 16dp 的 `Column`（D45）
- [x] 3.3 `HorizontalDivider(Modifier.padding(vertical = 12.dp))` 隔開表單與挑食物區（D45）
- [x] 3.4 刪掉「快速輸入」按鈕；「新建食物」「食物庫」降成 `TextButton` 並移到分隔線下面（D40）
- [x] 3.5 `bottomBar`：`Surface(tonalElevation = 3.dp)` + 「清空」`TextButton` 與「加入」`Button`，
      `windowInsetsPadding(ime union navigationBars)`（D43）
- [x] 3.6 `fillFrom(food)`：`dropNewPhotos` → `loadFrom(draftOf(food, state.meal))` → `animateScrollToItem(0)`，
      四個入口共用（D42）
- [x] 3.7 `add()`：`toDraft` → `commitPhotos` → `addEntry` → `onDone()`；`clear()`：`dropNewPhotos` → `reset()`
- [x] 3.8 `DisposableEffect` 的 `onDispose` 收掉沒用上的照片檔（D44）

## 4. 其他呼叫端與資源

- [x] 4.1 `ui/TodayScreen.kt`：`EntryFormDialog` 少傳 `editing`
- [x] 4.2 `res/values/strings.xml`：加 `add_clear`（清空）附在檔尾；刪掉沒有任何地方用到的 `add_amount_hint`；
      `add_quick` 留著（名稱欄 placeholder 與預設名稱還要用）
- [x] 4.3 `app/build.gradle.kts`：versionCode 7 → 8、versionName `0.5.0` → `0.6.0`

## 5. 驗證

- [x] 5.1 `./gradlew :app:assembleDebug` 與 `:app:assembleRelease` 綠燈
- [x] 5.2 裝置：今日頁按 ＋ → 表單直接在畫面上，名稱欄馬上打得進字
- [x] 5.3 裝置：只打熱量 450 直接按「加入」→ 今日頁出現「快速輸入 450」
- [x] 5.4 裝置：點一個「最近」的食物 → 名稱／份數／公克／四個營養欄被填好、畫面捲回頂端；
      分類分組清單裡的食物也一樣
- [x] 5.5 裝置：選了食物後把份數改成 2 → 公克與四個營養欄跟著變（540.0 g／920／58.0／30.4／98.0）
- [x] 5.6 裝置：公克輸入 150（每份 270 g 的食物）→ 加入後列表顯示「0.56 份 · 150.0 g」、256 大卡
- [x] 5.7 裝置：「清空」把表單重設成空白（含照片列退回「加照片」），餐別保留
- [x] 5.8 裝置：編輯既有紀錄仍然是「修改紀錄」對話框，⋯ →「編輯」也一樣；只改餐別存檔後仍是 150.0 g
- [x] 5.9 裝置：新增頁選一張照片 → 加入 → 今日頁那一列有縮圖
- [x] 5.10 裝置：選了照片後按「清空」或返回 → 不留孤兒檔
      （設定 → 應用程式資訊 → 儲存空間的「使用者資料」：274 KB → 選圖後 340 KB → 清空／返回後回到 274 KB）
- [x] 5.11 裝置：捲到最下面的分類清單時「加入」「清空」仍然看得到
- [x] 5.12 裝置：匯入使用者的 184 筆舊資料，今日／歷史／食物庫都正常，全程 `logcat -b crash` 乾淨
- [x] 5.13 測完刪掉自己造的測試紀錄，匯出比對：食物 78／紀錄 184／體重 7／備註 1／喝水 1 與測試前一致
- [x] 5.14 commit（英文、無 Co-Authored-By）；不 push、不 tag
