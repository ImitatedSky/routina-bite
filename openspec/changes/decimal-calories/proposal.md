# Change: decimal-calories

## Why

熱量在資料裡一直是小數（使用者的食物庫裡就有 168.32、337.89、536.52 這些從營養標示抄來的值），
但顯示與輸入一律 `roundToInt()`。這不只是看起來不精確——**它會吃掉資料**：
打開「The vegan 高蛋白(芝麻)」的編輯頁，什麼都不改按儲存，168.32 就變成 168。

## What Changes

- 新增 `formatKcalExact`：整數不帶小數點，有小數留到兩位、去掉尾端的 0。
- **單筆的熱量改用它**：食物庫列表、食物編輯頁的熱量欄、紀錄表單的熱量欄、
  今日頁每一筆紀錄右邊的數字、桌面捷徑記完食物的提示。
- **合計維持整數**：今日已吃／剩餘、餐別小計、歷史、趨勢圖、兩個小工具。
  一整天加起來的 0.01 是雜訊，而且一排三位小數的數字很難掃讀。

## Non-goals

- 不動三大營養素的格式（本來就是一位小數）
- 不動儲存格式（`Nutrients.kcal` 本來就是 Double，這次只改顯示與回填）

## Capabilities

### New Capabilities
（無）

### Modified Capabilities
- `bite-food-library`: 食物的熱量照實顯示，不再被四捨五入
- `bite-diary`: 單筆紀錄的熱量照實顯示

## Impact

- `ui/Format.kt`、`ui/FoodEditScreen.kt`、`ui/EntryForm.kt`、`ui/FoodGroups.kt`、
  `ui/TodayScreen.kt`、`ShortcutActivity.kt`、`strings.xml`（一個格式從 `%d` 改 `%s`）
- 零新權限、零新相依
