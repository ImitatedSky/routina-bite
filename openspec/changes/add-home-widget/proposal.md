# Change: add-home-widget

## Why

v0.7.0 的桌面捷徑解決了「快速記一筆」，但沒有解決「不用開 App 就看得到」。
使用者最常問自己的兩件事——今天還能吃多少、水喝夠了沒——目前都得開 App 才知道。
捷徑是動作的入口，看不到狀態；那是小工具的工作。

## What Changes

- **新增一個桌面小工具**（`BiteWidgetProvider`，4×2）：左邊是今天的剩餘熱量與進度，
  右邊是喝水的累計與進度，下面兩顆按鈕。
- **按鈕直接重用 v0.7.0 的管線**：「+250 水」走 `ShortcutActivity`（不開畫面、toast 回饋），
  「記一餐」走 `MainActivity` 的 `EXTRA_OPEN_ADD`。點小工具其他區域開今日頁。
- **即時更新**：`BiteApp` 觀察 `entries` / `water` / `targets` 三條資料流，一變就重畫；
  跨日靠 `DATE_CHANGED` / `TIMEZONE_CHANGED` 廣播，另有 30 分鐘的定期更新當保險。
- 深色模式有對應的顏色（`values-night`），小工具長在別人的桌面上，桌布深淺不由我們決定。

## Non-goals

- 不做多種尺寸的版面切換（一個 4×2 版面，設好縮放下限即可）
- 不顯示三大營養素、不顯示歷史；那些是開 App 才看的
- 不用 Glance（見 design D55）

## Capabilities

### New Capabilities
- `bite-home-widget`: 桌面小工具的內容、互動與更新時機

### Modified Capabilities
（無）

## Impact

- 新增 `BiteWidgetProvider.kt`、`res/layout/bite_widget.xml`、`res/xml/bite_widget_info.xml`、
  四個 drawable、`values-night/colors.xml`
- `BiteApp.kt`（多一條觀察資料流的路）、`AndroidManifest.xml`（receiver）、
  `proguard-rules.pro`（`-keep` provider）、`colors.xml`／`strings.xml`
- **零新權限、零新相依**
