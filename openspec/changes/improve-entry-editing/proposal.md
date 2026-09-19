# Change: improve-entry-editing

## Why

使用者實際用了 v0.2.0 一段時間後回報五件事，其中四件是同一個病根：**新增紀錄與編輯紀錄是兩套不同的對話框，各自只給一部分欄位**。

- 從食物庫點一個食物 → `AmountDialog`：只有份數／公克，下面一行「xxx 大卡 · 蛋白質 xx g」的預覽，看不到也改不了細項。
- 按「快速輸入」→ `QuickAddDialog`：有名稱與四個營養欄，但沒有份數／公克，而且名稱欄預先塞了「快速輸入」四個字。
- 點今日頁的一筆紀錄 → `EntryEditDialog`：只有份數與餐別。名稱、熱量、營養素都改不了；
  快速輸入的紀錄打錯一個數字，唯一的辦法是刪掉重記。

使用者的原話：「已經新增的更改只有刪除這一種方法，實在不方便」「使用食物庫，也是希望像是新增一樣，
只是協助填入快速輸入，這樣才可以觀看熱量或是細項」——他要的不是第四個對話框，而是**三個入口同一張表單**：
食物只是把表單預先填好，之後每個欄位都看得到、改得動。

另外兩件小事同源於「輸入摩擦」：數字欄與名稱欄預先塞了值，要先刪字才能打；以及每一筆紀錄想加一句備註
（現有的備註是每日一則，不是每筆一則）。

## What Changes

- **新檔 `ui/EntryForm.kt`：一張共用的紀錄表單對話框**，取代 `AmountDialog`、`QuickAddDialog`、`EntryEditDialog` 三者。
  三個入口共用：新增頁的「快速輸入」（空白表單）、新增頁點一個食物（用該食物的數值填好）、
  今日頁點一筆紀錄或 ⋯ →「編輯」（用該筆紀錄的數值填好）。
  欄位由上到下：名稱、份數、公克（只在有每份公克基準時出現）、熱量／蛋白質／脂肪／碳水、餐別、備註。
- **四個營養欄顯示的是「這一筆的總量」（每份值 × 份數），不是每份值**——使用者要看的是「我吃了多少」。
  表單內部保存 `basis: Nutrients`（每一份的值）：改份數／公克就依 `basis × 份數` 重算四欄；
  直接改某一欄就把 `basis` 的那一項改成 `輸入值 ÷ 份數`。最後動的那個就是使用者要的值。
- **名稱欄預設空字串**，空白時以 placeholder 顯示「快速輸入」，**存檔時仍空白就存成「快速輸入」**。
  不再預先塞字讓使用者刪。
- **`NumberField` 取得焦點時全選現有內容**，直接打字就覆蓋。這一改同時修好份數、公克、每日目標、
  體重、食物編輯頁的同一個問題。一般文字欄（名稱、備註）不套用——全選在那裡只會礙事。
- **`DiaryEntry` 加 `note: String = ""`：每一筆紀錄可以有一句備註**，今日頁的紀錄列在有備註時多顯示一行。
  與既有的「每日備註」（`DayNote`）並存，兩者不互相取代。
- **今日頁紀錄列的 ⋯ 選單加「編輯」**，放在「刪除」上面；點整列仍然是編輯（行為不變，但開的是新表單）。
- **版本**：versionCode 3 → 4、versionName `0.2.0` → `0.3.0`。

## Non-goals（留給之後的版本）

- **紀錄附圖片**（使用者第 4 點）。要先決定圖片進不進備份檔、相機／相簿權限、儲存位置與清理策略，
  這一版不碰，見 design 的 Open Questions。
- 飽和脂肪、反式脂肪、糖、鈉、膽固醇不上表單（跟著 `basis` 原樣保留並隨份數縮放，只是不顯示）。
- 不做「把這筆紀錄存回食物庫」、不做多筆一起編輯、不做紀錄搬到別的日期。
- 備份格式不動（`schemaVersion` 維持 1）。

## Capabilities

### Modified Capabilities
- `bite-diary`: 新增與編輯紀錄合併成同一張表單（ADDED：共用表單、營養欄連動規則、每筆備註、
  數字欄聚焦全選；MODIFIED：`Diary entry snapshot` 多一個 `note` 欄位、
  `Log by servings or grams` 的公克精度與欄位來源、`Quick add without a food` 的名稱預設、
  `Edit and delete entries` 改成整筆可編輯）

（`openspec/specs/` 目前仍是空的，`add-bite-app` 尚未 archive，既有需求的正本在
`openspec/changes/add-bite-app/specs/bite-diary/spec.md`；MODIFIED 區塊是從那裡整段複製再改寫。）

## Impact

- 新增 `app/src/main/java/com/routina/bite/ui/EntryForm.kt`（共用表單與連動規則）
- 修改 `model/Diary.kt`（`DiaryEntry.note`，有預設值，舊 `diary.json` 與舊備份照樣讀得進來）
- 修改 `ui/Components.kt`（`NumberField` 聚焦全選；對外 API 維持 `String`，呼叫端不用改）
- 修改 `ui/AddFoodScreen.kt`（刪掉 `AmountDialog` 與 `QuickAddDialog`，兩個入口都改開 `EntryForm`）
- 修改 `ui/TodayScreen.kt`（刪掉 `EntryEditDialog`，改開 `EntryForm`；紀錄列多一行備註；⋯ 選單加「編輯」）
- 修改 `ui/BiteViewModel.kt`（`addEntryFromForm` / `updateEntryFromForm` 取代 `addFoodEntry` / `addQuickEntry` / `updateEntry`）
- 修改 `res/values/strings.xml`（新字串附加在檔尾）、`app/build.gradle.kts`（版本）
- **無新相依、無新權限、無備份格式變更**
