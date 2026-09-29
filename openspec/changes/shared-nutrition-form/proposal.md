# Change: shared-nutrition-form

## Why

紀錄表單與食物編輯頁填的是同一張營養標示，卻各寫了一份：

- 紀錄表單只有四個營養欄（熱量、蛋白質、脂肪、碳水），食物編輯頁有九個。
  快速輸入一樣東西就只能填四項，剩下五項要先建食物、再回來點它。
- 「三大營養素自動算熱量」在兩邊各實作了一次（`EntryFormState.recalcKcalFromMacros`
  與 `FoodEditScreen.recalcKcal`），兩份要一起改才不會漂。
- 標籤也兩套（`field_protein` 是「蛋白質（g）」、`entry_field_protein` 是「蛋白質 g」）。

另外兩個洞：

- 從食物庫點一個食物，**備註沒有帶過來**。食物庫的備註寫的就是「這東西是什麼」
  （麵包種類、醬料、份量說明），記下來的那一筆卻看不到。
- 快速輸入打完一筆好吃的東西，**沒有辦法順手存進食物庫**，下次得整份重打。

## What Changes

- **營養欄抽成共用元件** `NutrientFields`：兩邊同一個順序、同一組標籤、同一個摺疊行為。
  常用四欄一直在，其餘五欄收在「更多營養標示」底下（開表單時就有值的話預設展開）。
- **分類與常用也抽成共用** `CategoryField` / `FavoriteSwitch`。
- **換算規則只留一份** `kcalFromMacros(protein, fat, carbs)`。
- **紀錄表單補上另外五個營養欄**（飽和脂肪、反式脂肪、糖、鈉、膽固醇）。
  `DiaryEntry.perServing` 本來就是完整的 `Nutrients`，只是表單沒開出來。
- **點食物時一併帶入備註**。
- **新增紀錄頁多一個「同時加進食物庫」**：勾了才顯示每份公克、分類、常用，
  按「加入」時先建食物再把這一筆的 `foodId` 掛上去。

## Non-goals

- 不把兩個畫面合成一個 Composable（見 design D85）
- 不動 `Food` 與 `DiaryEntry` 的欄位，不做資料遷移
- 編輯既有紀錄的對話框不給「加進食物庫」（改一筆舊紀錄不該產生新食物）

## Capabilities

### New Capabilities
（無）

### Modified Capabilities
- `bite-entry-form`: 完整的九個營養欄、備註帶入、順手存進食物庫
- `bite-food-library`: 編輯頁改用共用欄位

## Impact

- 新增 `ui/FoodFields.kt`
- 改 `ui/EntryForm.kt`、`ui/FoodEditScreen.kt`、`ui/AddFoodScreen.kt`、`strings.xml`
- 字串：`entry_field_*` 與食物欄位的九個 `field_*` 併成 `nutrient_*`
- **零新權限、零新相依、無資料遷移**
