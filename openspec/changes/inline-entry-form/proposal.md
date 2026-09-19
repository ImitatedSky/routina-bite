# Change: inline-entry-form

## Why

使用者用了 v0.5.0 之後回報：「當再新增一筆資料，快速輸入有點不明顯。以體驗來說，到這個步驟就是要輸入新增了，
但卻多一步驟。」

現在的流程是：今日頁按 ＋ → 新增紀錄頁（餐別、常吃、最近、搜尋、三顆小按鈕「快速輸入」「新建食物」「食物庫」、
分類分組的食物清單）→ **再按一次「快速輸入」** 才跳出表單對話框 → 才能打字。

**使用者按下「新增」的當下就已經表達要輸入了**，卻還要再點一次才看得到欄位；而且「快速輸入」只是三顆外觀相同的
小按鈕之一，一點都不顯眼。

這其實是 `improve-entry-editing` 那一輪回饋第五點的延伸：「使用食物庫，也是希望像是新增一樣，只是協助填入快速輸入」
——**在使用者的心智模型裡，表單是主角，食物庫是幫他填表單的工具**。上一版把三個入口統一成同一張表單，
但資訊架構仍然是「先選食物、才有表單」，正好相反。這一版把它轉過來。

## What Changes

- **紀錄表單直接長在新增紀錄頁上**，預設是空白表單。今日頁按 ＋ 進來就能打字，不必再點任何按鈕。
  版面由上到下：餐別晶片、表單（名稱／份數＋公克／熱量＋蛋白質／脂肪＋碳水／說明／照片／備註）、
  分隔線、食物挑選區（新建食物與食物庫入口、常吃、最近、搜尋、分類分組的清單）。
- **「快速輸入」那顆按鈕拿掉**——表單已經在畫面上了，它沒有存在意義。
- **點任何一個食物（常吃／最近／搜尋結果／分組清單）＝用那個食物的值重填上方表單**（保留目前選的餐別），
  並把畫面捲回最上面。不再跳對話框，點擊數與現在一樣。
- **「加入」放在 `Scaffold` 的 `bottomBar`**，捲到哪裡都看得到；份數 ≤ 0 或不是數字時停用。
  旁邊一顆「清空」把表單重設回空白，選錯食物時不必一格一格刪。
- **表單拆成可重用的兩塊**：`EntryFormState`（狀態與連動規則）與 `EntryFormFields`（欄位 UI）。
  內嵌表單與編輯對話框共用同一份，連動規則一行都沒改。
- **編輯既有紀錄仍然是對話框**（今日頁點一列或 ⋯ →「編輯」）：那個情境是「改一筆」，對話框是對的。
  `EntryFormDialog` 因此變成「`AlertDialog` 包 `EntryFormFields`」，只剩編輯一個用途。
- **內嵌情境的照片生命週期補上兩個出口**：按「清空」、以及選了照片後直接返回上一頁，
  都要把這次新寫進 `filesDir/photos/` 但還沒歸屬給任何紀錄的檔刪掉。
- **版本**：versionCode 7 → 8、versionName `0.5.0` → `0.6.0`。

## Non-goals（留給之後的版本）

- 不動連動規則（份數／公克／四個營養欄的計算、`AmountEdit` 三態、每份值為 0 顯示空白、存檔補「快速輸入」）。
  現在是對的，改壞了使用者會馬上發現。
- 不做「加入後留在原頁面繼續記下一筆」。現在加入完就回今日頁，與 v0.5.0 相同。
- 不把表單狀態做成 `rememberSaveable`：離開新增紀錄頁（含去「新建食物」「食物庫」）表單就重置，見 design D46。
- 食物庫頁、食物編輯頁、今日頁的版面都不動；備份格式不動（`schemaVersion` 維持 1）。

## Capabilities

### Modified Capabilities
- `bite-diary`: 新增紀錄的流程改成「表單先在畫面上、食物只是填表單」
  （ADDED：清空表單；MODIFIED：`One entry form for every entry` 的三個入口與內嵌／對話框分工、
  `Quick add without a food` 不再需要按鈕、`Add screen ordering` 整頁版面與常駐的「加入」）
- `bite-entry-photos`: `Photo files follow the entry` 多兩個出口（清空表單、離開新增紀錄頁），
  因為內嵌之後「取消」這個動作在新增情境裡不存在了

（`openspec/specs/` 目前仍是空的，既有需求的正本在 `openspec/changes/add-bite-app/specs/bite-diary/spec.md`，
`improve-entry-editing` 與 `add-entry-photos` 各有 delta；MODIFIED 區塊是從最後改寫它的那一份整段複製再改寫。）

## Impact

- 修改 `app/src/main/java/com/routina/bite/ui/EntryForm.kt`
  （`EntryFormState` + `rememberEntryFormState` + `EntryFormFields`，`EntryFormDialog` 只剩編輯；
  `draftOf` 的餐別參數放寬成 `Meal?`）
- 修改 `app/src/main/java/com/routina/bite/ui/AddFoodScreen.kt`（表單內嵌、bottomBar、點食物重填、照片收尾）
- 修改 `app/src/main/java/com/routina/bite/ui/TodayScreen.kt`（`EntryFormDialog` 少一個 `editing` 參數）
- 修改 `res/values/strings.xml`（加「清空」，刪掉沒人用的 `add_amount_hint`）、`app/build.gradle.kts`（版本）
- **無新相依、無新權限、無資料模型變更、無備份格式變更**
