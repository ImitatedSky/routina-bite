# Tasks: shared-nutrition-form

## 1. 共用的欄位

- [x] 1.1 `ui/FoodFields.kt`：`kcalFromMacros`、`NutrientField`、`NutrientFields`
- [x] 1.2 `NutrientFields` 的摺疊：常用四欄常駐，其餘五欄收起；開表單時有值就展開
- [x] 1.3 `CategoryField`（文字欄＋既有分類 chip）與 `FavoriteSwitch` 從食物編輯頁抽出
- [x] 1.4 字串：`entry_field_*` 與食物的九個 `field_*` 併成一組 `nutrient_*`

## 2. 紀錄表單補齊

- [x] 2.1 `EntryFormState` 加 satFat／transFat／sugar／sodium／cholesterol 五個欄位與 updater
- [x] 2.2 `fillNutrients` 與 `loadFrom` 一併處理九項
- [x] 2.3 五個次要欄不參與熱量換算
- [x] 2.4 `recalcKcalFromMacros` 改用共用的 `kcalFromMacros`
- [x] 2.5 `EntryFormFields` 改用 `NutrientFields`

## 3. 備註帶入

- [x] 3.1 `draftOf(food, meal)` 帶入 `food.note`

## 4. 順手存進食物庫

- [x] 4.1 `EntryFormState`：`saveToLibrary`／`category`／`favorite`／`servingGramsText`
- [x] 4.2 `updateServingGrams`：改每份公克不動份數，公克欄跟著重算
- [x] 4.3 `toFood(id, createdAt)` 存 `basis`（每份值）不是欄位上的總量
- [x] 4.4 勾了才顯示每份公克／分類／常用；`foodId != null` 時整塊不出現
- [x] 4.5 勾了之後名稱必填（`valid`）
- [x] 4.6 `AddFoodScreen.add()` 先建食物再把 `foodId` 掛上這一筆
- [x] 4.7 `loadFrom` 換一筆時把三個欄位重設

## 5. 食物編輯頁改用共用欄位

- [x] 5.1 `FoodEditScreen` 改用 `NutrientFields`／`CategoryField`／`FavoriteSwitch`
- [x] 5.2 `recalcKcal` 改用 `kcalFromMacros`
- [x] 5.3 必填驗證（名稱、熱量）維持不變

## 6. 驗證

- [x] 6.1 `assembleDebug` 綠燈
- [x] 6.2 新增紀錄頁：「更多營養標示」展開／收起
- [x] 6.3 蛋白質 20 → 熱量自動 80
- [x] 6.4 份數改 2 → 熱量 160、蛋白質 40.0、飽和脂肪 10.0（九項都連動）
- [x] 6.5 勾「同時加進食物庫」→ 每份公克／分類（含既有 chip）／常用出現
- [x] 6.6 名稱空白時「加入」停用
- [x] 6.7 加入後食物庫出現該筆，顯示 **80 大卡**（每份值，不是 160）
- [x] 6.8 食物編輯頁開它：欄位與紀錄表單同一組，飽和脂肪 5.0 且「更多營養標示」自動展開
- [x] 6.9 食物加備註 → 回新增頁點它 → 備註帶進表單
- [x] 6.10 點了食物之後「同時加進食物庫」整塊消失
- [x] 6.11 測試資料（紀錄與食物）已刪除，食物庫其餘內容未受影響
- [x] 6.12 `logcat -b crash` 乾淨
- [ ] 6.13 真機（Pixel）：兩欄欄位在實機寬度下的斷字、鍵盤遮擋
