# Tasks: improve-entry-editing

## 1. 資料模型

- [x] 1.1 `model/Diary.kt`：`DiaryEntry` 加 `val note: String = ""`（放在 `perServing` 之後、`createdAt` 之前）（D26）
- [x] 1.2 確認備份不用改版：`BiteBackup` 序列化的是整個 `DiaryEntry`，`schemaVersion` 維持 1，`BackupCodec` 不動

## 2. 共用元件

- [x] 2.1 `ui/Components.kt`：`NumberField` 內部改用 `TextFieldValue`，焦點狀態進 `LaunchedEffect` 後才全選（當場設會被點擊的游標定位蓋掉）；外部 `value` 變更則在組合階段同步回內部（D25）
- [x] 2.2 `ui/Components.kt`：非數字字元改成整批拒收，不再逐字過濾（游標位置才對得上）（D25）
- [x] 2.3 確認對外 API 仍是 `String`，既有呼叫端（份數、公克、目標、體重、食物編輯頁）一行都不用改

## 3. 共用的紀錄表單

- [x] 3.1 新檔 `ui/EntryForm.kt`：`EntryDraft`（name／servings／servingGrams／basis／meal／note／foodId）與三個 `draftOf`（空白、從食物、從既有紀錄）（D21）
- [x] 3.2 `EntryFormDialog(initial, editing, onConfirm, onDismiss)`：標題與按鈕文字依 `editing` 切換（新增紀錄／加入、修改紀錄／儲存）
- [x] 3.3 欄位與版面：名稱（placeholder「快速輸入」，不預填）／份數＋公克並排／熱量、蛋白質、脂肪、碳水直排／`MealPicker`／備註；`text` 區塊包 `verticalScroll`；組內 8dp、組間 16dp（D24、D27）
- [x] 3.4 份數真值 `AmountEdit { NONE, SERVINGS, GRAMS }`：沒動過用 `initial.servings`，動過才用欄位文字算，公克那條用 `公克 ÷ 每份公克`（D23）
- [x] 3.5 連動：改份數或公克 → 四個營養欄以 `basis × 份數` 重填；份數不合法時不重填（D22）
- [x] 3.6 連動：改某個營養欄 → `basis` 的那一項 = `輸入值 ÷ 份數`；份數不合法時只改文字不動 `basis`（D22）
- [x] 3.7 營養欄的 0 顯示成空白；存檔時空白當 0（D22）
- [x] 3.8 存檔：`servings = 份數`、`perServing = basis`（五項隱藏營養原樣保留）、`name` 空白補「快速輸入」、`foodId` 保留；份數 ≤ 0 或不是數字時停用按鈕（D22、D24、D29）

## 4. ViewModel

- [x] 4.1 `ui/BiteViewModel.kt`：`addEntry(date, draft)` 與 `updateEntry(entry, draft)` 取代 `addFoodEntry`／`addQuickEntry`／舊的 `updateEntry`；`updateEntry` 用 `copy` 保留 `id`／`date`／`createdAt`（D29）

## 5. 畫面

- [x] 5.1 `ui/AddFoodScreen.kt`：刪掉 `AmountDialog` 與 `QuickAddDialog`，點食物與「快速輸入」都開 `EntryFormDialog`（`editing = false`）
- [x] 5.2 `ui/TodayScreen.kt`：刪掉 `EntryEditDialog`，點一列改開 `EntryFormDialog`（`editing = true`）
- [x] 5.3 `ui/TodayScreen.kt`：紀錄列的 ⋯ 選單加「編輯」放在「刪除」之上（D28）
- [x] 5.4 `ui/TodayScreen.kt`：紀錄列在 `note` 非空時，於份數那行下面多一行備註（`bodySmall` + `onSurfaceVariant`）（D26）

## 6. 字串與版本

- [x] 6.1 `res/values/strings.xml`：`entry_form_hint`／`entry_note`／`entry_note_hint` 三個新字串附加在檔尾，全部繁中不硬編（標題與按鈕沿用既有的 `add_title`、`entry_edit_title`、`action_add`、`action_save`、`action_edit`，欄位沿用 `field_name`、`add_servings`、`add_grams`、`field_kcal`／`field_protein`／`field_fat`／`field_carbs`）
- [x] 6.2 `app/build.gradle.kts`：versionCode 3 → 4、versionName `0.2.0` → `0.3.0`

## 7. 驗證

- [x] 7.1 `./gradlew :app:assembleDebug` 與 `:app:assembleRelease` 綠燈
- [x] 7.2 裝置：`adb uninstall` 後裝 debug 版，從 `/sdcard/Download/bite-import.json` 匯入還原（78 食物／184 紀錄／31 天／7 體重／1 備註），舊紀錄沒有 `note` 欄位也載得進來、不崩潰
- [x] 7.3 裝置：從食物庫／常吃點一個食物 → 表單帶出熱量與三大營養素；改份數四個欄位跟著動
- [x] 7.4 裝置：直接改熱量欄再存，今日頁那一列顯示的就是打進去的數字
- [x] 7.5 裝置：快速輸入的名稱欄一開始是空的、有 placeholder，不刪字就能打；留空存下去是「快速輸入」
- [x] 7.6 裝置：數字欄聚焦後直接打字會覆蓋（份數、公克、目標、體重都試）
- [x] 7.7 裝置：既有紀錄用 ⋯ →「編輯」改名稱與熱量，存檔後列表更新
- [x] 7.8 裝置：備註存得起來、今日頁列表顯示得出來；沒備註的列不多佔一行
- [x] 7.9 裝置：公克欄輸入 150 存進去就是 150.0 g（不是 150.6）；只改餐別存檔不會讓公克漂掉
- [x] 7.10 裝置：1080x1920 上表單捲得動，每個欄位都碰得到
- [x] 7.11 測完把使用者資料還原好（重新匯入 `bite-import.json`）
- [x] 7.12 commit（英文、無 Co-Authored-By）；不 push、不 tag
