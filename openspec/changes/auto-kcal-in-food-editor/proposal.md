# Change: auto-kcal-in-food-editor

## Why

v0.9.0 讓新增紀錄的表單在填了三大營養素之後自動算出熱量。使用者接著指出：
**新建／編輯食物那一頁沒有這個功能**。同一件事在兩個地方行為不一致，
而且食物編輯頁正是「照著營養標示一欄一欄抄」的地方，更需要它。

## What Changes

- 食物編輯頁填了蛋白質、脂肪、碳水之後，**熱量欄自動算出來**（蛋白質與碳水 4、脂肪 9）。
- 使用者自己改熱量欄之後就不再覆蓋；把熱量欄清空即交還自動換算。
- 編輯既有食物時，它原本的熱量算「已經指定」，不會被換算蓋掉。
- 那一頁的說明文字補上這個行為。

## Capabilities

### New Capabilities
（無）

### Modified Capabilities
- `bite-food-library`: 食物編輯頁的熱量欄從純手動改成「可自動換算、可手動覆寫」

## Impact

- `ui/FoodEditScreen.kt`、`strings.xml`
- 零新權限、零新相依
