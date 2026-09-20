# Change: add-macro-widget

## Why

v0.8.0 的小工具回答「還能吃多少、水喝了沒」，使用者看完說**「只有喝水」**——
他要的是「今天所有飲食狀況」：熱量吃了多少，以及脂肪、蛋白質、碳水各吃了幾公克。
那是另一個問題，值得第二個小工具，不該硬塞進同一塊（塞進去會超過桌面給的高度）。

同時使用者指出一件輸入上的事：**填了碳水、脂肪、蛋白質之後，熱量本來就算得出來**，
不該再要他自己算一次；但如果他想特別指定熱量，也要讓他改。

## What Changes

- **新增第二個桌面小工具「今日營養」**（`BiteMacroWidgetProvider`，4×2）：
  上面一行「已吃 / 目標 大卡」加進度條，下面三欄並排顯示蛋白質、脂肪、碳水各幾公克，
  三個數字用三大營養素的固定色。純顯示，點一下開今日頁。
- **紀錄表單自動換算熱量**：填了三大營養素就依 4/9/4 算出熱量並填進熱量欄。
  使用者自己改熱量欄之後就不再覆蓋；把熱量欄清空即交還給自動換算。
  從食物庫帶進來的熱量是標示值，算「已經指定」，不會被換算蓋掉。

## Non-goals

- 不在這個小工具上放按鈕（動作留給另一個小工具，兩者分工是「狀態」與「動作」）
- 不顯示每一筆吃了什麼（那需要 collection widget，而且 4×2 放不下）

## Capabilities

### New Capabilities
- `bite-macro-widget`: 今日營養小工具的內容與更新

### Modified Capabilities
- `bite-diary`: 紀錄表單的熱量欄從純手動改成「可自動換算、可手動覆寫」

## Impact

- 新增 `BiteMacroWidgetProvider.kt`、`res/layout/bite_macro_widget.xml`、
  `res/xml/bite_macro_widget_info.xml`
- `BiteApp.kt`（同一條資料流一併重畫兩種小工具）、`AndroidManifest.xml`（第二個 receiver）、
  `proguard-rules.pro`、`colors.xml` 與 `values-night/colors.xml`（三大營養素色）、`strings.xml`
- `ui/EntryForm.kt`（`kcalIsManual` 與 `recalcKcalFromMacros`）
- **零新權限、零新相依**
